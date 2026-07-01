# 0008. Model amount + currency as a Money value object

**Status:** Accepted

## Context

`Payment` and the bank `AuthorizationCommand` previously carried `long amount` and
`String currency` as independent primitives, threaded through several constructors. In a
payments domain this is the textbook case of primitive obsession: nothing keeps the amount
and its currency together, which invites mismatched-unit and cross-currency arithmetic bugs —
exactly the class of error a payment gateway must not make. "Avoid over-engineering"
(REQUIREMENTS) argues against speculative abstractions, but a `Money` type is not speculative
here; it is the core domain concept.

## Decision

We will introduce `domain/model/Money(long amount, String currency)` as a pure value object.
Its compact constructor enforces invariants — currency is a 3-letter ISO 4217 code and amount
is a positive integer in the minor currency unit — throwing `IllegalArgumentException` on
violation. `Payment` and `AuthorizationCommand` hold a `Money` instead of loose primitives.

`Money` is only constructed **after** request validation has passed (the rule-based
`ValidationRule` components still operate on the raw `PaymentCommand` fields and return
structured errors). Because validation runs first, `Money`'s invariants act as a defensive
backstop and never surface as a 500 on well-formed-but-invalid input — that path is already a
400 via `ValidationException`.

## Consequences

Positive: amount and currency can no longer drift apart; the invariant lives in one place; the
domain reads in the ubiquitous language of the problem.

Negative: there are now two representations of "amount + currency" — the raw, possibly-invalid
fields on the inbound `PaymentCommand`, and the validated `Money` in the domain. This split is
deliberate (the command is the unvalidated edge; `Money` is the trusted core), but it must be
understood to avoid duplicating validation into `Money`'s constructor as user-facing errors.
