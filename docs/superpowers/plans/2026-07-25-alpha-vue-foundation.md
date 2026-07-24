# Alpha Vue Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable, responsive Alpha Vue administration foundation with login, RBAC, files, logs, Flyway migrations, Docker support, and baseline tests.

**Architecture:** Keep a Vue single-page admin application and a Spring Boot single service in separate top-level directories. The backend owns authentication, authorization, validation, storage, auditing, and database migrations; the frontend consumes its routes and permissions to render the same functionality at every breakpoint.

**Tech Stack:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Ant Design Vue, Tailwind CSS v4, Axios, Java 21, Spring Boot 4, MyBatis-Plus, Sa-Token, MySQL, Redis, Flyway, MinIO, SLF4J/Logback.

## Global Constraints

- Use Java 21 and Spring Boot 4.
- Use MyBatis-Plus and Sa-Token; do not introduce JWT.
- Use Ant Design Vue for business components and Tailwind CSS v4 only for layout and visual composition.
- Keep a single backend module and a single frontend application.
- Every protected API requires Bearer Token authentication and backend permission validation.
- Do not persist passwords, Tokens, Cookies, captchas, request bodies, or secret/key fields in audit logs.
- Keep mobile functionality complete by changing layout rather than removing actions.
- Flyway migrations contain only schema and minimal demonstration data.

---

### Task 1: Repository, backend build, and infrastructure baseline

