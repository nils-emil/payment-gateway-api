# Payment Gateway

A REST API that lets merchants process card payments through a simulated acquiring bank and retrieve previously made payments by ID. Built with Java 21 and Spring Boot 3, using a hexagonal (ports & adapters) architecture to keep domain logic framework-free and independently testable.

## Prerequisites

- Java 21
- Docker (for the bank simulator)

## Build and run

**Build and run all tests:**
```bash
./gradlew clean build
```

**Unit tests only** (fast, no Spring context — use for the TDD inner loop):
```bash
./gradlew test
```

**Integration tests only** (Spring slice/context tests):
```bash
./gradlew integrationTest
```

**Run the application** (serves on http://localhost:8090):
```bash
./gradlew bootRun
```

## Bank simulator

The gateway forwards valid payment requests to a simulated acquiring bank running on port 8080.

Start the simulator with Docker:
```bash
docker-compose up
```

The gateway's bank base URL is configured by `payment.bank.base-url` in `src/main/resources/application.yml`. Override it via environment variable if needed; never hardcode it.

## API endpoints

### Process a payment

```
POST /payments
```

Request body fields: `card_number`, `expiry_month`, `expiry_year`, `currency` (ISO 4217; GBP/USD/EUR), `amount` (integer, minor currency unit), `cvv`.

Returns `200` with the payment record on success (`Authorized` or `Declined`). Returns `400` with a list of validation errors if the request is invalid — no payment is created in that case. Returns `502` if the bank is unavailable.

### Retrieve a payment

```
GET /payments/{id}
```

Returns `200` with the payment record (including `Pending` if the bank was unreachable when it was processed). Returns `404` if the ID is unknown.

Response fields: `id`, `status`, `last_four`, `expiry_month`, `expiry_year`, `currency`, `amount`, and `authorization_code` (when present). CVV and full card number are never stored or returned.

## Architecture decisions

Design rationale is recorded as ADRs in [`docs/adr/`](docs/adr/):

- [ADR-0001](docs/adr/0001-use-hexagonal-architecture.md) — Hexagonal architecture and domain purity
- [ADR-0002](docs/adr/0002-persist-first-payment-lifecycle.md) — Persist-first payment lifecycle and `Pending` status
- [ADR-0003](docs/adr/0003-rejected-as-400-no-resource.md) — Rejected as HTTP 400 with no persisted resource
- [ADR-0004](docs/adr/0004-in-memory-repository.md) — In-memory repository
- [ADR-0005](docs/adr/0005-separate-unit-and-integration-source-sets.md) — Separate unit and integration source sets
