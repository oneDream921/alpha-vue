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

## Review remediation

- Replaced the separate lock check and failure increment with `reserveAttempt`, which reserves an attempt before account lookup or BCrypt verification. Redis executes the cap check, increment, and first-attempt 15-minute expiry in one Lua script; the test-profile implementation provides the same expiry-aware operation under a lock. This permits exactly five attempts per username/IP window and rejects later requests before BCrypt.
- `OperationLogAspect` now captures both the numeric login ID and its username before proceeding to the controller, so a logout cannot erase the actor before asynchronous audit persistence.

## Remediation TDD and validation

1. RED: `AuthControllerTests#admitsOnlyFiveSimultaneousFailedLoginAttemptsPerUsernameAndIp` sent ten barrier-synchronized bad requests. Before the fix, all ten received `401` (expected five `401` and five `429`).
2. RED: `AuthControllerTests#recordsLogoutOperationWithThePrincipalThatInitiatedIt` persisted a null `user_id` before the fix.
3. GREEN: both remediation tests pass, including the post-cap `429` check and persisted logout `user_id=1`, `username=admin` assertion.
4. `/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn -q -Dtest=AuthControllerTests test` — passed: 5 tests, 0 failures/errors.
5. `/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn -q test` — passed: 10 tests, 0 failures/errors.

`mvn` is not on this environment's PATH; the validated IntelliJ-bundled Maven executable was used for the commands above.
