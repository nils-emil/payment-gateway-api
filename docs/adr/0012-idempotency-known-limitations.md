# 0012. Known idempotency limitations

**Status:** Accepted

## Context

Idempotency is implemented as a read-then-act flow (ADR-0009): `ProcessPaymentUseCase` looks up an
existing payment by `Idempotency-Key`, and on a miss runs the create path
(`validate → savePending → authorize → settle`). The unique constraint on `idempotency_key`
(`uq_payments_idempotency_key`) is the durable backstop. ADR-0011 added status-aware replay so an
in-doubt `Pending` record returns `409` rather than a misleading `200`.

Several known gaps remain and are worth recording explicitly rather than leaving implicit:

1. **Concurrent-submit race surfaces as `500`.** Two requests carrying the same key can both pass
   the lookup before either has inserted its `Pending` row. Both reach `savePending`; the unique
   constraint lets exactly one `INSERT` win, and the loser throws
   `DataIntegrityViolationException`. No handler maps it, so it falls through to the catch-all and
   the caller receives `500 internal error`.

   The crucial point: the **core invariant still holds**. The loser fails at `savePending`, which
   is *before* the bank call, so it never authorizes — there is no duplicate payment and no double
   charge. Only the surfaced status is wrong (`500` instead of a graceful idempotent response).
   This race window is narrow: once the winner's `Pending` row is committed, a later duplicate is
   caught by the lookup and handled by ADR-0011 (`409`).

2. **No request-fingerprint check.** A client that reuses a key with a *different* body receives
   the original payment instead of `422`/`409` (already noted in ADR-0009).

3. **No key lifecycle.** Keys are retained indefinitely with no TTL/expiry, and their
   format/length is not validated before persistence (an over-length key would itself trip the
   column limit and surface as `500`).

## Decision

We **accept** these limitations for the scope of this exercise and document the remediation rather
than implementing it:

- The concurrent-submit race should be closed by catching the unique-constraint violation in the
  create path and resolving it to the **replay path** — re-read by key and return the now-committed
  winner (terminal → `200`, still `Pending` → `409` per ADR-0011) — so the loser sees the same
  graceful outcome as any other replay, never a `500`.
- Request-fingerprint matching, key expiry/TTL, and explicit key-format validation are deferred.

We record this as its own ADR rather than editing ADR-0009, keeping the original decision intact.

## Consequences

Positive: the safety-critical invariant is stated plainly and shown to hold under concurrency — at
most one authorization per key, no double charge — and the remaining gaps are visible and have a
defined remediation path.

Negative: until the race is closed, a genuine concurrent double-submit can return `500` to one
caller, who must retry (the retry then replays cleanly). Key retention is unbounded, and
mismatched-body replays are silently accepted (ADR-0009).
