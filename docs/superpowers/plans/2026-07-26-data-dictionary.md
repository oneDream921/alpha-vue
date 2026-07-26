# Data Dictionary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a RBAC-protected system data-dictionary management capability (dictionary types and items), plus an authenticated business-read API for enabled items.

**Architecture:** Add `sys_dict_type` and `sys_dict_item` through a new Flyway migration, then implement the existing Entity/DTO/VO/Service/Mapper-interface/Mapper-XML layering in the system module. The Vue management view at `/system/dicts` will use the established typed service, controlled-route whitelist, fixed Chinese navigation, permission directive, table pagination, form validation, and delete-confirmation patterns; it will not retrofit any existing business page to consume dictionaries.

**Tech Stack:** Spring Boot 4, Sa-Token, MyBatis-Plus pagination with custom Mapper XML, Flyway, H2 test profile, JUnit 5/MockMvc, Vue 3, TypeScript, Vite, Ant Design Vue, Vitest, ESLint, Prettier.

## Global Constraints

- Start from `codex/config-management` at commit `8df2707`; preserve any pre-existing worktree changes and never reset or overwrite unrelated files.
- Never read, source, print, or modify `deploy/.env`.
- Use custom Mapper SQL in XML for every new dictionary query and write; do not introduce annotation SQL or default `BaseMapper`/wrapper CRUD for dictionary persistence.
- Add only a new Flyway migration (`V11__add_system_dictionary.sql`); do not edit historical migrations.
- Use `0` for disabled and `1` for enabled statuses; normalize optional remarks to `null`, default omitted status to `1`, and default omitted item sort order to `0`.
- Type code is required, trimmed, matches `[A-Za-z][A-Za-z0-9._-]*`, has a maximum of 64 characters, is unique among active types, and is immutable after creation.
- Type name and item label have a maximum of 64 characters; item value has a maximum of 128 characters; remarks have a maximum of 500 characters; item sort order is a non-negative integer.
- Dictionary values are strings (for example `enabled`, `draft`, and `1`); do not impose a numeric-only rule and do not enforce a single default item.
- All management endpoints require the corresponding `system:dict:*` permission. `GET /api/system/dicts/{typeCode}/items` requires only the existing authenticated-session interceptor, not `system:dict:list`.
- Public API errors must be Chinese. Use the exact messages `字典类型编码已存在`, `字典类型编码不可修改`, `字典项值已存在`, and `请先删除该字典类型下的字典项` for their respective business-rule failures.
- Decorate each create, update, and delete endpoint for both types and items with `@OperationLog`; leave request/response logging disabled so audit entries remain redacted and do not store whole form bodies.
- Do not add Redis management, any runtime cache, hot refresh, anonymous reads, import/export, hierarchical/multilingual dictionaries, physical item deletion, forced single defaults, or dictionary conversion of existing pages.

---

## Target File Structure

| File | Responsibility |
| --- | --- |
| `alpha-server/src/main/resources/db/migration/V11__add_system_dictionary.sql` | Creates dictionary tables, active-record uniqueness/indexes, menu/button permissions, and the `SUPER_ADMIN` grants. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/common/exception/PublicErrorMessage.java` | Exposes the four fixed Chinese dictionary business messages without leaking database errors. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictType.java` | Maps `sys_dict_type` and inherited audit/logical-delete columns. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictItem.java` | Maps `sys_dict_item` and inherited audit/logical-delete columns. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/DictRequests.java` | Declares validated create/update request records. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictTypeVo.java` | Maps safe type management responses. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictItemVo.java` | Maps safe item management responses. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/EnabledDictItemVo.java` | Restricts business-read responses to display-safe item fields. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictTypeMapper.java` | Declares the type XML statements. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictItemMapper.java` | Declares the item XML statements. |
| `alpha-server/src/main/resources/mapper/system/SysDictTypeMapper.xml` | Owns active-type paging, lookups, writes, dependency count, and soft deletion. |
| `alpha-server/src/main/resources/mapper/system/SysDictItemMapper.xml` | Owns active-item paging, uniqueness lookups, enabled business reads, writes, and soft deletion. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/DictService.java` | Owns transactions, normalization, invariants, duplicate translation, and all type/item/read use cases. |
| `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DictController.java` | Exposes the approved API paths, RBAC gates, OpenAPI metadata, traces, and audit annotations. |
| `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java` | Covers dictionary API, authorization, audit, deletion, and enabled-item behavior through MockMvc. |
| `alpha-web/src/service/system.ts` | Adds dictionary TypeScript contracts and typed management/read requests. |
| `alpha-web/src/router/index.ts` | Adds the controlled `/system/dicts` route definition. |
| `alpha-web/src/router/routes.test.ts` | Verifies the dictionary component is registered only for its known menu/permission pair. |
| `alpha-web/src/layouts/BaseLayout.vue` | Adds the fixed Chinese `数据字典` navigation entry. |
| `alpha-web/src/layouts/BaseLayout.test.ts` | Verifies navigation visibility follows `system:dict:list`. |
| `alpha-web/src/views/system/dicts.vue` | Provides the responsive type/item management page and permission-gated actions. |
| `alpha-web/src/views/system/dicts.pagination.ts` | Keeps item pagination state transitions testable without mounting the page. |
| `alpha-web/src/views/system/dicts.pagination.test.ts` | Tests type switching and table pagination state helpers. |
| `alpha-web/src/views/system/dicts.validation.ts` | Keeps type-code and item-form normalization/validation helpers testable. |
| `alpha-web/src/views/system/dicts.validation.test.ts` | Tests required, format, and length validation messages used by the forms. |

