# 0011. Recovering in-doubt Pending payments without re-authorizing

**Status:** Accepted

## Context

We persist a payment as `Pending` *before* calling the acquiring bank (ADR-0002), and a failed
or timed-out bank call leaves that `Pending` record in place and returns `502` (ADR-0007). Such a
record is **in-doubt**: we do not know whether the card was actually charged, because the response
never arrived.

Two constraints make this hard to resolve automatically:

- We authorize **at most once**. The acquiring bank does **not** support an idempotency key, so a
  second authorization call risks a genuine double charge — re-driving is never safe.
- We deliberately do not store the PAN or CVV (PCI hygiene), so we cannot reconstruct an
  authorization request from the stored payment later.

The original idempotency behaviour (ADR-0009) returned *any* stored payment for a replayed
`Idempotency-Key`, including a `Pending` one. That meant a transient bank outage was reported to
the client as a `200` success carrying a non-terminal `Pending` status, and the payment was never
resolved — the worst of both worlds: a stranded record *and* a misleading response.

## Decision

- `Pending` is treated as **outcome-unknown**, not as a result.
- On a replayed `POST /payments` (same `Idempotency-Key`):
  - a **terminal** stored payment (`Authorized`/`Declined`) is returned unchanged — a true
    idempotent replay;
  - a **`Pending`** stored payment yields **`409 Conflict`** ("payment is still being processed").
    The bank is **never** re-called on this path. (This mirrors how Stripe treats an idempotent
    request that is still in flight.)
- A failed/timed-out bank call is never retried automatically, on the request path or elsewhere.
- In-doubt `Pending` payments are reconciled **out-of-band**, and we deliberately do **not** build
  an automated sweep for this exercise. Such a record cannot be settled by the gateway anyway:
  re-authorization is forbidden (at-most-once), and we lack the PAN/CVV to rebuild the request.
  Automated settlement would require the bank to expose a status-by-reference **read** (which cannot
  double-charge); no such endpoint exists today. An in-doubt record stays observable through
  `GET /payments/{id}` (returning `200` with `Pending`) and the bank-failure log, which is
  sufficient for an operator to reconcile manually. A scheduled sweep that only re-alerts on the
  same records adds operational surface without changing the outcome, so it is out of scope.

## Consequences

Positive: a transient bank outage can never be reported as a successful or final payment; double
charges are structurally impossible because the bank is called at most once; an in-doubt record
stays observable via `GET /payments/{id}` and the failure log without any background machinery.

Negative: resolving an in-doubt payment is currently a manual operation; a client that replays
during the in-doubt window receives `409` and must poll `GET /payments/{id}` later; full
automation is blocked on a bank status-query endpoint we do not yet have. Request-fingerprint
matching for replays that reuse a key with a different body remains out of scope (ADR-0009).
