# 0001. Use Hexagonal Architecture

**Status:** Accepted

## Context

The payment gateway interacts with two external systems — a REST-facing merchant client and a simulated acquiring bank — and contains non-trivial domain logic around card validation, payment lifecycle, and currency rules. As an assessable codebase it must be easy to navigate, test in isolation, and extend without cascading changes.

Framework coupling (Spring annotations, Jackson, HTTP clients) mixed into business logic makes unit testing expensive (requires a Spring context) and creates pressure to duplicate or mock infrastructure in every domain test. Separating concerns gives each layer a clear responsibility and a natural test boundary.

## Decision

We will structure the application as a hexagonal (ports & adapters) architecture:

- **Domain** (`domain/`) contains pure Java business logic — models, use-case interfaces (ports/in), driven-port interfaces (ports/out), and service implementations. It has zero Spring, Jackson, or HTTP imports and compiles standalone.
- **Adapters** depend inward on the domain. The REST controller (`adapter/in/web/`) maps HTTP requests to domain commands and domain objects to HTTP responses via dedicated mappers; domain objects never cross the REST boundary directly. Each external integration (bank simulator) gets a port interface in `domain/port/out/` and an adapter in `adapter/out/`.
- **Config** (`config/`) handles Spring wiring, bean definitions, and externalised configuration. Framework concerns are confined here and to the adapter layer.

## Consequences

Positive: domain logic is fully unit-testable without a Spring context; the boundary between business rules and infrastructure is explicit and enforced; adding or swapping an adapter (e.g. replacing the in-memory store with a real database) requires no domain changes.

Negative: the layered package structure and mapper classes add boilerplate compared to a simpler script-style or anemic MVC approach. For a small service this overhead is notable; it is justified here by the testability and maintainability goals.
