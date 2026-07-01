# 0009. Idempotent payment creation via an Idempotency-Key header

**Status:** Accepted

## Context

`POST /payments` is not naturally idempotent: a client timeout, a proxy retry, or a user
double-submit can deliver the same request twice and create two payments — two authorizations,
potentially a double charge. ADR-0007 named idempotency keys as the eventual mechanism for
reconciling in-doubt `Pending` records; this ADR brings the key into the create path itself.

We considered carrying the key as a field in the request body, but that mixes a protocol
concern (de-duplicating a retried HTTP call) into the payment resource. The industry-standard
approach (e.g. Stripe) is a dedicated header.

## Decision

- Clients may send an `Idempotency-Key` HTTP header on `POST /payments`. It is optional;
  absent it, behaviour is unchanged.
- The key is carried on the inbound `PaymentCommand` and persisted on `Payment`
  (`idempotency_key` column, unique constraint).
- `ProcessPaymentUseCase` checks for an existing payment with the key **before** validating or
  calling the bank. On a hit it returns the stored payment unchanged — no second bank call, no
  second record.
- The unique constraint on `idempotency_key` is the durable guard against a concurrent
  double-submit racing past the read.

## Consequences

Positive: retries and double-submits are safe; the protocol concern stays out of the resource
body; the stored key gives operators a handle for the reconciliation flow ADR-0007 anticipated.

Negative: this implementation keys purely on the header value and does not compare the replayed
request body against the original, so a client that reuses a key with a *different* body
receives the original payment rather than a `409 Conflict`. Request-fingerprint matching and key
expiry are deliberately out of scope for this exercise.
