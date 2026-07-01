# 0007. Idempotent payment creation via an Idempotency-Key header

**Status:** Accepted

## Context

`POST /payments` is not naturally idempotent: a client timeout, a proxy retry, or a user
double-submit can deliver the same request twice and create two payments — two authorizations,
potentially a double charge. ADR-0005 named idempotency keys as the mechanism for reconciling
in-doubt `Pending` records; this ADR brings the key into the create path itself.

We considered carrying the key as a field in the request body, but that mixes a protocol concern
(de-duplicating a retried HTTP call) into the payment resource, so we use a dedicated header.

Two properties of the domain shape the replay rules. We persist a payment as `Pending` *before*
calling the acquiring bank (ADR-0002), and a failed or timed-out bank call leaves that `Pending`
record in place (ADR-0005). Such a record is **in-doubt**: we do not know whether the card was
charged, because the response never arrived. And we authorize **at most once** — the acquiring
bank exposes no idempotency key, so re-driving an authorization risks a genuine double charge and
is never safe.

## Decision

- Clients may send an optional `Idempotency-Key` header on `POST /payments`. Absent it, behaviour
  is unchanged.
- The key is carried on the inbound `PaymentCommand` and persisted on `Payment`.
- `ProcessPaymentUseCase` checks for an existing payment with the key **before** validating or
  calling the bank (read-then-act).
- **Status-aware replay.** `Pending` is treated as outcome-unknown, not as a result:
  - a **terminal** stored payment (`Authorized`/`Declined`) is returned unchanged — a true
    idempotent replay (`200`), with no second bank call and no second record;
  - a **`Pending`** stored payment yields **`409 Conflict`** ("payment is still being
    processed"). The bank is **never** re-called on this path.
- A failed/timed-out bank call is never retried automatically, on the request path or elsewhere.
  In-doubt `Pending` payments are reconciled **out-of-band**; we deliberately do not build an
  automated sweep. Such a record cannot be settled by the gateway anyway (re-authorization is
  forbidden, and we do not store the PAN/CVV to rebuild the request), and it stays observable via
  `GET /payments/{id}` (`200` with `Pending`) and the bank-failure log — sufficient for an
  operator to reconcile manually.

## Known limitations

We accept the following for the scope of this exercise and record the remediation rather than
implementing it:

1. **De-duplication is application-logic only.** With the in-memory repository there is
   no unique constraint on the key. Under a true concurrent race, two requests with the same key
   can each miss the lookup and both persist, leaving two records that share a key (the lookup
   then returns the first). The safety-critical invariant still holds — the bank is called **at
   most once** per create flow, so there is no double charge — only durable de-duplication is
   missing. A real relational store would add a unique constraint;
   the loser's `INSERT` would then be caught and resolved to the replay path (re-read by key,
   return the committed winner — terminal → `200`, still `Pending` → `409`) rather than creating a
   duplicate.
2. **No request-fingerprint check.** A client that reuses a key with a *different* body receives
   the original payment instead of a `409`/`422`.
3. **No key lifecycle.** Keys are retained indefinitely with no TTL/expiry, and their
   format/length is not validated before persistence.

## Consequences

Positive: retries and double-submits are safe; the protocol concern stays out of the resource
body; a transient bank outage can never be reported as a successful or final payment; double
charges are structurally impossible because the bank is called at most once; the in-doubt window
is observable without background machinery; and the remaining gaps are visible with a defined
remediation path.

Negative: resolving an in-doubt payment is a manual operation; a client that replays during the
in-doubt window receives `409` and must poll `GET /payments/{id}` later; with the in-memory store
there is no durable or cross-instance de-duplication; mismatched-body replays are silently
accepted; and key retention is unbounded.
