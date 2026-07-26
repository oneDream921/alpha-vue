# 配置管理与 Redis 运维台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提供受 RBAC 和审计保护的业务配置管理，以及只管理 Alpha Vue 自有 Redis 键空间的 Redis 运维台。

**Architecture:** 配置管理以现有 `sys_config` 为唯一持久化来源，第一期只提供受限的 CRUD 和查询，不把基础设施参数或密钥写入数据库，也不隐式让任意配置热生效。Redis 运维台以受控前缀白名单、`SCAN` 游标分页和单键操作为边界，展示元数据而非认证、验证码等敏感值；它不提供全库扫描、全库清空或任意键写入。

**Tech Stack:** Java 21、Spring Boot 4、MyBatis-Plus、Flyway、Spring Data Redis/Lettuce、Sa-Token、Vue 3、TypeScript、Ant Design Vue、Vitest、JUnit 5、MockMvc。

## Global Constraints

- 自定义数据库访问遵循项目现有 Mapper XML 约定；实体仅承载持久化字段，DTO 接收输入，VO 返回输出，Service 承担事务和业务规则。
- 所有公开错误消息使用中文；每个写操作使用 `@OperationLog`，不记录配置值、Redis 键值或令牌内容。
- `sys_config` 禁止保存密钥、密码、令牌和 `spring.*`、`server.*`、`datasource.*`、`redis.*`、`minio.*`、`sa-token.*` 前缀的配置；这类配置继续只由环境变量提供。
- 第一期开关配置不直接改变 Spring、Sa-Token、Redis、MinIO 或 multipart 的运行参数；需要热生效的业务开关必须在后续需求中逐项注册、验证和测试。
- Redis 查询和删除只能匹配 `alpha.redis-management.prefixes` 中配置的前缀，开发默认 `auth:,satoken:`；禁止 `KEYS`、`FLUSHDB`、`FLUSHALL`、任意键写入和批量删除。
- Redis 的验证码、登录失败窗口和 Sa-Token 会话键只展示键名分类、Redis 类型、TTL 和大小估计，不返回值；删除会使登录状态或验证码失效，前端必须二次确认。
- 所有 Flyway 更改只新增迁移，不修改 V1--V9；菜单和按钮标题必须使用中文。

---

## Scope and release order

| 里程碑 | 可独立交付 | 不包含 |
| --- | --- | --- |
| A：配置管理 | 参数配置菜单、受限 CRUD、审计、RBAC、前端页面 | 任意配置热生效、配置导入导出、敏感配置托管 |
| B：Redis 运维台 | 概览、白名单前缀扫描、元数据、单键删除、审计、前端页面 | 任意 Redis 浏览器、查看敏感值、清空库、批量删除 |
| 后续门槛：字典 | 仅在业务出现可运营选项时立项 | 为状态枚举提前建模 |

## Planned file structure

| 路径 | 责任 |
| --- | --- |
| `alpha-server/src/main/resources/db/migration/V10__add_configuration_management.sql` | 扩展 `sys_config` 并植入配置管理菜单与权限 |
| `alpha-server/src/main/resources/db/migration/V11__add_redis_management_menu.sql` | 植入 Redis 运维菜单与权限 |
| `alpha-server/src/main/java/.../modules/system/entity/SysConfig.java` | 配置持久化实体 |
| `alpha-server/src/main/java/.../modules/system/mapper/SysConfigMapper.java` 与 `resources/mapper/system/SysConfigMapper.xml` | 配置查询 SQL |
| `alpha-server/src/main/java/.../modules/system/dto/ConfigRequests.java` | 配置创建、更新、分页输入 |
| `alpha-server/src/main/java/.../modules/system/vo/ConfigVo.java` | 配置响应模型 |
| `alpha-server/src/main/java/.../modules/system/service/ConfigService.java` | 配置校验、事务和 CRUD |
| `alpha-server/src/main/java/.../modules/system/controller/ConfigController.java` | `/api/system/configs` API 与权限检查 |
| `alpha-server/src/main/java/.../modules/monitor/...` | Redis 前缀策略、访问服务、DTO/VO、控制器 |
| `alpha-server/src/test/java/.../modules/system/ConfigControllerTests.java` | 配置 CRUD、权限、敏感键拒绝、审计测试 |
| `alpha-server/src/test/java/.../modules/monitor/RedisManagementControllerTests.java` | Redis 范围、游标、脱敏和删除安全测试 |
| `alpha-web/src/service/configs.ts`、`alpha-web/src/views/system/configs.vue` | 配置管理前端 API 和页面 |
| `alpha-web/src/service/redis.ts`、`alpha-web/src/views/monitor/redis.vue` | Redis 运维 API 和页面 |
| `alpha-web/src/router/index.ts`、`alpha-web/src/layouts/BaseLayout.vue` | 受控路由与中文导航 |
| `docs/api.md`、`docs/security.md`、`docs/operations.md` | API、权限边界和生产运维说明 |

