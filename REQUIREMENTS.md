# Payment Gateway — Requirements

## Overview

Build a payment gateway API that lets merchants:

1. **Process** a customer's card payment through the gateway.
2. **Retrieve** the details of a previously made payment by its identifier.

## Payment outcomes

A processed payment ends in one of three states:

- **Authorized** — the payment was authorized by the call to the acquiring bank.
- **Declined** — the payment was declined by the call to the acquiring bank.
- **Rejected** — no payment was created because invalid information was supplied.
  The gateway never calls the bank in this case.

## Process a payment

### Request fields & validation

| Field        | Rules                                                        |
|--------------|-------------------------------------------------------------|
| Card number  | Required; 14–19 numeric characters.                         |
| Expiry month | Required; 1–12.                                              |
| Expiry year  | Required; month + year combination must be in the future.   |
| Currency     | Required; 3-character ISO 4217 code; validate against an allow-list of no more than 3 currencies. |
| Amount       | Required; integer in the minor currency unit.               |
| CVV          | Required; 3–4 numeric characters.                           |

### Response fields

- Payment ID (a GUID is acceptable).
- Status — `Authorized` or `Declined`.
- Last four digits of the card number (the full number is never returned).
- Expiry month and year.
- Currency (ISO code).
- Amount (minor currency unit).

CVV is never stored or returned.

## Retrieve a payment

Returns the same fields as the process-payment response, looked up by payment ID,
so merchants can access payment history for reconciliation.

## Bank simulator

The gateway forwards valid requests to a simulated acquiring bank.

- **Endpoint:** `POST http://localhost:8080/payments`
- **Setup:** run via `docker-compose up` (Mountebank with EJS templates).
- Configure the base URL via `application.yml` / env var — never hardcode it.

### Request body

```json
{
  "card_number": "2222405343248877",
  "expiry_date": "04/2025",
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

### Response body

```json
{
  "authorized": true,
  "authorization_code": "0bb07405-6d44-4b50-a14f-7ae0beff13ad"
}
```

### Simulator behaviour

- Missing fields → `400 Bad Request`.
- Card number ending in an odd digit → `200 OK`, authorized.
- Card number ending in an even digit → `200 OK`, declined.
- Card number ending in `0` → `503 Service Unavailable`.

## Constraints

- Code must compile and have automated test coverage.
- Keep it simple and maintainable — avoid over-engineering.
- A mock/in-memory repository is acceptable for storage; no real database needed.
- API design should focus on the stated functional requirements.
