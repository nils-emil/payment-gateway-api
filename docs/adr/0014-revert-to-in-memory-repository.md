# 0014. Revert to an in-memory repository for the exercise scope

**Status:** Accepted

Supersedes 0010. Reinstates the approach of 0004.

## Context

ADR-0010 moved persistence to Postgres via Spring Data JPA, with Liquibase owning the schema and
Testcontainers backing the integration suite. That is the right call for a production gateway, but
it works against the brief for this exercise, which states that a mock/in-memory repository is
acceptable, that no real database is required, and — explicitly — to keep the solution simple and
avoid over-engineering. The JPA/Liquibase/Postgres stack adds a running database, a schema-migration
tool, ORM dependencies, and a Docker requirement on the integration suite, none of which advance the
two functional requirements (process a payment, retrieve a payment).

The hexagonal boundary (ADR-0001) means storage is a single driven adapter, so reverting is a
localized change with no impact on the domain or use cases.

## Decision

- `PaymentRepository` is implemented by `InMemoryPaymentRepository` (a `ConcurrentHashMap<UUID,
  Payment>`) in `adapter/out/persistence`, wired as the sole `@Repository` bean.
- The JPA adapter, `PaymentEntity`, the Spring Data repository, the Liquibase changelog, and the
  Postgres/Testcontainers dependencies are removed. Integration tests run against the in-memory
  adapter inside the Spring context and the `MockWebServer` bank stub — no Docker, no database.
- Idempotency (ADR-0009) is retained but is now enforced **only in application logic** via the
  read-then-act lookup, not by a database unique constraint. This weakens the concurrent-submit
  guarantee that ADR-0012 leaned on the constraint for: under a true race, two requests with the
  same key can each miss the lookup and both persist, leaving two records that share a key (the
  lookup then returns the first). The safety-critical invariant from ADR-0011 still holds at the
  use-case level — the bank is called at most once per create flow — but durable de-duplication of
  the key is out of scope here and would return with a real store.

## Consequences

Positive: the solution matches the brief — it starts with no external infrastructure, the full
build (`./gradlew clean build`) runs without Docker, and the code surface is smaller. The domain
and use cases are untouched, demonstrating that the storage choice is genuinely a pluggable adapter.

Negative: payments are lost on restart; there is no durability, no cross-instance idempotency, and
no query capability beyond lookup by id. ADR-0010 records the production-grade alternative and the
exact migration path (re-introduce the JPA adapter behind the same port) should durability be
required.