**Files:**
- Create: `README.md`, `.gitignore`, `.editorconfig`, `deploy/docker-compose.yml`, `deploy/.env.example`
- Create: `alpha-server/pom.xml`, `alpha-server/src/main/java/io/github/onedream921/alphavue/AlphaVueApplication.java`
- Create: `alpha-server/src/main/resources/application.yml`, `alpha-server/src/main/resources/application-dev.yml`, `alpha-server/src/main/resources/application-prod.yml`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/AlphaVueApplicationTests.java`

**Interfaces:**
- Produces Spring Boot application artifact `alpha-server` and Docker services `mysql`, `redis`, and `minio`.

- [ ] Write a context-load test annotated with `@SpringBootTest`.
- [ ] Run `mvn test -DskipTests=false` and verify the test initially fails because the project files do not exist.
- [ ] Add the Maven project using Spring Boot Web, Validation, Test, MyBatis-Plus, Flyway, MySQL, Redis, Sa-Token, BCrypt, Lombok, and OpenAPI dependencies.
- [ ] Add application bootstrap and profile configuration, with `dev` as the local profile and all credentials read from environment variables.
- [ ] Add Docker Compose services for MySQL 8, Redis 7, and MinIO with health checks and named volumes.
- [ ] Run `mvn test` and `docker compose -f deploy/docker-compose.yml config`.
- [ ] Commit with `chore: scaffold backend and local infrastructure`.

### Task 2: Common API contract, trace IDs, and Flyway schema

**Files:**
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/common/api/ApiResponse.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/common/api/PageResponse.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/common/exception/BusinessException.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/common/exception/GlobalExceptionHandler.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/framework/web/TraceIdFilter.java`
- Create: `alpha-server/src/main/resources/db/migration/V1__initial_schema.sql`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/common/api/ApiResponseTests.java`

**Interfaces:**
- Produces `ApiResponse.success(T data, String traceId)` and standardized error responses with `code`, `message`, `data`, and `traceId`.
- Produces the tables `sys_user`, `sys_role`, `sys_menu`, `sys_user_role`, `sys_role_menu`, `sys_dept`, `sys_config`, `sys_file`, `sys_login_log`, and `sys_oper_log`.

- [ ] Write tests asserting successful responses use code 200 and error responses preserve the supplied trace ID.
- [ ] Run the tests and verify they fail before implementing the response types.
- [ ] Implement immutable response records, page records, business exception mapping, and a servlet filter that sets `X-Trace-Id`.
- [ ] Write Flyway DDL with indexes and soft-delete columns where required, then seed only `admin`, `SUPER_ADMIN`, role-menu relations, and menus.
- [ ] Run backend tests and start against the Compose MySQL service to verify Flyway migrates successfully.
- [ ] Commit with `feat: add API contract and versioned database schema`.

### Task 3: Authentication, session security, and audit foundations

**Files:**
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/framework/security/SaTokenConfig.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/framework/security/StpInterfaceImpl.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/auth/AuthController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/auth/AuthService.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/auth/dto/LoginRequest.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/auth/dto/LoginResponse.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/log/OperationLog.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/log/OperationLogAspect.java`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/auth/AuthControllerTests.java`

**Interfaces:**
- Produces `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/profile`, and `GET /api/auth/routes`.
- Consumes a BCrypt password hash and Redis-backed Sa-Token session.

- [ ] Write MockMvc tests for invalid credentials, successful login, missing token, and protected profile retrieval.
- [ ] Run the tests and verify they fail before security configuration exists.
- [ ] Implement login failure limits keyed by username and IP in Redis, BCrypt verification, 8-hour sessions, 30-minute activity timeout, and Bearer Token extraction.
- [ ] Implement Sa-Token permission lookup and a global interceptor that excludes only login, actuator health, and static resources.
- [ ] Implement asynchronous login and operation audit services that redact sensitive request fields.
- [ ] Run tests with Redis available and verify unauthenticated protected endpoints return 401.
- [ ] Commit with `feat: add Redis-backed login and audit foundations`.

### Task 4: System RBAC management

**Files:**
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/*.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/*.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/*.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/UserController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/RoleController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/MenuController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DeptController.java`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/RbacControllerTests.java`

**Interfaces:**
- Produces CRUD endpoints under `/api/system/users`, `/roles`, `/menus`, and `/depts`.
- Enforces permissions including `system:user:list`, `system:user:create`, `system:role:assign`, and `system:menu:update`.

- [ ] Write authorization tests proving a non-admin user cannot call user-management endpoints and a permitted user can list records.
- [ ] Run the tests and verify they fail before RBAC controllers exist.
- [ ] Implement MyBatis-Plus entities, mappers, services, validation DTOs, soft delete behavior, and paginated endpoints.
- [ ] Implement safe role/menu assignment and protect the built-in SUPER_ADMIN role from deletion.
- [ ] Run tests and verify user/role/menu changes are captured by operation audit.
- [ ] Commit with `feat: add RBAC management APIs`.

### Task 5: Storage providers and file APIs

**Files:**
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/file/StorageProvider.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/file/LocalStorageProvider.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/file/MinioStorageProvider.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/file/FileController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/file/FileService.java`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/file/FileServiceTests.java`

**Interfaces:**
- Produces `POST /api/files/upload`, `GET /api/files`, and `DELETE /api/files/{id}`.
- `StorageProvider` exposes `store(String key, InputStream input, String contentType)`, `delete(String key)`, and `publicUrl(String key)`.

- [ ] Write tests for rejected extension, rejected size, successful local upload, and delete ordering.
- [ ] Run the tests and verify they fail before provider implementations exist.
- [ ] Implement configuration-backed allowed types and size, UUID-based object keys, local path traversal prevention, and MinIO provider construction.
- [ ] Add MyBatis-Plus file metadata persistence and operation audit annotations.
- [ ] Run focused tests and a manual multipart upload against the running backend.
- [ ] Commit with `feat: add pluggable file storage`.

### Task 6: Vue application shell, responsive layout, and API client

**Files:**
- Create: `alpha-web/package.json`, `alpha-web/vite.config.ts`, `alpha-web/tailwind.config.ts`, `alpha-web/src/main.ts`, `alpha-web/src/App.vue`
- Create: `alpha-web/src/service/http.ts`, `alpha-web/src/service/auth.ts`
- Create: `alpha-web/src/router/index.ts`, `alpha-web/src/router/guard.ts`
- Create: `alpha-web/src/stores/auth.ts`, `alpha-web/src/layouts/BaseLayout.vue`
- Create: `alpha-web/src/styles/index.css`, `alpha-web/src/styles/tokens.css`
- Test: `alpha-web/src/service/http.test.ts`, `alpha-web/src/layouts/BaseLayout.test.ts`

**Interfaces:**
- Consumes backend `ApiResponse`, Bearer Tokens, profile data, and routes.
- Produces a Vite application with a mobile drawer navigation below 768px and collapsible desktop navigation at or above 1024px.

- [ ] Write Vitest tests for attaching a token, handling 401 by clearing the store, and rendering drawer navigation on small viewports.
- [ ] Run `pnpm test` and verify it fails before the application exists.
- [ ] Configure Vite, Vue, TypeScript, Tailwind v4, Ant Design Vue, Vitest, ESLint, and Prettier.
- [ ] Implement the HTTP client, auth store, route guard, design tokens, and responsive layout breakpoints.
- [ ] Run typecheck, unit tests, and production build.
- [ ] Commit with `feat: add responsive Vue application shell`.

### Task 7: Vue login, dynamic routes, and management pages

**Files:**
- Create: `alpha-web/src/views/login/index.vue`, `alpha-web/src/views/home/index.vue`, `alpha-web/src/views/profile/index.vue`
- Create: `alpha-web/src/views/system/{users,roles,menus,depts,logs}.vue`
- Create: `alpha-web/src/views/files/index.vue`, `alpha-web/src/views/errors/{403,404}.vue`
- Create: `alpha-web/src/directives/permission.ts`
- Test: `alpha-web/src/stores/auth.test.ts`, `alpha-web/src/directives/permission.test.ts`

**Interfaces:**
- Consumes `auth.login`, `auth.profile`, `auth.routes`, RBAC endpoints, file endpoints, and permission string arrays.
- Produces login, profile, RBAC, file, and audit-log views with permission-aware buttons.

- [ ] Write tests for successful login state, rejected login state, dynamic route registration, and removal of a button when its permission is absent.
- [ ] Run the tests and verify they fail before the views and directive exist.
- [ ] Implement form validation, login redirect behavior, dynamic sidebar route rendering, data tables, create/edit dialogs, and upload UI.
- [ ] Verify tables scroll horizontally on phone-sized viewports while create, edit, delete, upload, and filter actions remain reachable.
- [ ] Run typecheck, unit tests, and production build.
- [ ] Commit with `feat: add login RBAC and file management views`.

### Task 8: Documentation, integration verification, and delivery audit

**Files:**
- Create: `docs/development.md`, `docs/api.md`, `docs/security.md`
- Modify: `README.md`
- Test: `deploy/smoke-test.sh`

**Interfaces:**
- Produces documented local startup, required environment variables, API contract, and smoke test commands.

- [ ] Write smoke assertions for health, unauthorized protected endpoint, login, authorized profile, list users, and multipart file upload.
- [ ] Start dependencies with Docker Compose and run Flyway migration from a clean database.
- [ ] Run Maven tests/package, pnpm typecheck/test/build, and browser-responsive checks at desktop, tablet, and phone widths.
- [ ] Run `git diff --check`, inspect tracked files for credentials, and verify no audit seed rows or tokens exist in migrations.
- [ ] Update the README with exact commands and compose URLs.
- [ ] Commit with `docs: add Alpha Vue development and security guides`.
