# Alpha Vue Java 开发规范

## 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档版本 | 1.0.0 |
| 生效日期 | 2026-08-08 |
| 适用范围 | `alpha-server` Java、Flyway 迁移、后端测试及与后端契约相关的 `alpha-web` 代码 |
| 维护人 | Alpha Vue 项目维护组 |
| 依据 | 阿里巴巴 Java 开发手册相关条目、Alpha Vue 现有工程约定 |

### 修改记录

| 版本 | 日期 | 修改内容 |
| --- | --- | --- |
| 1.0.0 | 2026-08-08 | 首次建立，结合当前项目分层、MyBatis-Plus、Flyway、审计日志和测试入口整理规约 |

## 1. 项目基线与适用原则

Alpha Vue 是 Spring Boot 单体后端（`alpha-server`）与 Vue 3 管理端（`alpha-web`）组成的单仓库项目。后端按以下边界组织：

```text
common                         公共响应、异常、工具和跨领域契约
framework                      Web、安全、MyBatis、Redis、异步和文档基础设施
modules/<domain>/controller    HTTP 接入、参数校验、权限和统一响应
modules/<domain>/service       事务、业务编排和领域规则
modules/<domain>/manager       第三方平台适配、通用能力下沉、多 DAO 组合（按需创建）
modules/<domain>/mapper        MyBatis-Plus Mapper 与 XML SQL
modules/<domain>/entity        数据库持久化对象（DO）
modules/<domain>/dto           Service 边界的请求、更新和查询对象
modules/<domain>/bo            多对象组合后的业务对象（确有需要时创建）
modules/<domain>/vo            Web 响应对象
```

本项目现有代码以 `Controller -> Service -> Mapper` 为主，`Manager` 不是每个领域的必设层。没有第三方适配、跨 DAO 复用或通用能力下沉时，不为凑层级创建空 Manager。新增代码必须遵守职责边界；历史代码按触碰范围逐步治理，不为本规范进行无关大规模重构。

各层对象不跨层复用：Entity/DO 不直接作为请求或响应，DTO 不直接作为数据库更新对象，BO 不直接暴露给 Web，VO 不参与持久化。Controller 默认继承 `framework.web.BaseController`，使用统一响应和请求上下文；前端页面通过 `alpha-web/src/service` 调用 API，不直接使用 Axios。

规范等级：

- **必须**：代码评审阻断项；除非有书面例外记录。
- **应该**：默认采用，偏离时需在代码或设计说明中解释原因。
- **建议**：用于容量、性能或演进规划，需结合监控数据决策。

## 2. 数据库规约

### 2.1 表结构与关系

1. **必须禁止外键约束和数据库级联操作**。用户、角色、菜单、文件等关系由 Service 层在应用中校验和维护；删除使用明确条件和软删除策略，禁止依赖数据库级联。
2. Flyway 迁移只允许新增 `V<n>__description.sql`，不得修改已经在任何环境执行过的迁移。迁移必须包含明确过滤条件，禁止无条件批量删除或更新。
3. 新增字段**应该**定义为 `NOT NULL`，同时提供合理默认值或在迁移中完成数据回填。确需可空时，在设计说明中说明“未知”和“空值”的业务区别。
4. 时间、状态、删除标记、金额等字段必须与现有 `SystemEntity` 和项目约定保持一致；金额使用整数最小货币单位或明确精度的 `DECIMAL`，禁止浮点金额。
5. 单表预计达到 500 万行或 2GB 前，必须在容量评审中评估归档、分库分表、读写拆分和索引策略；达到阈值后由架构评审决定，不在业务代码中临时拆表。

### 2.2 查询与批量操作