## API and Data Contracts

```java
// DictRequests.java
public record TypeSave(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]*") String typeCode,
        @NotBlank @Size(max = 64) String typeName,
        @Min(0) @Max(1) Integer status,
        @Size(max = 500) String remark) { }

public record ItemSave(
        @NotBlank @Size(max = 64) String label,
        @NotBlank @Size(max = 128) String value,
        @PositiveOrZero Integer sortOrder,
        @Min(0) @Max(1) Integer status,
        @Min(0) @Max(1) Integer isDefault,
        @Size(max = 500) String remark) { }
```

```java
// DictService.java
PageResponse<DictTypeVo> pageTypes(int page, int size);
DictTypeVo getType(long id);
DictTypeVo createType(DictRequests.TypeSave request);
DictTypeVo updateType(long id, DictRequests.TypeSave request);
void deleteType(long id);
PageResponse<DictItemVo> pageItems(long typeId, int page, int size);
DictItemVo createItem(long typeId, DictRequests.ItemSave request);
DictItemVo updateItem(long id, DictRequests.ItemSave request);
void deleteItem(long id);
List<EnabledDictItemVo> enabledItems(String typeCode);
```

```text
GET    /api/system/dict-types?page=1&size=10
GET    /api/system/dict-types/{id}
POST   /api/system/dict-types
PUT    /api/system/dict-types/{id}
DELETE /api/system/dict-types/{id}

GET    /api/system/dict-types/{typeId}/items?page=1&size=10
POST   /api/system/dict-types/{typeId}/items
PUT    /api/system/dict-items/{id}
DELETE /api/system/dict-items/{id}
GET    /api/system/dicts/{typeCode}/items
```

### Task 1: Add the dictionary schema, permissions, and persistence contracts

**Files:**
- Create: `alpha-server/src/main/resources/db/migration/V11__add_system_dictionary.sql`
- Modify: `alpha-server/src/main/java/io/github/onedream921/alphavue/common/exception/PublicErrorMessage.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictType.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictItem.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictTypeMapper.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictItemMapper.java`
- Create: `alpha-server/src/main/resources/mapper/system/SysDictTypeMapper.xml`
- Create: `alpha-server/src/main/resources/mapper/system/SysDictItemMapper.xml`
- Test: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java`

**Consumes:** Existing `SystemEntity`, `Page<T>`, Flyway H2 MySQL mode, `sys_menu`, `sys_role_menu`, and the custom-XML pattern in `SysConfigMapper`.

**Produces:** Two logical-delete-safe tables and the mapper methods required by `DictService` in Tasks 2–3. Active rows use `deleted = 0`; every custom soft-delete statement sets `deleted = id`, so the composite unique keys permit recreation after logical deletion while still enforcing one active code/value.

- [ ] **Step 1: Write the migration-backed failing tests for the new permission/menu records and active-record uniqueness behavior.**

```java
assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_menu WHERE permission = 'system:dict:list'", Integer.class)).isEqualTo(1);
assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_role_menu rm JOIN sys_role r ON r.id = rm.role_id "
                + "JOIN sys_menu m ON m.id = rm.menu_id "
                + "WHERE r.code = 'SUPER_ADMIN' AND m.permission = 'system:dict:delete'",
        Integer.class)).isEqualTo(1);
```

- [ ] **Step 2: Run the new controller test class before adding the migration.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: FAIL during application startup because the dictionary tables and permissions do not exist.

- [ ] **Step 3: Add `V11__add_system_dictionary.sql` with active-row constraints, indexes, and RBAC seed data.**

```sql
CREATE TABLE sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_type_code_deleted UNIQUE (type_code, deleted)
);

CREATE TABLE sys_dict_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_id BIGINT NOT NULL,
    label VARCHAR(64) NOT NULL,
    value VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    is_default TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    deleted BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_item_type_value_deleted UNIQUE (type_id, value, deleted)
);

