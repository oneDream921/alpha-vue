# S0-03 SpringDoc/Therapi 兼容性 Spike

## 1. 执行摘要

| 项目 | 结果 |
| --- | --- |
| 执行日期 | 2026-07-29 |
| 目标环境 | Java 21、Spring Boot 4.0.0 |
| SpringDoc | 3.0.3 |
| Therapi | 0.15.0 |
| 决策 | `GO WITH CONSTRAINTS`，按计划归入 `GO`，已获用户确认 |

本次只在 `/tmp/alpha-vue-springdoc-spike` 中验证，没有修改生产代码、依赖和接口。

正式目标确定为：

> Javadoc 优先，Swagger 注解只表达 Javadoc 和 Validation 无法表达的契约。

不采用“绝对零 Swagger 注解”。普通 CRUD 的 `@Operation` 可以删除，但中文业务 Tag、
隐藏接口、复杂 Schema、特殊参数、非 200 响应和示例仍允许使用最小注解。

## 2. 当前 Alpha 基线

Alpha 当前已经使用：

- SpringDoc 3.0.3；
- Knife4j 4.4.0；
- 13 个 Controller；
- 12 个 `@ApiSupport`；
- 12 个 `@Tag`；
- 67 个 HTTP 映射和 67 个 `@Operation`；
- 34 个 DTO/VO 文件；
- 0 个 `@Schema`。

当前 `application-prod.yml` 已关闭 SpringDoc 和 Knife4j，但 Maven 尚未配置 Therapi
运行库或注解处理器。

因此 P1-02 的主要工作不是升级 SpringDoc，而是：

1. 删除 Knife4j；
2. 接入 Therapi 编译和运行链；
3. 将重复的 `@Operation` 说明迁移到已有 Javadoc；
4. 固化最小 Swagger 注解白名单；
5. 修正公开接口与全局 Bearer 的 OpenAPI 契约。

## 3. 官方兼容依据

