# 0004. In-Memory Repository

**Status:** Accepted

## Context

The requirements explicitly state that a mock or in-memory repository is acceptable for storage and that no real database is needed for this scope. Introducing a relational or document database would require schema migration tooling, Docker Compose configuration, a running database process in CI, and ORM dependencies — none of which add value to demonstrating the gateway's core logic.

## Decision

We will implement `PaymentRepository` as an in-memory store backed by a `ConcurrentHashMap<UUID, Payment>`. The implementation lives in `adapter/out/persistence/InMemoryPaymentRepository` and wires into the domain's `PaymentRepository` port via Spring's dependency injection.

`ConcurrentHashMap` is chosen over a plain `HashMap` to be safe under concurrent HTTP requests without introducing explicit locking.

## Consequences

Positive: no external infrastructure dependency; the application starts instantly with no setup; repository behaviour is trivially verifiable in unit tests without an embedded database.

Negative: all payment data is lost on restart; there is no durability guarantee, no query capability beyond lookup by ID, and no transaction support. Replacing this adapter with a real database adapter in the future would require implementing a new `PaymentRepository` but no domain changes (ADR-0001).