CREATE INDEX idx_sys_dict_type_deleted_name ON sys_dict_type (deleted, type_name, id);
CREATE INDEX idx_sys_dict_item_type_deleted_sort ON sys_dict_item (type_id, deleted, sort_order, id);
```

Append these idempotent menu/button inserts and grants (ids `31`–`34`):

```sql
INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, permission, icon, visible, sort_order)
SELECT 31, 2, '数据字典', 'MENU', 'dicts', 'system/dicts', 'system:dict:list', 'BookOutlined', 1, 6
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:list');

INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 32, 31, '新增数据字典', 'BUTTON', 'system:dict:create', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:create');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 33, 31, '修改数据字典', 'BUTTON', 'system:dict:update', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:update');
INSERT INTO sys_menu (id, parent_id, title, menu_type, permission, visible, sort_order)
SELECT 34, 31, '删除数据字典', 'BUTTON', 'system:dict:delete', 0, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:dict:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission IN (
    'system:dict:list', 'system:dict:create', 'system:dict:update', 'system:dict:delete'
)
LEFT JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.menu_id = m.id
WHERE r.code = 'SUPER_ADMIN' AND rm.menu_id IS NULL;
```

- [ ] **Step 4: Add the dictionary entities and XML-only mapper interfaces/statements.**

```java
@TableName("sys_dict_type")
public class SysDictType extends SystemEntity {
    private String typeCode;
    private String typeName;
    private Integer status;
    private String remark;
}

@TableName("sys_dict_item")
public class SysDictItem extends SystemEntity {
    private Long typeId;
    private String label;
    private String value;
    private Integer sortOrder;
    private Integer status;
    private Integer isDefault;
    private String remark;
}
```

Declare, then implement in the XML files, `selectPageActive`, `selectActiveById`, `selectActiveByTypeCode`, `insert`, `update`, and `softDeleteById` for types; declare/implement `selectPageActiveByTypeId`, `selectActiveById`, `selectActiveByTypeIdAndValue`, `selectEnabledByTypeCode`, `insert`, `update`, `softDeleteById`, and `countActiveByTypeId` for items. Every select must state `deleted = 0`; enabled reads must join an active, enabled type and require `item.status = 1`, ordering by `item.sort_order ASC, item.id ASC`; all soft deletes must use `SET deleted = id, updated_at = CURRENT_TIMESTAMP`.

- [ ] **Step 5: Add fixed public-error enum members used by later business-rule checks.**

```java
DICT_TYPE_CODE_EXISTS("字典类型编码已存在"),
DICT_TYPE_CODE_IMMUTABLE("字典类型编码不可修改"),
DICT_ITEM_VALUE_EXISTS("字典项值已存在"),
DICT_TYPE_HAS_ITEMS("请先删除该字典类型下的字典项"),
```

- [ ] **Step 6: Re-run the focused backend test to verify Flyway applies the schema and permission seed.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: startup reaches test execution; CRUD assertions remain red until Tasks 2–3 add the controller.

- [ ] **Step 7: Commit the schema and persistence foundation.**

```bash
git add alpha-server/src/main/resources/db/migration/V11__add_system_dictionary.sql \
  alpha-server/src/main/java/io/github/onedream921/alphavue/common/exception/PublicErrorMessage.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictType.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/entity/SysDictItem.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictTypeMapper.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/mapper/SysDictItemMapper.java \
  alpha-server/src/main/resources/mapper/system/SysDictTypeMapper.xml \
  alpha-server/src/main/resources/mapper/system/SysDictItemMapper.xml