6. **禁止 N+1 查询**：循环中不得逐条查询数据库。先批量加载并建立内存索引，或使用一次 JOIN/批量 `IN` 查询。评审时关注 Service 循环、Mapper XML 和懒加载边界。
7. 批量新增、更新、删除必须使用批处理（MyBatis-Plus 批量 API、JDBC batch 或单次批量 SQL），禁止在循环中逐条提交事务。批量操作必须设置合理批次大小并处理失败重试/回滚。
8. 单次 SQL 查询耗时**建议**控制在 0.5 秒以内。超过该阈值必须通过 SQL 摘要、执行计划和数据量定位原因；禁止通过简单增大连接池掩盖慢 SQL。
9. `IN` 查询集合**建议**不超过 1000 个元素。超过时拆分批次、临时表或改用批量 JOIN；禁止把任意大集合直接拼进 SQL。
10. Mapper 接口不得使用 `@Select`、`@Update` 等 SQL 注解；自定义 SQL 统一放在 `alpha-server/src/main/resources/mapper/<domain>/*.xml`，使用参数绑定，禁止字符串拼接用户输入。
11. SQL 日志只记录占位符 SQL 摘要，不记录真实参数值。任何查询优化必须同时检查索引、分页、排序和返回列，禁止 `SELECT *` 作为新增业务 SQL 的默认写法。

## 3. OOP 与对象模型规约

12. 新增 Entity、DTO、BO、VO、Query 等 POJO 属性必须使用包装数据类型（`Integer`、`Long`、`Boolean` 等），禁止用基本类型表达可缺失的业务字段。RPC/HTTP 契约中的 ID、状态、数量和开关也使用包装类型；若协议明确要求不可缺失，仍需在校验层保证非空。
13. 包装类对象比较必须使用 `equals()` 或 `Objects.equals()`，禁止使用 `==` 比较值。可能为空的对象优先使用 `Objects.equals(left, right)`，避免 NPE。
14. 所有 POJO 必须有可读的 `toString()`。Java `record` 使用编译器生成实现；普通 Entity/DTO/VO 必须显式实现或使用项目认可的生成方式，并确认不会输出密码、Token、密钥等敏感字段。
15. POJO 只承载数据和必要的不变性约束，禁止在 Entity/DTO/VO 中定义数据库访问、权限判断、外部调用或业务流程。业务逻辑放在 Service/Manager，转换放在明确的 assembler/静态工厂。
16. 循环体内拼接字符串必须使用 `StringBuilder`；日志优先使用 SLF4J 参数化占位符。少量固定片段的单次拼接可使用 `+`，不得在高频循环中重复创建中间字符串。
17. Query 对象用于查询条件和分页；查询参数超过两个时禁止使用无语义 `Map`，必须定义命名明确的 Query DTO。Query 不得直接暴露给 Web 响应。

## 4. 异常与日志规约

18. `@Transactional` 方法中捕获异常后必须回滚或重新抛出。禁止 catch 后只记录日志并返回成功；需要转换异常时使用 `BusinessException` 或保留原异常作为 cause，并确保事务回滚规则正确。
19. `finally` 块禁止 `return`、吞掉异常或覆盖原始返回值。资源优先使用 try-with-resources。
20. 日志必须使用 SLF4J 门面（`org.slf4j.Logger` / `LoggerFactory`），禁止 `System.out`、`System.err` 和直接依赖具体日志实现。使用 MDC 中的 traceId 关联请求。
21. 日志文件和可检索日志至少保留 15 天。当前项目维护任务默认保留更长周期；变更保留策略时必须同步评估审计、隐私和存储容量。
22. 日志禁止输出密码、Token、密钥、手机号、身份证、支付凭据和完整请求体。手机号等需要展示时必须脱敏；审计摘要使用现有 `AuditDetailSanitizer`，并遵守硬性禁止采集路径。
23. 异常日志必须记录完整堆栈（使用 `log.error("...", exception)`）以及足以定位问题的安全上下文：traceId、业务 ID、操作类型、数据范围和耗时。上下文必须经过脱敏和限长，禁止把敏感参数原样放入日志。
24. 对外响应只返回稳定的公开错误消息和 traceId，不返回堆栈、SQL、内部路径或第三方响应中的敏感字段。预期业务错误抛 `BusinessException`，由统一异常处理器转换。

## 5. 分层与领域模型规约

