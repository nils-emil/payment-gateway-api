# 0013. Payment outcome observability: outcome log + Micrometer metrics via an outbound port

**Status:** Accepted

## Context

Operators need two distinct things from a payment gateway: **individual traceability** ("what
happened to payment X") and **aggregate signals** ("what's the decline rate / authorized volume").
Logs serve the first; metrics serve the second — they are not interchangeable.

Two constraints shape the design. First, nothing observable may leak card data (PAN/CVV). Second,
the domain must not be coupled to a metrics library: `domain/model` is pure, and the use cases are
unit-tested without a Spring context, so pulling Micrometer into them directly would force the
metrics library into the domain test path.

## Decision

- **Outcome log.** `ProcessPaymentUseCase` emits a single `INFO` line when a payment reaches a
  terminal state on the create path: `paymentId`, `status`, `amount` (minor units), `currency` —
  never PAN/CVV. It is emitted only on first processing, so idempotent replays neither re-log nor
  re-count.
- **Metrics via an outbound port.** A `PaymentMetrics` port lives in `domain/port/out`; the
  Micrometer implementation `MicrometerPaymentMetrics` lives in `adapter/out/metrics`. This follows
  the existing rule (new external integrations get a port + adapter) and keeps the use case
  library-agnostic and testable with a fake.
- **Meters.** `payments.processed{status,currency}` (counter) for rates, and
  `payments.amount{status,currency}` (distribution summary, base unit `minor`) for volume. Tags are
  deliberately low-cardinality; `paymentId` is never a tag.
- **Exposure.** Spring Boot Actuator exposes `/actuator/metrics` and `/actuator/prometheus`.

## Consequences

Positive: outcome rates and volumes are queryable without log-scraping; the domain stays free of
Micrometer and the use-case unit test uses a fake `PaymentMetrics`; replays are not double-counted;
only PCI-safe fields are emitted.

Negative: `payments.amount` is in minor units tagged by currency, so cross-currency sums are not
meaningful without conversion; meters are per-instance and must be aggregated at the
Prometheus/scrape layer; there is still no request-level correlation id tying the log line to other
layers' logs — that remains future work.