git commit -m "feat: add dictionary persistence"
```

### Task 2: Implement dictionary-type management and audit-safe controller paths

**Files:**
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/DictRequests.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictTypeVo.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/DictService.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DictController.java`
- Modify: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java`

**Consumes:** Type mapper methods from Task 1, `SystemAccessService`, `ApiResponse`, `PageResponse`, `TraceIdFilter`, `@OperationLog`, and `PublicErrorMessage` dictionary members.

**Produces:** RBAC-protected type list/detail/create/update/delete endpoints. Item paths and the business-read route are deliberately left for Task 3.

- [ ] **Step 1: Add failing MockMvc tests for type CRUD, list pagination/detail, code invariants, and independently denied permissions.**

```java
mockMvc.perform(post("/api/system/dict-types").header("Authorization", bearer(adminToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"typeCode\":\"dict-test.status\",\"typeName\":\"状态\",\"status\":1}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.typeCode").value("dict-test.status"));

mockMvc.perform(put("/api/system/dict-types/{id}", typeId).header("Authorization", bearer(adminToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"typeCode\":\"dict-test.changed\",\"typeName\":\"状态\",\"status\":1}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.message").value("字典类型编码不可修改"));
```

Create a `DICT_LIST_ONLY` fixture role/user/menu in the test setup. Assert its token receives `200` for type list/detail and `403` for type create, update, and delete. Exercise a duplicate active code and assert `字典类型编码已存在`; after type deletion, recreate the same code to prove deleted rows no longer participate in active uniqueness.

- [ ] **Step 2: Run the focused test to verify it fails before the type service/controller exist.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: FAIL because `POST /api/system/dict-types` is unmapped.

- [ ] **Step 3: Define validated requests and a management response that contains no persistence-only delete state.**

```java
public final class DictRequests {
    public record TypeSave(@NotBlank @Size(max = 64)
                           @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]*") String typeCode,
                           @NotBlank @Size(max = 64) String typeName,
                           @Min(0) @Max(1) Integer status,
                           @Size(max = 500) String remark) { }
}

public record DictTypeVo(Long id, String typeCode, String typeName, Integer status, String remark,
                         LocalDateTime createdAt, LocalDateTime updatedAt) { }
```

Implement `DictTypeVo.from(SysDictType type)` exactly once and trim `typeCode`/`typeName`, nulling blank remarks in the service copy method.

- [ ] **Step 4: Implement the transactional type use cases with explicit mapper calls and duplicate translation.**

```java
public DictTypeVo updateType(long id, DictRequests.TypeSave request) {
    SysDictType type = requireType(id);
    String typeCode = request.typeCode().trim();
    if (!type.getTypeCode().equals(typeCode)) {
        throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_CODE_IMMUTABLE);
    }
    copyType(request, type, typeCode);
    updateTypeOrThrow(type);
    return DictTypeVo.from(type);
}

public void deleteType(long id) {
    requireType(id);
    if (itemMapper.countActiveByTypeId(id) > 0) {
        throw new BusinessException(400, PublicErrorMessage.DICT_TYPE_HAS_ITEMS);
    }
    if (typeMapper.softDeleteById(id) != 1) throw invalidRequest();
}
```

`createType` must pre-check `selectActiveByTypeCode`, translate a concurrent `DuplicateKeyException` to `DICT_TYPE_CODE_EXISTS`, and leave the migration’s unique key as the final integrity guard. `pageTypes` must use the existing `PageResponse` shape and map the XML page in `id DESC` order. `deleteType` must be transactional and must not delete items automatically.

- [ ] **Step 5: Add type endpoint methods to `DictController` using the exact management paths and permissions.**

```java
@RestController
@RequestMapping("/api/system")
public class DictController {
    @GetMapping("/dict-types")
    public ApiResponse<PageResponse<DictTypeVo>> pageTypes(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("system:dict:list");
        return ApiResponse.success(dictService.pageTypes(page, size), traceId(request));
    }

    @PostMapping("/dict-types")
    @OperationLog(module = "System", operation = "Create dictionary type", type = BusinessType.CREATE)
    public ApiResponse<DictTypeVo> createType(@Valid @RequestBody DictRequests.TypeSave body,
                                               HttpServletRequest request) {
        access.require("system:dict:create");
        return ApiResponse.success(dictService.createType(body), traceId(request));
    }

    @GetMapping("/dict-types/{id}")
    public ApiResponse<DictTypeVo> getType(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dict:list");
        return ApiResponse.success(dictService.getType(id), traceId(request));
    }

    @PutMapping("/dict-types/{id}")
    @OperationLog(module = "System", operation = "Update dictionary type", type = BusinessType.UPDATE)
    public ApiResponse<DictTypeVo> updateType(@PathVariable @Positive long id,
                                               @Valid @RequestBody DictRequests.TypeSave body,
                                               HttpServletRequest request) {
        access.require("system:dict:update");
        return ApiResponse.success(dictService.updateType(id, body), traceId(request));
    }

    @DeleteMapping("/dict-types/{id}")
    @OperationLog(module = "System", operation = "Delete dictionary type", type = BusinessType.DELETE)
    public ApiResponse<Void> deleteType(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("system:dict:delete");
        dictService.deleteType(id);
        return ApiResponse.success(null, traceId(request));
    }
}
```

Do not set `saveRequest` or `saveResponse` on any operation annotation; the existing audit service therefore records `[redacted]` rather than submitted fields.

- [ ] **Step 6: Run the focused type tests.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: type CRUD, pagination/detail, type-code rules, and four-way permission assertions PASS; item/read assertions remain red until Task 3.

- [ ] **Step 7: Commit the type-management slice.**

```bash
git add alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/DictRequests.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictTypeVo.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/DictService.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DictController.java \
  alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java
git commit -m "feat: manage dictionary types"
```

### Task 3: Implement dictionary items and the authenticated enabled-item read API

**Files:**
- Modify: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/DictRequests.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictItemVo.java`
- Create: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/EnabledDictItemVo.java`
- Modify: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/DictService.java`
- Modify: `alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DictController.java`
- Modify: `alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java`

**Consumes:** The type mapper/service/controller from Task 2 and every item mapper method from Task 1.

