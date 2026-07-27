---
name: alpha-vue-api-design
description: Design or change Alpha Vue REST API contracts across the Spring Boot backend and Vue frontend. Use when adding or changing endpoints, request or response models, pagination, filtering, permissions, file operations, OpenAPI annotations, API compatibility, or frontend service contracts.
---

# Alpha Vue API Design

Treat `docs/api.md` and `docs/security.md` as the baseline contract. Preserve existing public behavior unless the request explicitly authorizes a breaking change.

## Load Context

1. Read `docs/api.md`, `docs/security.md`, and `docs/conventions.md` completely for every API contract change.
2. Inspect the nearest backend controller, DTO, VO, service, OpenAPI configuration, frontend `src/service` module, and their tests.
3. Read relevant contract memory through `docs/ai/README.md` when it exists.
4. Use `requirements-explore` when the resource, actor, operation, or acceptance behavior is not confirmed.

## Design The Contract

1. Define the actor, permission code, resource, operation, and success or failure behavior.
2. Use the project API prefix, `ApiResponse<T>`, `traceId`, and `page` / `size` pagination conventions. Keep HTTP status and response `code` aligned.
3. Specify HTTP method, path, path/query parameters, request DTO, response VO, validation boundaries, authorization, and stable error outcomes.
4. For list endpoints, define filtering, sort behavior, page reset behavior, empty results, and upper bounds where relevant.
5. For mutations, define idempotency expectations, audit requirements, transaction ownership, refresh behavior in the frontend, and whether existing clients remain compatible.
6. For files, authentication, Redis, or operational APIs, apply the additional security constraints in `docs/security.md`; do not expose internal values, secrets, or implementation details.
7. Update or plan matching Springdoc/Knife4j annotations and typed frontend service contracts. Do not introduce a separate OpenAPI YAML, `/api/v1` prefix, cursor pagination, RFC 7807 envelope, or JWT scheme unless the project contract is explicitly changed.

## Output

Provide a compact contract table with method, path, authorization, inputs, success data, and failure cases. State affected backend and frontend files, compatibility implications, and focused tests. For implementation, hand off to `alpha-vue-backend` and `alpha-vue-frontend` as needed.

## Boundaries

- Do not implement an unconfirmed business rule merely because a REST pattern suggests it.
- Do not let frontend route visibility replace backend authorization.
- Do not use this Skill for a purely internal Java refactor with no contract effect; use `alpha-vue-backend` instead.
