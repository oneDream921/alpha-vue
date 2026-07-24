# Task 2 report: API contract, trace IDs, and Flyway schema

## Delivered

- Added immutable `ApiResponse<T>` and `PageResponse<T>` records.
- Added `BusinessException` and a global REST exception handler that returns the
  common response envelope without exposing exception details.
- Added a highest-precedence `TraceIdFilter` that generates a UUID for each
  request, stores it as a request attribute, and returns `X-Trace-Id`.
- Replaced the initial Flyway probe script with `V1__initial_schema.sql` while
  retaining the `migration_probe` table used by the Task 1 context test.
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
