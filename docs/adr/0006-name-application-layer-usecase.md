# 0006. Name the application layer `domain/usecase`

**Status:** Accepted

## Context

ADR-0001 placed use-case implementations in `domain/service`. The package name has drifted from what it holds: every class in it is named `…UseCase`, and the classes are application services that *orchestrate* the driven ports (repository, acquiring bank) and the validation rules — not pure domain services. "Service" is a vague Spring stereotype that undersells this and reads as a technical layer rather than the application core.

We considered three options:

- **Keep `domain/service`** — consistent with ADR-0001 but keeps the naming mismatch.
- **Promote to a top-level `application/` layer** — the most orthodox hexagonal split (pure `domain/` vs orchestrating `application/`), but a larger conceptual move.
- **Rename to `domain/usecase`** — the package name then matches the `…UseCase` class names with minimal disruption.

This packages each use case by feature *within* the application layer, while the shared `Payment` model and the ports stay in `domain/model` and `domain/port` respectively — so per-use-case packaging does not fracture the shared model. We keep the two use cases symmetric (each in its own subpackage) rather than leaving the trivial one flat, so the layout reads consistently as "one package per use case."

## Decision

We will rename `domain/service` to `domain/usecase` and give each use case its own subpackage named after it. A use case's subpackage holds the use case plus the collaborators it owns: `ProcessPaymentUseCase`, its sibling `ProcessPaymentUseCaseSteps` `@Component`, and the `ValidationRule<PaymentCommand>` components live together under `domain/usecase/processpayment` (validation at `domain/usecase/processpayment/validation`); `GetPaymentUseCase` lives under `domain/usecase/getpayment`. Use cases remain Spring `@Service` beans named `…UseCase`. This colocates the validation rules with the only flow that consumes them, rather than presenting them as a shared sibling. The shared domain model, ports, adapters, and config from ADR-0001 are unchanged.

## Consequences

Positive: the package name now states what it contains; navigation is clearer and the layer's role (application orchestration) is no longer hidden behind a generic "service" label.

Negative: a one-time package rename touching imports across the codebase, and a documentation drift cost — CLAUDE.md and ADR-0001's `domain/service` references must be read together with this ADR. We do not edit ADR-0001 (it stays as the accepted record of its time); this ADR refines its package naming.