**Produces:** Item CRUD below an existing type, type-dependency protection, and the safe non-admin read model for already authenticated users.

- [ ] **Step 1: Add failing MockMvc tests for item rules, type deletion dependency, enabled-item filtering/order, authentication, and audit redaction.**

```java
mockMvc.perform(get("/api/system/dicts/{typeCode}/items", "dict-test.status")
        .header("Authorization", bearer(readOnlyToken)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data[0].value").value("draft"))
    .andExpect(jsonPath("$.data[0].remark").doesNotExist())
    .andExpect(jsonPath("$.data[0].status").doesNotExist());

mockMvc.perform(get("/api/system/dicts/{typeCode}/items", "dict-test.status"))
    .andExpect(status().isUnauthorized());

mockMvc.perform(delete("/api/system/dict-types/{id}", typeId).header("Authorization", bearer(adminToken)))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.message").value("请先删除该字典类型下的字典项"));
```

Create enabled items whose sort orders are `20` and `10`, one disabled item, and one enabled item under a disabled type. Assert the read endpoint returns only the two enabled items for the enabled type in `10`, then `20` order; assert its response includes only `label`, `value`, `sortOrder`, and `isDefault`. Create/update an item with a duplicate active value and assert `字典项值已存在`; verify a deleted item value may be reused. After all six write operations (type and item create/update/delete), poll the async audit log and assert each operation exists with `request_params = '[redacted]'` and does not contain test form labels, values, or remarks.

- [ ] **Step 2: Run the focused test to verify the item and public-read cases fail.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: FAIL because the item paths and `/api/system/dicts/{typeCode}/items` path are not yet mapped.

- [ ] **Step 3: Add item input and response contracts.**

```java
public record ItemSave(@NotBlank @Size(max = 64) String label,
                       @NotBlank @Size(max = 128) String value,
                       @PositiveOrZero Integer sortOrder,
                       @Min(0) @Max(1) Integer status,
                       @Min(0) @Max(1) Integer isDefault,
                       @Size(max = 500) String remark) { }

public record EnabledDictItemVo(String label, String value, Integer sortOrder, Integer isDefault) {
    public static EnabledDictItemVo from(SysDictItem item) {
        return new EnabledDictItemVo(item.getLabel(), item.getValue(), item.getSortOrder(), item.getIsDefault());
    }
}
```

`DictItemVo` must contain its management fields (`id`, `typeId`, `label`, `value`, `sortOrder`, `status`, `isDefault`, `remark`, timestamps). It must not be reused by the business-read endpoint.

- [ ] **Step 4: Implement item transactions and enabled-read filtering in `DictService`.**

```java
public List<EnabledDictItemVo> enabledItems(String typeCode) {
    return itemMapper.selectEnabledByTypeCode(typeCode.trim()).stream()
            .map(EnabledDictItemVo::from)
            .toList();
}

public DictItemVo updateItem(long id, DictRequests.ItemSave request) {
    SysDictItem item = requireItem(id);
    requireType(item.getTypeId());
    String value = request.value().trim();
    assertValueUnique(item.getTypeId(), value, id);
    copyItem(request, item, value);
    updateItemOrThrow(item);
    return DictItemVo.from(item);
}
```

`createItem` must first `requireType(typeId)`, pre-check active uniqueness within that `typeId`, then translate `DuplicateKeyException` to `DICT_ITEM_VALUE_EXISTS`. `updateItem` keeps the item attached to its existing `typeId` (the approved UI has no cross-type move action), validates that type is still active, and applies the same uniqueness check excluding its own id. `pageItems` must require the type exists and return `sort_order ASC, id ASC`. `deleteItem` is transactional and uses XML logical deletion only. Do not add a default-item uniqueness query or constraint.

- [ ] **Step 5: Add the item and business-read methods to `DictController`.**

```java
@GetMapping("/dict-types/{typeId}/items")
public ApiResponse<PageResponse<DictItemVo>> pageItems(@PathVariable @Positive long typeId,
                                                        @RequestParam(defaultValue = "1") @Min(1) int page,
                                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                                        HttpServletRequest request) {
    access.require("system:dict:list");
    return ApiResponse.success(dictService.pageItems(typeId, page, size), traceId(request));
}

@GetMapping("/dicts/{typeCode}/items")
public ApiResponse<List<EnabledDictItemVo>> enabledItems(@PathVariable @Size(max = 64) String typeCode,
                                                          HttpServletRequest request) {
    return ApiResponse.success(dictService.enabledItems(typeCode), traceId(request));
}

@PostMapping("/dict-types/{typeId}/items")
@OperationLog(module = "System", operation = "Create dictionary item", type = BusinessType.CREATE)
public ApiResponse<DictItemVo> createItem(@PathVariable @Positive long typeId,
                                          @Valid @RequestBody DictRequests.ItemSave body,
                                          HttpServletRequest request) {
    access.require("system:dict:create");
    return ApiResponse.success(dictService.createItem(typeId, body), traceId(request));
}

@PutMapping("/dict-items/{id}")
@OperationLog(module = "System", operation = "Update dictionary item", type = BusinessType.UPDATE)
public ApiResponse<DictItemVo> updateItem(@PathVariable @Positive long id,
                                          @Valid @RequestBody DictRequests.ItemSave body,
                                          HttpServletRequest request) {
    access.require("system:dict:update");
    return ApiResponse.success(dictService.updateItem(id, body), traceId(request));
}

@DeleteMapping("/dict-items/{id}")
@OperationLog(module = "System", operation = "Delete dictionary item", type = BusinessType.DELETE)
public ApiResponse<Void> deleteItem(@PathVariable @Positive long id, HttpServletRequest request) {
    access.require("system:dict:delete");
    dictService.deleteItem(id);
    return ApiResponse.success(null, traceId(request));
}
```

