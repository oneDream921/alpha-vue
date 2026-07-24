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
