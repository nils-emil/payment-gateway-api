# 0002. Persist-First Payment Lifecycle

**Status:** Accepted

## Context

`REQUIREMENTS.md` describes three payment outcomes: Authorized, Declined, and Rejected. It says nothing about what happens when the acquiring bank is unreachable mid-flight. In a naive implementation, a valid request would call the bank and only persist if the call returned. If the bank call hangs or the process crashes after the call returns but before persistence, the payment is lost with no record and no way to reconcile it.

## Decision

We will adopt a persist-first lifecycle and introduce a fourth internal status, `Pending`:

1. A validated payment request immediately persists a `Pending` payment record before the bank is called.
2. After the bank responds, the payment is updated to `Authorized` or `Declined` and saved again.
3. If the bank call fails (HTTP 503, timeout, or connection error), the payment remains `Pending` in the repository and the gateway returns `502` to the caller.

`Pending` is deliberately visible: a `GET /payments/{id}` for a pending record returns `200` with `"status": "Pending"` so merchants can observe the in-doubt state. No async retry or reconciliation job is built; that is explicitly out of scope.

## Consequences

Positive: an in-doubt payment is never silently lost — it always has a persisted record that can be inspected and, in a future iteration, picked up by a reconciliation job. The lifecycle is honest about intermediate state rather than hiding it.

Negative: we now surface a status (`Pending`) that the requirements specification does not mention, which could surprise a client that only expected Authorized/Declined/Rejected. The `GET` response must document this fourth value. The two-write pattern (persist then update) also adds a second repository call to the happy path.