The enabled-items method intentionally has no RBAC permission check; it relies on the existing global Sa-Token `StpUtil.checkLogin()` interceptor for its required authentication.

- [ ] **Step 6: Run the complete dictionary integration test class.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=DictControllerTests test`

Expected: PASS for type/item CRUD, pagination/detail, permission denials, duplicate cases, dependency refusal, enabled filtering/order, authenticated non-admin read, and redacted audit logs.

- [ ] **Step 7: Commit the item and read-interface slice.**

```bash
git add alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/dto/DictRequests.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/DictItemVo.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/vo/EnabledDictItemVo.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/service/DictService.java \
  alpha-server/src/main/java/io/github/onedream921/alphavue/modules/system/controller/DictController.java \
  alpha-server/src/test/java/io/github/onedream921/alphavue/modules/system/DictControllerTests.java
git commit -m "feat: manage dictionary items"
```

### Task 4: Add the typed frontend API, controlled route, and fixed navigation

**Files:**
- Modify: `alpha-web/src/service/system.ts`
- Modify: `alpha-web/src/router/index.ts`
- Modify: `alpha-web/src/router/routes.test.ts`
- Modify: `alpha-web/src/layouts/BaseLayout.vue`
- Modify: `alpha-web/src/layouts/BaseLayout.test.ts`

**Consumes:** The exact server response/request names from Tasks 2–3 and the Flyway menu tuple (`system/dicts`, `system:dict:list`).

**Produces:** Compile-safe frontend contracts plus route/navigation visibility before the view is rendered in Task 5.

- [ ] **Step 1: Extend route and layout tests first.**

```ts
expect(
    managementRoutesFor([
        {
            id: 31,
            parentId: 2,
            title: '数据字典',
            menuType: 'MENU',
            path: 'dicts',
            component: 'system/dicts',
            permission: 'system:dict:list',
            sortOrder: 6,
        },
    ]).map((route) => route.name),
).toEqual(['dicts'])
```

In `BaseLayout.test.ts`, mount once without `system:dict:list` and assert `数据字典` is absent; mount again with the permission and assert it is present. Keep the existing parameter-configuration assertions intact.

- [ ] **Step 2: Run the focused frontend tests before registering the route/navigation.**

Run: `cd alpha-web && pnpm test -- routes.test.ts BaseLayout.test.ts`

Expected: FAIL because `dicts` is not a whitelisted route and the fixed Chinese navigation item does not exist.

- [ ] **Step 3: Add typed dictionary contracts and explicit requests to `service/system.ts`.**

```ts
export interface DictType extends BaseEntity {
    typeCode: string
    typeName: string
    remark?: string
    updatedAt?: string
}
export interface DictItem extends BaseEntity {
    typeId: number
    label: string
    value: string
    sortOrder: number
    isDefault: number
    remark?: string
    updatedAt?: string
}
export interface DictTypeSave {
    typeCode: string
    typeName: string
    status: number
    remark?: string
}
export interface DictItemSave {
    label: string
    value: string
    sortOrder: number
    status: number
    isDefault: number
    remark?: string
}
export interface EnabledDictItem {
    label: string
    value: string
    sortOrder: number
    isDefault: number
}
```

Export `dictApi` with `pageTypes`, `getType`, `createType`, `updateType`, `deleteType`, `pageItems`, `createItem`, `updateItem`, `deleteItem`, and `enabledItems`. Use exact paths from the approved API; include only `page`/`size` as page query parameters and use `http.get<ApiResponse<PageResponse<DictType>>>` / `http.get<ApiResponse<PageResponse<DictItem>>>` for management lists and `http.get<ApiResponse<EnabledDictItem[]>>` for the business read.

- [ ] **Step 4: Register the route and navigation with the exact menu contract.**

```ts
{
    path: 'system/dicts',
    name: 'dicts',
    componentId: 'system/dicts',
    component: () => import('@/views/system/dicts.vue'),
    meta: { permission: 'system:dict:list' },
},
```

Import `BookOutlined` in `BaseLayout.vue` and add `{ path: '/system/dicts', title: '数据字典', icon: BookOutlined, permission: 'system:dict:list' }` directly after `参数配置`. Do not use backend menu titles as a display fallback.

- [ ] **Step 5: Re-run the focused navigation/route tests.**

Run: `cd alpha-web && pnpm test -- routes.test.ts BaseLayout.test.ts`

Expected: PASS; the route is accepted only when the menu component and list permission match, and the nav entry follows the permission.

- [ ] **Step 6: Commit the frontend integration surface.**

```bash
git add alpha-web/src/service/system.ts alpha-web/src/router/index.ts \
  alpha-web/src/router/routes.test.ts alpha-web/src/layouts/BaseLayout.vue \
  alpha-web/src/layouts/BaseLayout.test.ts
