# Payment Gateway

API-based payment gateway that validates and forwards card payment requests to a
simulated acquiring bank, and lets merchants retrieve previously made payments.
Java · Spring Boot · Gradle · JUnit.

## Architecture: Hexagonal (Ports & Adapters)

```
src/main/java/com/paymentgateway/
├── domain/
│   ├── model/               # Pure value objects: Payment, Money, PaymentStatus, CurrencyAllowList, ValidationError
│   ├── port/in/              # Use case input commands (e.g. PaymentCommand — the unvalidated edge)
│   ├── port/out/             # Driven ports — interfaces + outbound commands
│   │                          # (PaymentRepository, AcquiringBankClient, IdGenerator, AuthorizationCommand)
│   ├── usecase/             # One subpackage per use case (@Service beans named …UseCase)
│   │   ├── getpayment/       # GetPaymentUseCase
│   │   └── processpayment/   # ProcessPaymentUseCase + ProcessPaymentUseCaseSteps
│   │       └── validation/    # ValidationRule<PaymentCommand> @Components, one per rule
│   └── support/             # Cross-cutting domain support
│       ├── exception/        # ValidationException, BankUnavailableException, EntityNotFoundException
│       └── util/             # Stateless helpers (Sensitive — card-number masking)
├── adapter/
│   ├── in/web/                # REST controllers, request/response DTOs, mappers
│   └── out/
│       ├── persistence/        # InMemoryPaymentRepository implementing PaymentRepository (ConcurrentHashMap)
│       ├── id/                  # IdGenerator adapter (UUID)
│       └── bank/                # HTTP client implementing AcquiringBankClient, talks to bank simulator
└── config/                   # Spring wiring (property beans, RestClient config)
```

Rules:
- `domain/model` is pure: zero Spring/Jackson/HTTP annotations or imports — value objects
  must compile standalone.
- Each use case lives in its own subpackage under `domain/usecase` (e.g. `usecase/getpayment`,
  `usecase/processpayment`) as a Spring `@Service` bean named `…UseCase`. A multi-step use case's
  subpackage also holds its sibling `…UseCaseSteps` `@Component` and the validation rules it owns.
  Controllers depend on the use-case class directly (no inbound port interface).
- Request validation is a set of `ValidationRule<PaymentCommand>` `@Component`s in
  `domain/usecase/processpayment/validation`; each returns a `List<ValidationError>` (`code` +
  `description`, never throws). `ProcessPaymentUseCaseSteps` injects the
  `List<ValidationRule<PaymentCommand>>` and aggregates them in `validate(...)`, which raises one
  `ValidationException` (→ 400 Rejected) carrying all errors. Add a rule by adding a `@Component`.
- Validation errors carry a translation `code` and a fallback `description`; both are returned
  in the 400 response (`{ "status": "Rejected", "errors": [ { "code", "description" } ] }`).
- Driven side keeps interfaces: `domain/port/out` ports (`PaymentRepository`,
  `AcquiringBankClient`, `IdGenerator`) are implemented by adapters in `adapter/out`. The bank
  port takes its own outbound `AuthorizationCommand` (built in the use case from `PaymentCommand`)
  — the driven side does not reuse the inbound command type.
- Persistence is an in-memory `InMemoryPaymentRepository` (`ConcurrentHashMap<UUID, Payment>`) in
  `adapter/out/persistence`, wired as the sole `@Repository`. No database, schema, or migrations.
  Data is lost on restart — acceptable for this exercise. A real relational store (Postgres/JPA)
  behind the same port is the production path.
- `POST /payments` is idempotent on an optional `Idempotency-Key` header (ADR-0007): the key rides
  on `PaymentCommand`, is stored on `Payment`, and a replay returns the stored payment without
  re-calling the bank. With the in-memory store the key is de-duplicated in application logic only
  (no unique constraint) — see ADR-0007.
- Adapters depend on the domain, never the other way round.
- Controllers map to/from domain objects via dedicated mappers — domain objects never
  cross the REST boundary directly.
- New external integrations (e.g. the bank simulator) get a port in `domain/port/out`
  and an adapter in `adapter/out`. Don't call the bank client directly from a controller.

## Commands

- `./gradlew clean build` — build + run all tests (run this before considering any task done)
- `./gradlew test` — run unit tests only (fast TDD loop)
- `./gradlew integrationTest` — run integration tests only (Spring context + MockWebServer bank stub; no Docker)
- `./gradlew test --tests ClassName` — run a single test class
- `./gradlew bootRun` — run the app locally
- Bank simulator: run via Docker; configure its
  base URL via `application.yml` / env var, never hardcode it.

## Testing

- Unit and integration tests live in **separate Gradle source sets**:
  - `src/test/java` — fast unit tests (no Spring context), run by `./gradlew test`.
  - `src/integrationTest/java` — slice/integration tests (`@WebMvcTest`,
    `@SpringBootTest` with mocked bank client), run by `./gradlew integrationTest`.
- This keeps the unit suite fast and isolated so it can drive **TDD** — the red/green/refactor
  loop runs against `test` only; integration tests verify wiring separately.
- Tests mirror the main package structure.
- Domain logic: plain unit tests, no Spring context.
- Every new use case or validation rule needs a corresponding test before the task is done.

## Git workflow

- **Always work directly on `main`.** Don't create feature branches or git worktrees for
  this project — commit straight to `main`.
- **Never run `git push`.** Pushing is the user's call, always.
- **Never run `git commit` unless explicitly asked.** Stage/leave changes uncommitted by default.
- When asked to commit, write a clear, conventional message (e.g. `feat: add payment validation`).
- **One commit per completed task.** Don't stack multiple commits for a single task — fold
  follow-up fixes/review changes into that task's commit with `--amend` while it's still the
  tip and unpushed. Ask before amending anything already pushed or rewriting older history.

## Documentation

- Key architecture decisions are recorded as **Architecture Decision Records (ADRs)** in
  `docs/adr/`, one Markdown file per decision.
- File naming: `NNNN-short-title.md` with a zero-padded sequential number,
  e.g. `0001-use-hexagonal-architecture.md`. Numbers are never reused.
- Each ADR follows this structure:
  - **Title** — `# NNNN. Short decision title`
  - **Status** — Proposed / Accepted / Deprecated / Superseded (by `NNNN`)
  - **Context** — the forces and constraints driving the decision
  - **Decision** — what was decided, in active voice ("We will…")
  - **Consequences** — the resulting trade-offs, both positive and negative
- Supersede rather than edit accepted ADRs: add a new ADR and mark the old one
  `Superseded by NNNN`, keeping the history intact.
- Reference ADRs from commit messages, PR descriptions, and other ADRs
  (e.g. `Implements ADR-0005`) — not from inline code comments.
- Smaller assumptions/notes that aren't architectural can stay in `README.md`.

## Code style

- Don't comment code. Keep it self-explanatory through clear naming and structure, and
  put design rationale in ADRs (see Documentation), not in comments.
- One narrow exception: a single-line `// ADR-NNNN` pointer is allowed above a genuinely
  counter-intuitive decision that a reader might otherwise "fix" and unknowingly revert.
