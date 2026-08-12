---
name: alpha-vue-architecture
description: Design or review Alpha Vue system boundaries, module ownership, technology choices, significant non-functional requirements, and architecture changes. Use for architecture design, module split or merge decisions, cross-layer design, technical trade-offs, scalability or operational decisions, and ADR discussions in this Vue 3 and Spring Boot project.
---

# Alpha Vue Architecture

Design the smallest architecture that satisfies confirmed requirements and preserves the current monolith unless a demonstrated constraint requires change.

## Load Context

1. Read `docs/conventions.md` and the relevant `docs/api.md`, `docs/security.md`, `docs/development.md`, or `docs/operations.md`.
2. Read `docs/ai/README.md` when business, service, or contract memory is relevant, then load only the indexed material needed.
3. Inspect the affected `alpha-web`, `alpha-web-soybean`, `alpha-server`, `deploy`, tests, and nearby implementation before proposing a change. Treat the two frontends as separate clients of the shared backend; do not assume one frontend's structure or behavior defines the other.
4. If requirements are unclear, use `requirements-explore`; do not invent product scope.

## Design

1. State the problem, confirmed constraints, non-goals, and the current architecture that will remain unchanged.
2. Identify ownership across `alpha-web`, `alpha-web-soybean`, `alpha-server`, `deploy`, and `docs`; keep backend responsibilities in `common`, `framework`, or `modules/<domain>` as appropriate.
3. Compare the smallest viable option with only materially different alternatives. Cover security, data consistency, failure modes, operations, testing, and migration impact when applicable.
4. Select one option and state its trade-offs. Use a compact Mermaid diagram only when component relationships are not clear in prose.
5. Define API, permission, persistence, deployment, and verification consequences before implementation planning.

## Output

Provide:

- Decision summary and non-goals.
- Current and proposed ownership or component relationships.
- Alternatives considered and why they were not selected.
- Risks, failure modes, and validation evidence required.
- Any follow-up API, backend, frontend, deployment, or migration work.

Do not create an ADR or design document unless the user explicitly asks to persist the decision. When persistence is requested, first follow the repository's chosen documentation location and naming convention; do not create an ADR directory by assumption.

## Boundaries

- Do not use architecture work to justify speculative microservices, infrastructure, caches, queues, or generic abstractions.
- Do not replace `planning`: architecture chooses boundaries and trade-offs; planning sequences approved work.
- Do not replace `alpha-vue-api-design`, `alpha-vue-backend`, or `alpha-vue-frontend`: invoke them for the corresponding detailed design or implementation work.
