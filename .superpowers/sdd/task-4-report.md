# Task 4 report: system RBAC management

## Delivered

- Added paginated CRUD APIs under `/api/system/users`, `/roles`, `/menus`, and `/depts`.
- Added MyBatis-Plus entities, mappers, services, request validation, and `deleted = 1` soft-delete behavior against the existing `sys_*` schema.
- Added permission gates for every endpoint, including `system:user:list`, `system:user:create`, `system:role:assign`, and `system:menu:update`. The seeded `SUPER_ADMIN` role is the only all-system bypass and cannot be deleted.
- Role/menu and user/role assignment validate every referenced active record before replacing the relationship set. Mutations carry `@OperationLog` annotations.
- Added the MyBatis-Plus JSqlParser companion dependency required by version 3.5.13 to run database-backed pagination.

## TDD evidence

1. Added `RbacControllerTests` before the RBAC production code.
2. Red: `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q -Dtest=RbacControllerTests test` exited 1 with four expected failures because `/api/system/*` controllers did not exist (all requests resolved to the missing-resource handler and returned 500 rather than the asserted RBAC responses).
3. Green: the same focused command exited 0 after implementation. The four tests cover denied authenticated access, permission-granted user listing with page metadata, validation plus soft deletion, SUPER_ADMIN deletion protection, safe role-menu assignment, and asynchronous operation-audit persistence for user and role/menu mutations.

## Final verification

- `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q test` — exit 0; 14 tests, 0 failures, 0 errors.
- `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q package` — exit 0.
- `git diff --check` — clean.

The existing global-exception test intentionally logs a synthetic exception during the full suite; Maven reported no test failures.

## Review follow-up: authorization lifecycle and audit semantics

- Every protected request now verifies that the authenticated `sys_user` remains active and non-deleted. A disabled or soft-deleted account has its existing Sa-Token session invalidated and receives 401 on its next protected request.
- Permission, role, and route lookups now join active, non-deleted users and roles, so disabling or soft-deleting an assigned role removes its effective permissions immediately.
- Role/menu and user/role assignment validate active, non-deleted referenced menus and roles before replacing relationship sets.
- Expected `BusinessException` outcomes retain their real HTTP code in asynchronous operation audits while keeping `status = 0` and `[redacted]` payloads; successful audit behavior is unchanged.

## Review follow-up verification

1. Red: `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q -Dtest=RbacControllerTests test` exited 1 before the fixes: pre-issued disabled-user tokens returned 200, disabled roles retained `system:user:list`, and disabled-role assignments returned 200.
2. Green: the same focused command exited 0 with 7 tests. New integration coverage verifies pre-issued tokens lose protected access on user disable/delete; disabled/deleted roles lose effective permissions after login; inactive role/menu assignments are rejected; and 400/403 business and authorization failures are asynchronously audited with redacted payloads and their actual response code.
3. `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q test` — exit 0; 17 tests, 0 failures, 0 errors.
4. `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -q package` — exit 0.
5. `git diff --check` — clean.
