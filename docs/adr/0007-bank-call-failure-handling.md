# 0007. Bank-call failure handling: timeouts and a logged Pending record

**Status:** Accepted

## Context

ADR-0002 established the persist-first lifecycle: a `Pending` payment is saved before the bank is called, and on bank failure the gateway returns `502` while the record stays `Pending`. Implementing the failure path surfaced a gap: ADR-0002 lists "timeout" as a handled failure mode, but the bank `RestClient` was built with no connect/read timeout. The JDK client defaults to infinite, so a hung bank would block the worker thread indefinitely — the catch block could never fire.

We also considered distinguishing failure modes (a `4xx` "rejected" from a `5xx`/timeout "unavailable") and marking provably-dead payments `Failed`. We decided against both: the gateway's response is the same regardless of how the bank call failed — the payment is in-doubt — so a single failure type keeps the model simple, and a failed bank call does **not** change the payment status. We also chose not to expose the in-doubt payment's id to the caller: it is an internal diagnostic, not part of the error contract.

## Decision

- **Configure timeouts.** `payment.bank.connect-timeout` / `payment.bank.read-timeout` (defaults `2s` / `5s`) are bound on `PaymentProperties.Bank` and applied to the bank `RestClient`, so a slow/hung bank fails fast.
- **One failure type.** Every bank-call failure — HTTP error status (`4xx`/`5xx`), timeout, connection error, or an unreadable success body (empty, missing `authorized`, or `authorized: true` with no authorization code) — becomes a `BankUnavailableException`. A missing/ambiguous result is treated as in-doubt, never as a silent decline. The payment is left `Pending`; the status is not updated. The `502` body is a plain `{ "error": "acquiring bank unavailable" }`.
- **Log the pending payment id.** The use case logs a warning with the left-`Pending` payment's id when the bank call fails, so the in-doubt record can be found from the logs for reconciliation. The id is not returned to the caller.

## Consequences

Positive: the timeout failure mode ADR-0002 assumed is now real; the failure model stays deliberately small; and the error response carries no payment-specific data while the id is still captured (in logs) for operators.

Negative: a `4xx` from the bank (which would indicate our request was malformed) is reported identically to the bank being down — acceptable because validation runs before the bank call, so a well-formed request is expected. The caller cannot directly locate the in-doubt `Pending` record; resolving those records (ideally via an idempotency key sent to the bank and a reconciliation job working from the logged ids) remains out of scope, as in ADR-0002.