[SpringDoc 3.0.3 官方文档](https://springdoc.org/v4/index.html) 明确：

- SpringDoc 3.x 对应 Spring Boot 4.0.x；
- Javadoc 方法说明可生成 operation description；
- `@return` 可生成响应说明；
- 字段 Javadoc 可生成 Schema description；
- Therapi 运行库和注解处理器缺一时，Javadoc 支持会静默失效；
- Swagger 注解说明优先于 Javadoc，可用于特殊契约覆盖。

Therapi 的工作方式和 IDE 注解处理要求见
[therapi-runtime-javadoc](https://github.com/dnault/therapi-runtime-javadoc)。

## 4. 已验证能力

### 4.1 Javadoc 提取

未使用 `@Operation`、`@Schema` 或 `@Tag` 的 Spike 已验证：

| 来源 | OpenAPI 结果 |
| --- | --- |
| Controller 方法 Javadoc | operation summary 和 description |
| 方法 `@param` | Path、Query 参数和 requestBody description |
| 方法 `@return` | 200 response description |
| record 类 `@param` | Schema 属性 description |
| 嵌套 record `@param` | 嵌套 DTO Schema 属性 description |
| 普通 Java 字段 Javadoc | Schema 属性 description |
| Bean Validation | required、maxLength、Email 等约束 |
| 泛型统一响应 | `ApiResponse<T>` 响应 Schema |

仅修改 Javadoc 文本后执行非 clean Maven 构建，运行时 OpenAPI 已读取到新文本。

### 4.2 OpenAPI 基础能力

真实 HTTP 已验证：

- `/v3/api-docs`；
- `/v3/api-docs/examples`；
- `/swagger-ui/index.html`；
- `GroupedOpenApi` 分组；
- Bearer SecurityScheme；
- 受保护操作继承根级 Bearer；
- 公开路径由集中式 Customizer 生成 `security: []`；
- 不需要给每个公开方法添加安全注解。

### 4.3 构建链

显式 `annotationProcessorPaths` 同时包含：

- Therapi Scribe；
- Lombok；
- Spring Boot Configuration Processor。

已验证：

- Therapi `__Javadoc.json` 正常生成；
- Lombok Getter 正常生成；
- Spring 配置元数据正常生成；
- Maven clean test 通过；
- 触发源码重编译后 test 通过；
- 仅修改 Javadoc 后 test 和 package 通过；
- Spring Boot 可执行 JAR 正常生成并启动。

Alpha 正式接入时不应仅为 Therapi 顺带增加 Configuration Processor。若决定保留现有
配置元数据支持，则显式加入；否则 Processor 白名单只包含 Therapi 和 Lombok。

### 4.4 生产关闭

`prod` Profile 下真实 HTTP 已验证以下路径均返回 404：

- `/v3/api-docs`；
- `/v3/api-docs/examples`；
- `/v3/api-docs/swagger-config`；
- `/swagger-ui.html`；
- `/swagger-ui/index.html`。

生产配置必须同时关闭：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
  enable-javadoc: false
```

关闭端点不会从 JAR 中物理删除 Swagger UI 依赖和 Therapi 元数据。生产网关仍应拒绝
文档路径，防止配置误开启后直接暴露。

## 5. 可执行 JAR 边界

Spike 的 Spring Boot 可执行 JAR 已实际启动，分组文档和 Swagger UI 返回 200。

JAR 内包含：

- `therapi-runtime-javadoc-0.15.0.jar`；
- `springdoc-openapi-starter-webmvc-ui-3.0.3.jar`；
- 9 个 `__Javadoc.json`；
- Spring 配置元数据。

9 个 Javadoc 元数据文件的实际逻辑总量为 2059 字节，体积不是问题。

因为 Javadoc 会进入生产制品，代码注释禁止包含：

- 密码、Token、Cookie；
- 密钥和连接凭据；
- 内部临时访问地址；
- 可利用的安全绕过步骤；
- 不应对制品接收方公开的运维秘密。

## 6. 最小注解边界

### 6.1 默认删除

普通 CRUD 方法只要 Javadoc 完整，默认删除：

- `@Operation(summary = "...")`；
- 仅用于重复字段说明的 `@Schema(description = "...")`；
- Knife4j `@ApiSupport`。

### 6.2 允许保留

| 注解 | 使用场景 |
| --- | --- |
| `@Tag` | 稳定中文业务标签；否则自动 Tag 名来自 Controller 类名 |
| `@Hidden` 或 `@Operation(hidden = true)` | 测试、内部、特殊下载接口 |
| `@ApiResponse`、`@Content`、`@ExampleObject` | 非 200 状态、统一错误响应和示例 |
| `@Schema` | 格式、示例、枚举、隐藏字段、多态、读写属性 |
| `@Parameter` | 隐藏基础设施参数、Header/Cookie、特殊示例 |
| `@Operation` | operationId、特殊安全、弃用或无法由 Javadoc 表达的契约 |

Alpha 当前仍有两个隐藏接口，P1-02 迁移时不得因批量删除 `@Operation` 而重新暴露。

### 6.3 Javadoc 规范

- Controller 类写业务职责；
- Controller 方法第一段写简短摘要；
- 所有业务参数写 `@param`；
- 有响应数据的方法写 `@return`；
- record 组件说明写在类 Javadoc 的 `@param`；
- 普通 DTO 字段写字段 Javadoc；
- 复杂 OpenAPI 语义才使用白名单注解；
- 不复制字段名、Java 类型或 Validation 已能表达的内容。

SpringDoc 的 first-sentence 解析主要识别英文句点和 `<p>`。中文方法说明应保持第一段
简短；需要详细说明时，首段写摘要，后续使用新段落。

## 7. Maven 与 IDE 约束

`annotationProcessorPaths` 是排他性处理器白名单。正式接入必须：

1. 集中维护 Therapi、Lombok 和其他确有需要的 Processor；
2. Lombok 位于依赖它的 Processor 之前；
3. 将来引入 MapStruct 等 Processor 时同步加入白名单和 CI 断言；
4. CI 与发布必须使用 Maven clean 构建；
5. IntelliJ 启用 Annotation Processing，或委托 Maven 构建；
6. IDE 未刷新 Javadoc 时以 Maven 结果为准；
7. 类型删除、重命名后使用 clean 构建，避免旧 `__Javadoc.json` 残留。

建议添加 CI 结构断言：

- 至少一个 Controller 的 Javadoc 元数据存在；
- Lombok 生成方法可调用；
- 若启用 Configuration Processor，则配置元数据存在；
- OpenAPI 中不存在空 summary 或意外暴露的内部接口。

## 8. P1-02 必须完成

S0-03 证明技术路线可行，但不代替正式迁移。P1-02 必须：

1. 删除 Knife4j 依赖、配置、入口和 `@ApiSupport`；
2. 接入 Therapi 0.15.0 运行库和 Scribe Processor；
3. 保留 SpringDoc 3.0.3；
4. 将 67 个普通 `@Operation` 按最小注解边界逐项迁移；
5. 保留中文 `@Tag` 和隐藏接口；
6. 通过集中式公开路径白名单生成 `security: []`；
7. 验证统一错误响应、文件上传、下载和分页 Schema；
8. 验证生产全部文档路径为 404；
9. 检查 Alpha 可执行 JAR 中的依赖和 Javadoc 元数据；
10. 运行完整后端测试、打包和真实文档端点冒烟。

若迁移后出现空说明、公开接口仍要求 Bearer、内部接口被暴露、Processor 生成物缺失，
P1-02 必须停止，不得以手工复制大量重复注解绕过。

## 9. 验证结果

最终自动测试：

| 测试类 | 数量 | 结果 |
| --- | ---: | --- |
| Javadoc、OpenAPI、处理器共存 | 3 | 通过 |
| 生产关闭边界 | 1 | 通过 |

最终命令：

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn \
  -f /tmp/alpha-vue-springdoc-spike/pom.xml test package
```

结果：`BUILD SUCCESS`，4 项测试，0 失败、0 错误；可执行 JAR 冒烟通过。

## 10. 最终结论

S0-03 结论为 `GO WITH CONSTRAINTS`，在当前计划三态中归入 `GO`：

- SpringDoc 3.0.3 与 Therapi 0.15.0 可以进入 P1-02；
- 普通接口采用 Javadoc，不再重复维护 `@Operation`；
- 复杂契约继续使用最小 Swagger 注解；
- 生产运行时关闭文档端点，但接受制品携带 Therapi 元数据；
- 不代表 G0 已通过；
- 已于 2026-07-29 获得用户确认，S0-03 关闭；
- S0-04 仅在用户后续明确确认后开始。
