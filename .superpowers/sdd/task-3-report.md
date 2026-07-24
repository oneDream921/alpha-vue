# Task 3 report: authentication and audit foundations

## Delivered

- Added Redis-backed Sa-Token storage for non-test profiles and a no-Docker in-memory Sa-Token/login-limit configuration for `test`.
- Added BCrypt login, username-and-IP Redis failure limits, 8-hour sessions, 30-minute activity timeout, `Authorization: Bearer <token>` extraction, profile/routes/logout endpoints, and database-backed Sa-Token roles/permissions.
- Added a global Sa-Token interceptor that protects all non-static paths except login and actuator health.
- Added asynchronous login/operation audit persistence. Operation request data is always stored as `[redacted]`; credentials and response payloads are never logged.

## TDD evidence

1. Added `AuthControllerTests` before the authentication implementation.
2. RED: `mvn -q -Dtest=AuthControllerTests test` initially failed as expected because `/api/auth/*` was not implemented (expected `401`/`200`, received `500` from the missing endpoint handler).
3. GREEN: the same targeted suite passes all three cases: invalid credentials, missing token, and successful Bearer-token profile retrieval.

## Validation

- `mvn -q -Dtest=AuthControllerTests test` — passed: 3 tests, 0 failures/errors.
- `mvn -q test` — passed: 8 tests, 0 failures/errors.
- `mvn -q package -DskipTests` — passed.

The environment has no local Redis executable, so the test profile intentionally uses Sa-Token's in-memory DAO and in-memory failure-limit store; production/dev profiles use Redis-backed implementations without this Docker dependency.