git commit -m "feat: register dictionary management route"
```

### Task 5: Build the responsive two-pane dictionary management page and isolated frontend tests

**Files:**
- Create: `alpha-web/src/views/system/dicts.vue`
- Create: `alpha-web/src/views/system/dicts.pagination.ts`
- Create: `alpha-web/src/views/system/dicts.pagination.test.ts`
- Create: `alpha-web/src/views/system/dicts.validation.ts`
- Create: `alpha-web/src/views/system/dicts.validation.test.ts`

**Consumes:** `dictApi`, `DictType`, `DictItem`, and all four dictionary permissions from Task 4.

**Produces:** A responsive management page: filtered/paginated type table on the left and selected-type item table on the right, each with the approved CRUD interactions.

- [ ] **Step 1: Write the pure-helper tests that describe required selection, paging, and form validation behavior.**

```ts
expect(itemPageForTypeSelection(undefined, 3, 20)).toEqual({
    selectedTypeId: undefined,
    page: 1,
    pageSize: 20,
    shouldLoad: false,
})
expect(itemPageForTypeSelection(42, 3, 20)).toEqual({
    selectedTypeId: 42,
    page: 1,
    pageSize: 20,
    shouldLoad: true,
})
expect(validateTypeCode('')).toBe('请输入类型编码')
expect(validateTypeCode('1status')).toBe('类型编码需以字母开头，仅含字母、数字、点、下划线或连字符')
expect(validateItemValue('x'.repeat(129))).toBe('字典项值不能超过 128 个字符')
```

Also test table pagination falls back to the current values when Ant Design Vue omits `current` or `pageSize`, and test a valid string value such as `enabled` is accepted.

- [ ] **Step 2: Run the new helper tests before creating their implementations.**

Run: `cd alpha-web && pnpm test -- dicts.pagination.test.ts dicts.validation.test.ts`

Expected: FAIL because the helper modules do not exist.

- [ ] **Step 3: Implement testable pagination and validation helpers.**

```ts
export function itemPageForTypeSelection(
    selectedTypeId: number | undefined,
    _currentPage: number,
    currentPageSize: number,
) {
    return {
        selectedTypeId,
        page: 1,
        pageSize: currentPageSize,
        shouldLoad: selectedTypeId !== undefined,
    }
}
```

Implement `dictPageFromTableChange` with the same fallback behavior as `configPageFromTableChange`; implement `validateTypeCode` and `validateItemValue` with the exact Chinese messages from Step 1. Keep the Vue rules as the final form gate as well: required type code/name, regex/length limits, non-negative sort order, `0|1` status/default select values, and optional 500-character remarks.

- [ ] **Step 4: Implement `dicts.vue` using two independently paginated panels without dropping mobile functionality.**

```ts
async function selectType(type: DictType | undefined) {
    const next = itemPageForTypeSelection(type?.id, itemPage.value, itemPageSize.value)
    selectedType.value = type
    itemPage.value = next.page
    itemPageSize.value = next.pageSize
    itemRows.value = []
    itemTotal.value = 0
    if (next.shouldLoad) await loadItems()
}