25. Web 层只负责 HTTP 映射、Jakarta Validation、权限校验、请求上下文和统一响应；不得写业务 CRUD、事务或 SQL。
26. Service 层负责事务边界、业务规则、状态变更和领域编排；事务注解放在 Service，不放在 Controller 或 Mapper。
27. Manager 层按需负责第三方平台封装、通用能力下沉和多个 DAO/Mapper 的可复用组合。微信、支付宝、OSS/COS 等外部协议适配不得散落在 Controller 或页面；单一领域且无复用价值时可直接由 Service 编排。
28. DAO 层在本项目中由 Mapper + XML 承担，只负责参数化持久化和查询映射。Mapper 不负责权限、事务、远程调用或 HTTP 响应。
29. 领域对象命名和放置遵循：DO/Entity 在 `entity`，DTO/Query 在 `dto`，BO 在 `bo`，VO 在 `vo`。各层转换必须显式、可审查；禁止为了省代码直接返回 Entity。
30. 新增接口必须同步更新 `docs/api.md`、OpenAPI 注解/契约和相关前端 service 类型；大版本变更必须提前通知，提供兼容期、版本化路径或适配层，不得静默破坏调用方。

## 6. 并发与集合规约

31. 禁止使用 `SimpleDateFormat` 作为 static 共享实例；优先使用 `java.time` 和 `DateTimeFormatter`，确需旧 API 时使用 ThreadLocal。
32. ThreadLocal 使用完必须在 `finally` 中 `remove()`，避免线程池复用造成上下文泄漏。项目请求 traceId 和 MDC 清理必须覆盖正常、异常和异步边界。
33. 禁止在 foreach 中直接对集合执行 `add`、`remove`、`clear`；需要边遍历边修改时使用 `Iterator`，或先收集变更后统一处理。`Arrays.asList()` 返回的集合不可增删，需可变集合时使用 `new ArrayList<>(...)`。
34. 线程池必须显式使用 `ThreadPoolExecutor` 或 Spring `ThreadPoolTaskExecutor` 配置核心线程数、最大线程数、队列、拒绝策略和线程命名，禁止使用 `Executors` 工厂创建无界线程池。项目异步配置统一放在 `framework/async` 或明确的基础设施包。
35. 集合初始化时根据容量估算指定初始容量，避免高频扩容；容量估算必须基于可解释的数据上限，不得盲目设置超大容量。

## 7. 配置、测试与交付规约

36. 配置文件必须包含版本号和修改记录。环境变量、默认值和敏感配置说明维护在 `deploy/.env.example`、`docs/development.md` 或对应模块文档；真实凭据只存在于被 Git 忽略的本地/部署密钥管理中。
37. 核心模块（认证、权限、系统配置、文件、支付、审计和安全过滤）单元测试覆盖率目标不低于 80%。行为变更至少增加聚焦测试；权限、事务、敏感数据和跨层契约优先覆盖失败路径。
38. 提交前必须运行与改动范围匹配的验证：后端 `./mvnw -f alpha-server/pom.xml test`/`package`，前端 `typecheck`、`test`、`lint`、`format:check`、`build`，部署变更执行 Compose `config --quiet`，并执行 `git diff --check`。
39. 提交前必须通过 Sonar 扫描，Critical 及以上问题为阻断项；High 问题必须有修复或审查记录。扫描不能替代单元测试、集成测试和浏览器验收。
40. 页面变更必须验收桌面、平板和手机视口，确认无重叠、无裁切、无不可达操作和无敏感信息泄漏。涉及真实存储、部署或第三方联调时，再执行对应 smoke test；没有凭据时不得伪造“已联调”。

## 8. 评审清单

提交或合并前，评审人至少确认：

- [ ] 是否新增了外键、级联、无条件批量更新/删除或 N+1 查询？
- [ ] SQL 是否参数化、分页、可解释并有索引/耗时证据？
- [ ] Entity/DTO/BO/VO/Query 是否分层，是否有基本类型、对象 `==` 或敏感 `toString()`？
- [ ] `@Transactional`、catch、finally、异步线程池和 ThreadLocal 是否满足回滚/清理规则？
- [ ] 日志是否使用 SLF4J、包含 traceId 和安全上下文且完成脱敏？
- [ ] 配置是否有版本/修改记录，敏感值是否只进密钥管理或加密存储？
- [ ] 是否补充了核心失败路径测试，并完成项目规定的质量命令和 Sonar 扫描？
- [ ] API 是否有兼容策略，前端 service、OpenAPI 和文档是否同步？

## 9. 例外与治理

历史代码不因本规范自动产生大范围重构任务。新增或修改代码必须遵守本规范；若暂时无法遵守，提交说明中必须写明影响、临时措施和治理计划。涉及数据库结构、公共 API、安全边界、日志保留或线程池参数的例外，必须经过模块负责人和架构评审确认。
