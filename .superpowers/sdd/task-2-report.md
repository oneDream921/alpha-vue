# Task 2 report: API contract, trace IDs, and Flyway schema

## Delivered

- Added immutable `ApiResponse<T>` and `PageResponse<T>` records.
- Added `BusinessException` and a global REST exception handler that returns the
  common response envelope without exposing exception details.
- Added a highest-precedence `TraceIdFilter` that generates a UUID for each
  request, stores it as a request attribute, and returns `X-Trace-Id`.
- Replaced the initial Flyway probe script with `V1__initial_schema.sql`.
- Created the ten required system tables, indexes, soft-delete flags for
  mutable records, and only the initial admin/RBAC/menu data.

## TDD evidence

1. Added `ApiResponseTests` before the response implementation.
2. Ran `mvn -f alpha-server/pom.xml -Dtest=ApiResponseTests test` with a
   temporary Maven runtime; compilation failed because `ApiResponse` did not
   exist.
3. Implemented the response records and reran the focused test: 2 tests passed.

## Verification

- `mvn -f alpha-server/pom.xml clean test`: passed, 3 tests total.
- Started the application with the test classpath; Flyway applied V1 on a fresh
  H2 database and `GET /not-found` returned an `X-Trace-Id` response header.
- Compose/MySQL verification could not run because Docker CLI is unavailable in
  this environment (`docker: command not found`).

## Review remediation (2026-07-25)

- Removed the non-product `migration_probe` table from Flyway V1 and changed
  the baseline context assertion to require `sys_user` instead.
- Added `PublicErrorMessage`, so `BusinessException` accepts only a controlled
  client-safe message. The global handler no longer reads exception messages
  into responses and logs unexpected exception details with the trace ID.
- Added `GlobalExceptionHandlerTests` confirming business and unexpected
  exceptions return fixed public messages while preserving the request trace ID.
- TDD: the focused handler test first failed to compile because the controlled
  message type and exception-handler signature did not yet exist; it then
  passed after the implementation.
- Verification: `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml clean test`
  passed with 5 tests, including a fresh H2/Flyway migration that created
  `sys_user`.