async function loadItems() {
    if (!selectedType.value) return
    const response = await dictApi.pageItems(selectedType.value.id, itemPage.value, itemPageSize.value)
    itemRows.value = response.data.data.records
    itemTotal.value = response.data.data.total
}
```

Render the type side with a local type-code/name filter, refresh button, selectable table, type pagination, and permission-gated new/edit/delete buttons. Render the item side with the selected type heading, an empty instruction when no type is selected, no item request before selection, item pagination, and permission-gated new/edit/delete buttons. On a type switch, clear stale item rows and load page 1; after an item mutation reload only the selected type’s rows; after deleting the selected type clear selection and the right panel. Use `a-row`/`a-col` responsive breakpoints or equivalent CSS so the two panels stack on narrow screens instead of hiding a feature.

Use separate modals/forms for types and items. The edit-type modal must display `typeCode` as disabled but still submit its unmodified original value, allowing the server to enforce immutability. The item form must not expose a type selector: it creates under the selected type and updates its existing association. Use Ant Design `a-select` values `1`/`0` for status and default state. Confirm type deletion with text explaining that existing items must be deleted first; do not swallow the rejected request so the existing Axios interceptor displays `请先删除该字典类型下的字典项`.

- [ ] **Step 5: Run the focused frontend unit tests and type-check the new view.**

Run: `cd alpha-web && pnpm test -- dicts.pagination.test.ts dicts.validation.test.ts && pnpm typecheck`

Expected: PASS; no request is made by `loadItems` without a selection, helper behavior is correct, and the view imports/types compile.

- [ ] **Step 6: Commit the management page.**

```bash
git add alpha-web/src/views/system/dicts.vue \
  alpha-web/src/views/system/dicts.pagination.ts \
  alpha-web/src/views/system/dicts.pagination.test.ts \
  alpha-web/src/views/system/dicts.validation.ts \
  alpha-web/src/views/system/dicts.validation.test.ts
git commit -m "feat: add dictionary management page"
```

### Task 6: Run the full verification suite and perform post-build acceptance checks

**Files:**
- Modify only if verification exposes a defect in one of the task files above; do not broaden scope to Redis, caching, hot refresh, or existing-page dictionary adoption.

**Consumes:** Completed backend and frontend work from Tasks 1–5.

**Produces:** Evidence that the complete milestone is safe to hand to manual acceptance.

- [ ] **Step 1: Run the full backend test suite with the mandated Maven executable.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn test`

Expected: PASS, including Flyway migration startup and `DictControllerTests` coverage.

- [ ] **Step 2: Run all frontend automated verification commands.**

Run: `cd alpha-web && pnpm test && pnpm typecheck && pnpm lint && pnpm format:check && pnpm build`

Expected: every command exits `0`; the production build contains the lazily loaded `system/dicts` view.

- [ ] **Step 3: Inspect the complete diff for scope and persistence safety.**

Run: `git diff --check origin/codex/config-management...HEAD && git status --short && git diff --stat origin/codex/config-management...HEAD`

Expected: no whitespace errors; only the planned dictionary/migration/navigation/test files change; no `deploy/.env` access or edits appear.

- [ ] **Step 4: Start the backend and frontend only after all automated commands pass, then manually exercise the accepted flow.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn spring-boot:run -Dspring-boot.run.profiles=dev`

Run in a separate terminal: `cd alpha-web && pnpm dev --host 127.0.0.1`

Manual acceptance checklist:

- Log in as the seeded super administrator, open `/system/dicts`, and confirm `数据字典` appears in the fixed Chinese navigation.
- Create an enabled type and two enabled items with different sort orders; reload and confirm the left/right selection, item ordering, pagination, edit, and delete controls work.
- Verify the type delete is rejected with the exact Chinese message until its active items are deleted, then succeeds.
- Use a signed-in account without dictionary management permission to call the enabled-item API and confirm it reads enabled values; confirm the same request without a token is rejected.
- Narrow the browser viewport and confirm both panels remain reachable in a stacked responsive layout.

- [ ] **Step 5: Re-run automated suites after any verification fix.**

Run: `cd alpha-server && /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn test && cd ../alpha-web && pnpm test && pnpm typecheck && pnpm lint && pnpm format:check && pnpm build`

Expected: all checks PASS again after the final code state.

## Self-Review

- **Spec coverage:** Tasks 1–3 cover the two-table model, logical deletion, active uniqueness, CRUD, list/detail pagination, enabled-only authenticated read, RBAC, Flyway, deletion dependency, and redacted auditing. Tasks 4–5 cover the typed service, controlled whitelist, Chinese navigation, permissions, validation, selection-driven two-pane UI, pagination, and error feedback. Task 6 covers the required full test/typecheck/lint/format/build sequence and post-build responsive/manual acceptance.
- **Explicit exclusions:** No task creates Redis configuration, cache state, hot refresh, anonymous API access, existing business-page conversion, physical deletion, single-default enforcement, import/export, hierarchy, or multilingual behavior.
- **Consistency check:** The route, migration component id, navigation path, permission names, API paths, DTO fields, frontend contracts, and Mapper/service method names use the same `dict`/`dict-types`/`dict-items` vocabulary throughout. Type code is included in update payloads solely for server-side immutable-code detection; items retain their current type on update.
- **Placeholder scan:** This document contains no deferred implementation marker, unspecified error/validation behavior, or unscoped file target.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-26-data-dictionary.md`.

When you approve implementation, choose one execution mode:

1. **Subagent-Driven (recommended):** dispatch a fresh implementer per task and review each task before proceeding.
2. **Inline Execution:** execute the tasks in this session with checkpoints between the backend, frontend, and final verification slices.
