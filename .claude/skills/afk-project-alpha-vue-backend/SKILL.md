---
name: afk-project-alpha-vue-backend
description: Implement, refactor, debug, or review Alpha Vue Spring Boot and
  Java code under alpha-server. Use for controllers, services, MyBatis-Plus and
  XML mappers, entities, DTOs, VOs, Sa-Token authorization, transactions, Flyway
  migrations, OpenAPI annotations, file, Redis, HikariCP, SQL monitor
  infrastructure, backend tests, and backend module structure.
---

# Alpha Vue Backend

Follow the existing Spring MVC, MyBatis-Plus, MySQL, Flyway, Sa-Token, and OpenAPI patterns while preserving the user's requested level of action.

## Load Context

1. Read `docs/conventions.md`, `docs/security.md`, and `docs/development.md` before backend decisions or edits.
2. Read `docs/api.md` for public contract changes and use `alpha-vue-api-design` when the contract is being designed or changed.
3. Inspect `alpha-server/pom.xml`, nearby domain code, XML mappers, migrations, test fixtures, and `git status --short` before editing.
4. Read `docs/ai/README.md` and only relevant indexed memory when domain or contract context is needed.

## Classify The Task

- For review, explanation, or diagnosis requests, inspect and report with evidence; do not edit files unless the user explicitly asks for a fix.
- For implementation, refactoring, or explicitly requested fixes, make the smallest coherent change and verify it in proportion to risk.
- Use `debugging` for reproducible root-cause analysis and stop after the diagnosis when that is all the user requested.
- Use `java-code-review` for a risk-focused Java review; this Skill supplies project context but does not turn review into implementation.

## Preserve Architecture

- Place shared contracts in `common`, technical adapters in `framework`, and business code in `modules/<domain>`.
- Keep Controllers limited to HTTP, validation, permissions, and unified responses; inherit `framework.web.BaseController` for response wrapping, traceId, client IP, and current login user id instead of duplicating those helpers. Keep business rules and transaction boundaries in Services; keep persistence in Mappers and XML.
- Keep controller base classes lightweight. Do not introduce generic CRUD controllers that expose entities, infer permission prefixes, or hide service-level authorization and transaction decisions.
- Use immutable request DTO records and explicit `*Vo` responses. Never expose entities or sensitive fields as controller input or output.
- Use project permission codes and backend authorization on every protected entry point. Frontend visibility never authorizes access.
- Use `BusinessException`, the global exception handler, parameterized SLF4J logging, MDC trace IDs, and safe audit metadata.
- Keep HikariCP and SQL monitor features operationally safe: Hikari leak detection is disabled by default and only enabled in a controlled diagnostic window; SQL logs may store only bounded, in-memory, placeholder SQL summaries and must not expose real parameters or arbitrary SQL execution.

## Implement Safely

1. Trace the request from controller through service, mapper, entity, and response model before changing it.
2. Validate untrusted inputs at the HTTP boundary and preserve the established API envelope and error behavior.
3. Keep custom MyBatis SQL in `src/main/resources/mapper/<domain>/*.xml`; do not use MyBatis SQL annotations.
4. Make Flyway changes append-only. Before changing mappings, raw SQL, destructive behavior, or persistent data semantics, explain the impact and obtain the required confirmation.
5. Use explicit transaction boundaries for multi-step writes. Avoid unbounded reads, `KEYS` on Redis, N+1 persistence paths, sensitive logs, SQL parameter logging, and broad updates or deletes without a filter.
6. Keep configuration typed and externalized. Do not add JPA, WebFlux, Spring Security/JWT, or cloud infrastructure patterns that conflict with the existing stack without an approved architecture decision.
7. Add focused tests for changed security, permissions, transactions, validation, mapper behavior, and public contracts. Assert behavior and failure paths; do not rely only on MockMvc for real HTTP authentication.

## Verify

Run focused tests first, then run the relevant backend checks:

```bash
./mvnw -f alpha-server/pom.xml test
./mvnw -f alpha-server/pom.xml package
git diff --check
```

For API or cross-layer changes, report contract compatibility and the frontend work required. For deployment-relevant changes, also follow `docs/operations.md` and validate Compose configuration when in scope.

## Boundaries

- Do not substitute generic JPA repository, WebFlux, OAuth2/JWT, or microservice templates for the project architecture.
- Do not execute database DDL or DML directly; generate reviewed migration or operational scripts only.
- Do not replace `java-code-review` or `debugging`; combine them once with this project's context without recursively restarting the workflow.
