# 0010. Persist payments in Postgres via Spring Data JPA and Liquibase

**Status:** Superseded by 0014

Supersedes 0004. Superseded by 0014, which reverts to an in-memory repository for the scope of this
exercise; this ADR is retained as the production-grade persistence alternative and migration path.

## Context

ADR-0004 chose an in-memory `ConcurrentHashMap` repository, accepting that all payment data is
lost on restart. For a payment gateway that is the wrong durability story: an authorized payment
that vanishes on restart cannot be reconciled, and the idempotency guarantee of ADR-0009 needs a
durable, uniquely-constrained key. We are moving to a real relational store.

We chose Postgres (the de-facto default for transactional fintech workloads), Spring Data JPA for
the access layer, and Liquibase as the schema source of truth. An explicit migration tool is
preferred over Hibernate `ddl-auto` schema generation so that schema changes are reviewed,
versioned, and identical across environments; Hibernate runs in `validate` mode against the
Liquibase-managed schema.

## Decision

- `PaymentRepository` (domain port) is implemented by `JpaPaymentRepository` in
  `adapter/out/persistence`, which delegates to a Spring Data `PaymentJpaRepository` and maps
  between the JPA `PaymentEntity` and the pure domain `Payment`/`Money`. The domain stays free of
  JPA annotations (ADR-0001); only the entity and the adapter know about persistence.
- Liquibase owns the `payments` schema (`db/changelog/db.changelog-master.yaml`), including the
  unique constraint on `idempotency_key` that backs ADR-0009.
- The in-memory repository is retained only as a test double in the unit (`src/test`) source set,
  driving fast use-case tests with no database.
- Integration tests run against a real Postgres via Testcontainers (`@ServiceConnection`),
  consistent with the Docker-based bank simulator; the persistence adapter is verified end-to-end
  rather than against an embedded H2 that behaves differently from production.

## Consequences

Positive: payments are durable; the idempotency key is enforced by a database constraint, not
just application logic; schema is versioned and reviewable; the hexagonal boundary made the swap
a single new adapter with no domain changes.

Negative: the app now requires a running Postgres (env-configured datasource) and the integration
suite requires Docker, so `./gradlew integrationTest` no longer runs on a machine without it. The
unit suite (`./gradlew test`) stays infrastructure-free via the in-memory double.
