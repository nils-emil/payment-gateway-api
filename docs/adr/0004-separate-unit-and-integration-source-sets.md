# 0004. Separate Unit and Integration Source Sets

**Status:** Accepted

## Context

Spring slice tests (`@WebMvcTest`) and full-context integration tests (`@SpringBootTest`) are valuable for verifying wiring and HTTP behaviour, but they start an application context and are therefore slow — typically one to two orders of magnitude slower than plain unit tests. Mixing slow integration tests into the single default `test` source set forces developers to wait for context startup on every red/green/refactor cycle, discouraging TDD.

## Decision

We will maintain two Gradle source sets with separate tasks:

- **`src/test/java`** — fast unit tests with no Spring context. Domain validation, status transitions, service logic with mocked ports, and mapper logic. Run with `./gradlew test`. This is the TDD inner loop: it runs in seconds.
- **`src/integrationTest/java`** — slice and integration tests that require a Spring context: `@WebMvcTest` for the controller and error-handling advice; the `RestClient` bank adapter against a stubbed HTTP server; a `@SpringBootTest` bean-wiring smoke test. Run with `./gradlew integrationTest`.

`./gradlew clean build` runs both source sets: `check` depends on `integrationTest`, and `integrationTest` is ordered after `test` via `shouldRunAfter`, so the fast unit suite runs first and a failing unit test halts the build before the slower integration suite runs.

## Consequences

Positive: the unit suite runs in a few seconds, making TDD practical; integration tests are not skipped but are kept separate so they do not slow the inner loop; the distinction enforces the rule that domain tests must not touch Spring.

Negative: developers must remember to run `./gradlew integrationTest` (or `clean build`) before pushing, not just `./gradlew test`; configuring a second Gradle source set with its own classpaths and dependencies adds a small amount of build-script complexity.
