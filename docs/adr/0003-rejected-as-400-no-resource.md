# 0003. Rejected as HTTP 400 with No Persisted Resource

**Status:** Accepted

## Context

`REQUIREMENTS.md` defines `Rejected` as the outcome when invalid information is supplied: the gateway does not call the bank in this case. The question is whether `Rejected` should be treated as a persisted payment resource (returned as `200` with a body containing `"status": "Rejected"`) or as a request-level error with no resource created.

Persisting a rejected payment would mean every malformed request creates a record in the repository and is retrievable by ID. This conflates two distinct concepts — a payment that was processed (even if declined) and a request that was never accepted — and makes the repository a log of bad input rather than a store of payments.

## Decision

We will treat `Rejected` as an HTTP `400 Bad Request` with no persisted resource. When the domain's `ValidationException` is thrown:

- The controller advice maps it to `400` with a JSON error body: `{ "status": "Rejected", "errors": [ ... ] }` listing each validation failure.
- No `Payment` object is created; the bank is never called.
- A subsequent `GET /payments/{id}` for a rejected request ID returns `404` because no resource was ever stored.

## Consequences

Positive: the repository contains only real payment attempts; the REST semantics are conventional (`400` for a bad request, `404` for a missing resource); validation errors are reported as a structured list rather than a single status enum.

Negative: clients that POST an invalid request and then try to retrieve it by a self-generated ID will get a `404`, which requires clear API documentation. The `Rejected` string appears in the error body but is not a value of the `PaymentStatus` domain enum, which can cause confusion if not documented.