### Task 1: 配置管理数据模型、权限与后端契约

**Files:**

- Create: `alpha-server/src/main/resources/db/migration/V10__add_configuration_management.sql`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysConfig.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysConfigMapper.java`
- Create: `alpha-server/src/main/resources/mapper/system/SysConfigMapper.xml`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/ConfigRequests.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/ConfigVo.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/ConfigService.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/ConfigController.java`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/ConfigControllerTests.java`

**Interfaces:**

- Produces `GET /api/system/configs?page=&size=&keyword=`，`GET /api/system/configs/{id}`，`POST /api/system/configs`，`PUT /api/system/configs/{id}`，`DELETE /api/system/configs/{id}`。
- Produces permissions `system:config:list`、`system:config:create`、`system:config:update`、`system:config:delete`。
- `ConfigRequests.Save(String configKey, String configValue, String description)` validates a lowercase dot-separated key (`^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$`), key length at most 128, nonblank value and description length at most 500.
- `ConfigVo` exposes `id`、`configKey`、`configValue`、`description`、`createdAt`、`updatedAt`; it never adds a generic public lookup endpoint.

- [ ] **Step 1: Write failing controller tests**

```java
mockMvc.perform(post("/api/system/configs")
        .header("Authorization", bearer(adminToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"configKey\":\"ui.page-size\",\"configValue\":\"20\",\"description\":\"列表默认页大小\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.configKey").value("ui.page-size"));

mockMvc.perform(post("/api/system/configs")
        .header("Authorization", bearer(adminToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"configKey\":\"redis.password\",\"configValue\":\"x\",\"description\":\"invalid\"}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.message").value("请求参数错误"));
```

- [ ] **Step 2: Run the new tests and confirm they fail because the route does not exist**

Run: `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml -Dtest=ConfigControllerTests test`

Expected: test compilation or HTTP assertions fail before implementation.

- [ ] **Step 3: Add V10 migration and persistence model**

Add a non-null `name VARCHAR(100)` column with default empty string only if a short display name is required by the UI; otherwise keep the existing `config_key`/`description` schema and do not add unused fields. Insert menu id 27 (`参数配置`, component `system/configs`, permission `system:config:list`) below `系统管理`, plus four invisible BUTTON records for the produced permissions. Add the new menu ids to `SUPER_ADMIN` in `sys_role_menu` using `WHERE NOT EXISTS` guards.

Implement `SysConfig` from `SystemEntity`, XML-backed keyword pagination ordered by `id DESC`, and `ConfigService` methods `page`、`get`、`create`、`update`、`delete`. The service must reject duplicate active keys, forbidden prefixes and removal of records flagged as built-in in the migration; it must not cache values in Redis.

- [ ] **Step 4: Implement controller and audit boundaries**

Use `SystemAccessService.require` in every endpoint. Apply `@OperationLog(module = "System", operation = "创建参数配置", type = BusinessType.CREATE)` (and the Chinese update/delete equivalents) with `saveRequest = false` and `saveResponse = false`. Return existing `ApiResponse` and `PageResponse` shapes and trace id handling used by `MenuController`.

- [ ] **Step 5: Run focused and complete backend tests**

Run:

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml -Dtest=ConfigControllerTests test
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml test
```

Expected: configuration authorization, validation, uniqueness, soft deletion and audit assertions pass; Flyway reaches V10 in H2.

- [ ] **Step 6: Commit the backend milestone**

```bash
git add alpha-server/src/main alpha-server/src/test
git commit -m "feat: add configuration management api"
```

### Task 2: 配置管理前端与验收

**Files:**

- Create: `alpha-web/src/service/configs.ts`
- Create: `alpha-web/src/views/system/configs.vue`
- Create: `alpha-web/src/views/system/configs.test.ts`
- Modify: `alpha-web/src/router/index.ts`
- Modify: `alpha-web/src/layouts/BaseLayout.vue`
- Modify: `alpha-web/src/router/routes.test.ts`

**Interfaces:**

- Consumes the Task 1 API and `system:config:*` permissions.
- Produces a Chinese `参数配置` route at `/system/configs` and a table with keyword search, pagination, create, edit and delete flows.

- [ ] **Step 1: Write failing front-end tests**

```ts
expect(configApi.page).toHaveBeenCalledWith(1, 10, 'page')
expect(wrapper.text()).toContain('参数配置')
expect(wrapper.text()).not.toContain('redis.password')
```

Mock the API response with `ui.page-size`; verify the submit button is absent without `system:config:create`, and verify forbidden-key validation is shown before a request is sent.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `pnpm --dir alpha-web test --run src/views/system/configs.test.ts`

Expected: module or route cannot be resolved before implementation.

- [ ] **Step 3: Implement service, route and page**

Define typed `Config` and `ConfigSave` models in `configs.ts`. Add `system/configs` only to the route whitelist and to `BaseLayout` with the fixed Chinese label `参数配置`; do not reintroduce backend-title fallback. The page must use Ant Design Vue form validation, a masked placeholder for forbidden configuration categories, and a modal confirmation before deletion.

- [ ] **Step 4: Run front-end verification**

Run:

```bash
pnpm --dir alpha-web test --run
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
```

Expected: all tests, static checks and production build pass.

- [ ] **Step 5: Perform browser acceptance and commit**

Login as an administrator; create, search, edit and delete `ui.page-size`; verify a restricted user cannot see or call the page; inspect the audit log without configuration values. Then commit:

```bash
git add alpha-web
git commit -m "feat: add configuration management page"
```

### Task 3: Redis 管理安全边界与后端 API

**Files:**

- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/config/RedisManagementProperties.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/service/RedisManagementService.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/controller/RedisManagementController.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/dto/RedisKeyQuery.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/vo/RedisOverviewVo.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/vo/RedisKeyPageVo.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/monitor/vo/RedisKeyMetadataVo.java`
- Modify: `alpha-server/src/main/resources/application.yml`
- Create: `alpha-server/src/main/resources/db/migration/V11__add_redis_management_menu.sql`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/monitor/RedisManagementControllerTests.java`

**Interfaces:**

- Produces `GET /api/monitor/redis/overview`、`GET /api/monitor/redis/keys?prefix=&cursor=&count=`、`GET /api/monitor/redis/key?key=`、`DELETE /api/monitor/redis/key?key=`.
- Produces permissions `monitor:redis:list` and `monitor:redis:delete`.
- `RedisKeyPageVo(List<RedisKeyMetadataVo> records, String nextCursor, boolean hasMore)` returns opaque scan cursors; `count` is constrained to 1--100.
- `RedisKeyMetadataVo(String key, String category, String type, Long ttlSeconds, Long sizeBytes, boolean valueRedacted)` contains no Redis value field.

- [ ] **Step 1: Write failing safety tests using a fake keyspace**

Introduce a package-private `RedisKeyspace` interface so controller tests use an in-memory fake rather than a Docker Redis dependency:

```java
interface RedisKeyspace {
    RedisScanResult scan(String prefix, String cursor, int count);
    RedisKeyMetadata metadata(String key);
    boolean delete(String key);
    RedisOverview overview();
}
```

Test that `auth:captcha:*` returns `valueRedacted=true`, `other-app:key` returns 400, a caller with list but not delete permission gets 403 on `DELETE`, and no test calls `KEYS` or a flush command.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml -Dtest=RedisManagementControllerTests test`

Expected: compilation fails before the monitor package exists.

- [ ] **Step 3: Implement bounded Redis access**

Bind `alpha.redis-management.prefixes` in `application.yml` with default `auth:,satoken:`. Implement the production `RedisKeyspace` through `StringRedisTemplate.scan(ScanOptions)` or the underlying Redis connection cursor; preserve Redis's cursor as an opaque string and enforce the configured prefix before scanning, reading metadata or deleting.

The overview may return only selected non-sensitive metrics: Redis version, uptime, used memory bytes, connected clients and count of keys discovered inside each approved prefix. Do not return raw `INFO` properties, unknown keys or command statistics. Derive `category` from the approved prefix; redact all value content. Reject a key that is not an exact member of the approved prefix family even if the caller supplies it directly.

- [ ] **Step 4: Add permission, audit and migration wiring**

V11 creates `Redis 管理` at component `monitor/redis`, permission `monitor:redis:list`, and its delete button. Grant both to `SUPER_ADMIN` with idempotent migration inserts. The delete endpoint uses `@OperationLog(module = "Monitor", operation = "删除 Redis 键", type = BusinessType.DELETE, saveRequest = false, saveResponse = false)` and returns a Chinese confirmation result without echoing the key.

- [ ] **Step 5: Run backend tests**

Run:

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml -Dtest=RedisManagementControllerTests test
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml test
```

Expected: all fake-keyspace security tests pass and Flyway reaches V11 in H2.

- [ ] **Step 6: Commit the backend milestone**

```bash
git add alpha-server/src/main alpha-server/src/test
git commit -m "feat: add safe redis management api"
```

### Task 4: Redis 管理前端、文档与生产验收

**Files:**

- Create: `alpha-web/src/service/redis.ts`
- Create: `alpha-web/src/views/monitor/redis.vue`
- Create: `alpha-web/src/views/monitor/redis.test.ts`
- Modify: `alpha-web/src/router/index.ts`
- Modify: `alpha-web/src/layouts/BaseLayout.vue`
- Modify: `alpha-web/src/router/routes.test.ts`
- Modify: `docs/api.md`
- Modify: `docs/security.md`
- Modify: `docs/operations.md`

**Interfaces:**

- Consumes Task 3 APIs and `monitor:redis:*` permissions.
- Produces a Chinese `Redis 管理` route at `/monitor/redis`, with overview, allowed-prefix selector, cursor-based table, metadata drawer and destructive-action confirmation.

- [ ] **Step 1: Write failing front-end tests**

```ts
expect(redisApi.keys).toHaveBeenCalledWith({ prefix: 'auth:', cursor: '0', count: 50 })
expect(wrapper.text()).toContain('验证码内容已脱敏')
expect(wrapper.find('[data-testid="delete-redis-key"]').exists()).toBe(false)
```

Mock a redacted `auth:captcha:` record and verify list-only users cannot render the delete control; verify the confirmation modal explains that deleting a session key logs the user out.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `pnpm --dir alpha-web test --run src/views/monitor/redis.test.ts`

Expected: module or route cannot be resolved before implementation.

- [ ] **Step 3: Implement the controlled UI**

Add only `monitor/redis` to the route component whitelist and use the fixed Chinese navigation title `Redis 管理`. Render no raw values. Use server-provided opaque cursors for next/previous navigation, reset cursor when the prefix changes, and require an explicit typed confirmation of `删除` before invoking the delete API.

- [ ] **Step 4: Update operational documentation**

Document the four APIs, permissions, managed prefixes, redaction behavior, session invalidation effect and explicit prohibition of global scan/flush operations. State that a shared Redis instance must use a deployment-specific prefix configuration before enabling this page.

- [ ] **Step 5: Run full verification and browser acceptance**

Run:

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml test
pnpm --dir alpha-web test --run
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
git diff --check
```

Then run the dev stack, login as administrator, inspect overview and an `auth:` key's metadata, verify a CAPTCHA/session value cannot be read, test a single-key delete with confirmation, confirm the audit record hides the key, and verify a non-administrator receives 403.

- [ ] **Step 6: Commit and release the milestone**

```bash
git add alpha-web docs
git commit -m "feat: add redis management console"
git push origin main
```

## Data dictionary decision gate

Do not create `sys_dict_type` or `sys_dict_data` in this plan. Open a separate dictionary design only after all three conditions hold:

1. At least two independent pages consume the same selectable business value set.
2. An administrator, not a developer, must be able to add, disable, reorder or relabel that set.
3. The stored business value must remain stable while its display label can change.

When these conditions are met, create a separate plan with `sys_dict_type` and `sys_dict_data`, unique `dict_type` plus `(dict_type, dict_value)`, enabled-only public option API, cache invalidation on writes, and no CSS-class fields copied from RuoYi unless a concrete UI requirement needs them.

## Plan self-review

- Scope coverage: Tasks 1--2 deliver configuration management; Tasks 3--4 deliver bounded Redis operations; the dictionary gate explicitly excludes premature implementation.
- Safety coverage: forbidden configuration prefixes, no secret persistence, bounded Redis prefixes, `SCAN`, redaction, permission split, audit without payloads and no global destructive actions are specified in tasks and tests.
- Boundary coverage: server DTO/VO/service/mapper split, route allowlist, fixed Chinese labels and Flyway-only schema evolution are explicit.
- Placeholder scan: no deferred implementation markers remain; each delivery task has concrete files, APIs, test commands and commit boundary.
