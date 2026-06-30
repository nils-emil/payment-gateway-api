# Payment Gateway — Design

Design for implementing the payment gateway API described in `REQUIREMENTS.md`,
following the hexagonal architecture, Gradle setup, TDD, and ADR conventions in `CLAUDE.md`.

## Goal

A payment gateway API that lets merchants:

1. **Process** a card payment, forwarding valid requests to a simulated acquiring bank.
2. **Retrieve** a previously made payment by its identifier.

## Key decisions

- **Rejected = HTTP 400, no resource.** Invalid input never creates a `Payment`. The gateway
  never calls the bank. The response is `400` with an error body; a later `GET` returns `404`.
- **Persist-first lifecycle.** A valid request persists a `Pending` payment *before* the bank
  call, then updates it to the terminal status afterward. This guarantees an in-doubt payment
  is never lost if the bank call fails mid-flight.
- **Bank unavailable = HTTP 502, payment stays `Pending`.** A valid request whose bank call
  fails (simulator `503`, timeout, connection error) leaves the persisted payment `Pending`
  and returns `502`. No async retry/reconciliation job is built (out of scope); `Pending`
  is terminal-for-now and a future job could pick it up.
- **Four internal statuses, surfaced honestly.** `Pending`, `Authorized`, `Declined` are
  stored; `Pending` is visible via `GET`. `Rejected` is not a stored status — it only exists
  as the `400` validation outcome.

### Confirmed defaults

- HTTP client: Spring `RestClient` (synchronous).
- Currency allow-list: `GBP`, `USD`, `EUR`.
- Application port: `8090` (the bank simulator owns `8080`).
- Payment ID format: random `UUID`.

## Architecture

Hexagonal (ports & adapters). The domain is framework-free and compiles standalone;
adapters depend inward on the domain, never the reverse.

```
com.paymentgateway
├── domain
│   ├── model/        Payment, Card, Money, Currency, PaymentStatus, ExpiryDate
│   ├── port/in/      ProcessPaymentUseCase, GetPaymentUseCase
│   ├── port/out/     PaymentRepository, AcquiringBankClient
│   └── service/      ProcessPaymentService, GetPaymentService
├── adapter
│   ├── in/web/       PaymentController, request/response DTOs, PaymentWebMapper, error handler
│   └── out
│       ├── persistence/  InMemoryPaymentRepository
│       └── bank/         RestClientAcquiringBankClient, bank request/response DTOs, BankMapper
└── config            Bean wiring, RestClient config, currency allow-list properties
```

## Domain model

- **`Payment`** — `id` (UUID), `PaymentStatus`, masked PAN (last 4 digits only), expiry
  month/year, `Money`, optional `authorizationCode`. Holds **no CVV and no full PAN** after
  processing. State transitions: `pending()` factory, `authorize(code)`, `decline()`.
- **`Card`** — transient value object used only for the bank call. Validates 14–19 numeric
  digits, expiry month/year in the future, CVV 3–4 numeric digits. Never persisted, never
  returned.
- **`Money`** — allow-listed `Currency` + integer amount in the minor currency unit.
- **`Currency`** — ISO 4217, 3 characters, validated against the allow-list (≤3 entries).
- **`PaymentStatus`** — `Pending`, `Authorized`, `Declined`.
- Validation lives in the domain via factory methods/constructors that throw a domain
  `ValidationException` aggregating all field errors. No Spring/Jackson/HTTP imports in the
  domain.

## Process-payment flow

```
ProcessPaymentService.process(command):
  1. Build + validate Card and Money from the command
       → invalid → throw ValidationException   (aggregates field errors)
  2. payment = Payment.pending(maskedPan, expiry, money)
     repository.save(payment)
  3. bankResult = acquiringBankClient.authorize(card, money)
       → on 503 / timeout / connection error → throw BankUnavailableException
  4a. authorized → payment.authorize(authorizationCode); repository.save(payment)
  4b. declined   → payment.decline();                    repository.save(payment)
  4c. BankUnavailableException → payment left Pending; exception propagates
  5. return payment
```

`GetPaymentService.get(id)` returns the stored payment or throws `PaymentNotFoundException`.

## REST adapter & error handling

Endpoints:

- `POST /payments` → `200` with the payment body (Authorized or Declined).
- `GET /payments/{id}` → `200` with the payment body. A `Pending` record **is** returned.
  Unknown id → `404`.

Response body fields: `id`, `status`, `last_four`, `expiry_month`, `expiry_year`,
`currency`, `amount`, and `authorization_code` when present. **Never** CVV or full PAN.

`@RestControllerAdvice` mapping:

| Exception                   | HTTP | Body                                            |
|-----------------------------|------|-------------------------------------------------|
| `ValidationException`       | 400  | `{ "status": "Rejected", "errors": [ ... ] }`   |
| `BankUnavailableException`  | 502  | `{ "error": "acquiring bank unavailable" }`     |
| `PaymentNotFoundException`  | 404  | `{ "error": "payment not found" }`              |

On `ValidationException` nothing is persisted; on `BankUnavailableException` the payment
remains `Pending`.

## Bank adapter

- Port: `AcquiringBankClient` with `BankResult authorize(Card card, Money money)`.
- `RestClientAcquiringBankClient` maps the domain objects to the simulator's snake_case body:
  `card_number`, `expiry_date` (`MM/yyyy`), `currency`, `amount`, `cvv`. It parses
  `{ "authorized": bool, "authorization_code": string }`.
- Simulator `503` / connection failure / timeout → `BankUnavailableException`.
- A `400` from the simulator indicates an internal mapping bug (we validate before calling),
  so it is treated as a `5xx`-class failure.
- Base URL comes from configuration (`application.yml` / env var); never hardcoded.

## Testing (TDD, split source sets)

- **`src/test/java`** — fast unit tests, no Spring context. Domain validation rules, status
  transitions, mappers, and `ProcessPaymentService` flow with mocked port doubles. This is
  the red/green/refactor loop that drives development.
- **`src/integrationTest/java`** — `@WebMvcTest` for the controller and error mapping; the
  `RestClient` bank adapter against a stubbed HTTP server (MockWebServer or WireMock); a
  `@SpringBootTest` smoke test verifying bean wiring.

Every new use case and validation rule gets a corresponding test before it is considered done.

## ADRs to record

1. Hexagonal architecture and domain purity.
2. Persist-first lifecycle and the `Pending` status (deliberate deviation from the
   three-status model in `REQUIREMENTS.md`).
3. Rejected as a `400` with no persisted resource.
4. In-memory repository for storage.
5. Separate unit and integration source sets to support TDD.

## Out of scope

- Async retry / reconciliation of `Pending` payments.
- Real database persistence.
- Authentication, idempotency keys, multi-tenant merchant accounts.
- Currencies beyond the three allow-listed.
