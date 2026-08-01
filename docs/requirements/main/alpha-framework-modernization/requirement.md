# Alpha Vue 框架现代化与分阶段能力建设

## 背景

Alpha Vue 当前已经具备登录、RBAC、文件管理、审计日志、traceId、Redis 运维、SQL 摘要监控和响应式管理端等基础能力，但技术基线仍包含 Druid、Lettuce、RedisTemplate、Knife4j 等计划替换的实现。项目希望学习 RuoYi-Vue-Plus 与 Vben5 的成熟做法，同时保持“开发快、运行快、简单通用、规范但不复杂”的轻量单体定位。

本需求位于 `main` 分支，采用长期蓝图与分阶段交付分离的方式：第一期完成基础设施瘦身和 `clientId` 会话闭环；较重的运维平台和数据网格能力按需评估，跨设备用户偏好不纳入当前规划。

配套文档：

- [目标架构](./architecture.md)
- [后端设计](./backend-design.md)
- [前端设计](./frontend-design.md)
- [基础设施与运维设计](./infrastructure-operations.md)
- [详细实施计划](./implementation-plan.md)
- [验收清单](./acceptance.md)
- [项目专属 Skill 调整说明](./project-skill-plan.md)

## 目标与范围

### 目标

- 保持 `alpha-server + alpha-web` 业务单体，不拆分业务微服务或大量 Maven 子模块。
- 删除 Druid、p6spy、SkyWalking 目标，使用 HikariCP、应用内 SQL 摘要和 Actuator 作为基础观测能力。
- 先通过 Spring Boot 4 兼容性 Spike 验证 Redisson、Therapi、Spring Boot Admin、SnailJob、Lock4j 等候选依赖，再决定引入阶段。
- 第一期完成 Redisson 对 Lettuce、RedisTemplate 和 JDK 序列化的干净替换；Redis 访问通过领域 Adapter 和 Spring Cache 管理。
- 第一期强制登录携带公开 `clientId`，完成跨客户端并存、同客户端单会话、在线会话查询与指定会话强制下线。
- 保留本地与 MinIO 两个完整存储实现，通过可插拔 `StorageProvider` 为后续 OSS、COS、S3 留出实现入口。
- 使用 `traceId` 关联请求、应用日志、操作日志和 SQL 摘要；补充 IP、归属地、浏览器、操作系统和客户端快照。
- 建立类型化 `sys_config` 注册机制，只有代码登记、类型明确、范围受控的运行时策略才能动态生效。
- 前端继续使用 Vue 3、TypeScript、Vite、Ant Design Vue、UnoCSS、Pinia、Vue Router 和 Axios；借鉴 Vben5 的布局、密度和交互，不复制其 Monorepo。
- 第二期按真实收益增强管理体验：统一 Ant Table 列表、轻量列设置、Redis ECharts 和更丰富的操作日志详情；VXE Table 与跨设备用户偏好不采用。
- AFK 核心体系保持不变，只更新或扩展 Alpha 项目专属规则与 Skill。
- 提供普通执行模型可按步骤完成的详细任务单，并为安全、数据、会话和依赖迁移设置强模型复核点。

### 分阶段范围

| 阶段 | 交付主题 | 阶段约束 |
| --- | --- | --- |
| 阶段 0 | 兼容性与当前基线 | 当前测试基线；候选依赖 Spike；`GO/DEFER/REPLACE` 决策；规范冲突清单 |
| 一期 | 基础瘦身与认证闭环 | Hikari；SpringDoc 基线；Redisson 干净迁移；Spring Cache；类型化配置注册；完整 `clientId` 会话；在线用户；增强日志元数据；存储 Provider 整理；前端认证与领域 API 整理 |
| 二期 | 管理体验增强 | 各能力按真实收益单独触发和确认：Ant Table 列表与列设置；Redis 详细监控与 ECharts；普通缓存三级展示策略；经安全评审的操作日志参数详情；Spring Boot Admin；未触发项记 `N/A` |
| 三期 | 任务与投产能力 | 应用内受控定时清理；生产验证；投产前 ID 决策与数据重建/迁移；SnailJob、Lock4j 仅在触发条件满足并重新评审为 `GO` 后采用 |
| 长期 | 按业务需求扩展 | 微信小程序客户端；OSS/COS/S3 Provider；文件迁移；孤儿文件报告；第二数据源 |

### 非目标（本期不做）

以下内容不进入一期实现：

- Spring Boot Admin Server、SnailJob Server、Lock4j 和自动日志清理。
- VXE Table 引入、Redis ECharts、跨设备用户偏好和普通缓存明文查看。
- 普通操作日志完整请求体或完整响应体持久化。
- Snowflake ID 强制切换和历史 ID 原地迁移。
- OSS、COS、S3 的具体 Provider 实现。
- 工作流、租户、岗位、数据权限、代码生成、动态数据源、ELK、Loki、SkyWalking。
- Validation 国际化、复杂 OAuth、`clientKey`、`clientSecret` 和 Refresh Token。
- Vben Monorepo、Turbo、`@vben/*` 包、多 UI 框架适配和低代码 CRUD。

## 数据与接口（含库表 / DDL）

本需求阶段只定义数据语义，不直接提供可执行 DDL。所有 DDL 必须在对应实施任务中以追加 Flyway 迁移落地，并经人工审核。

### 一期数据变化

#### 客户端

新增最小客户端定义，初始数据为 `pc-admin`：

| 字段 | 语义 |
| --- | --- |
| `id` | 主键；一期沿用当前主键策略，投产前统一决策 |
| `client_id` | 对外公开、全局唯一的客户端标识 |
| `client_name` | 客户端名称 |
| `device_type` | Sa-Token 设备维度 |
| `status` | 是否允许登录 |
| `active_timeout` | 无操作超时 |
| `token_timeout` | Token 绝对有效期 |
| `created_at` / `updated_at` | 审计时间 |

不增加 `clientKey`、`clientSecret`、授权类型或 OAuth 表。

#### 登录与操作日志

一期在现有日志数据上补充以下快照字段：

- `trace_id`
- `client_id`
- `device_type`
- 客户端 IP
- IP 归属地
- 浏览器
- 操作系统
- User-Agent（有长度上限）
- 操作日志的部门快照、Java 方法、HTTP 方法、URI、响应状态、耗时和异常摘要

一期所有接口只保存操作与响应元数据，不保存请求参数、请求体或响应体。二期只有通过
独立安全评审后，才允许对普通管理写操作增加可配置、脱敏和截断的参数摘要。

#### 配置注册

`sys_config` 继续保存运行时业务配置，但必须增加代码侧注册表。注册项定义：

- 配置键
- 值类型
- 默认值
- 取值范围或枚举
- 是否敏感
- 是否动态生效
- 所属领域

未登记配置不得被运行时代码读取为技术开关。

### 二期数据变化

当前不新增用户偏好表。列设置使用前端本地、带版本的 `localStorage`，不依赖数据库，
也不规划跨设备同步接口。

### 接口变化

一期：

- 登录请求新增必填 `clientId`。
- 登录成功后的服务端会话绑定 `clientId` 和设备类型。
- 在线用户接口按会话返回 `clientId`、设备、登录时间、最后活动时间、IP、地点、浏览器、系统和过期信息。
- 强制下线接口按会话操作，不按用户全量下线；禁用/删除用户和关键权限变更仍可失效全部会话。
- Redis 监控一期维持只读概览、受限 `SCAN` 和受控单键删除，不扩展成通用 Redis 控制台。

二期：

- 普通业务缓存可按登记的缓存定义配置 `HIDDEN/MASKED/PLAIN`；安全命名空间永久 `HIDDEN`。
- 操作日志详情可按注解和全局策略保存经过强制清理与截断的参数摘要。

## 实现要点

| 模块 / 文件 | 改动摘要 |
| --- | --- |
| `alpha-server/pom.xml` | 依兼容性决策分阶段替换连接池、Redis、文档和后续运维依赖 |
| `framework/mybatis` | Hikari/MyBatis 配置、SQL 摘要边界、未来多数据源解耦 |
| `framework/redis`、`framework/cache` | Redisson Codec、命名空间、缓存管理和领域 Adapter 基础 |
| `modules/auth` | `clientId` 校验、会话绑定、并发规则和在线会话 |
| `modules/log` | 安全日志事件、设备/IP 地点快照和 traceId 关联 |
| `modules/system` | 客户端管理、类型化配置注册 |
| `modules/file` | Provider 自动发现、开发 local 与 Compose/生产 MinIO |
| `alpha-web/src/service` | 按领域拆分 API，保持迁移期兼容出口 |
| `alpha-web/src/stores` | 只持久化 Token、clientId 和存储模式，刷新时重取资料与菜单 |
| `alpha-web` 列表页 | 统一使用 Ant Table；按页面需要提供本地列设置 |
| `.project-agent` | 只更新项目专属规则与 Skill，不修改 AFK 核心 |

详细顺序见 [实施计划](./implementation-plan.md)。

## 风险与待办

- Spring Boot 4 兼容性是硬闸门；参考项目使用某依赖不等于 Alpha 可以直接采用。
- Redisson 迁移会改变 Redis 序列化格式；只允许按已确认旧前缀白名单清理并使旧会话
  失效，新 `alpha:*` 仅按本次迁移登记清单用于回滚，不得影响其他 Key。
- `clientId` 会话不是 DTO 小改，需要明确 Sa-Token device、Token 元数据、指定会话踢下线和审计行为。
- 日志参数持久化存在敏感数据泄露风险；一期只做摘要，二期必须先完成安全评审与递归清理测试。
- 类型化配置注册表完成前，不得继续把技术开关或密钥塞入 `sys_config`。
- VXE 和跨设备偏好不纳入当前规划；ECharts 仍需先证明收益，不得以长期目标为由全量改造现有页面。
- IP 归属地仅是离线数据库估算；内网、代理和移动网络可能不准确。
- 开发环境默认 local 是为了启动速度；MinIO 必须通过 Compose 与真实 smoke 保持可用。
- 当前正式规范描述现状。每个阶段实现完成后，必须同步更新 `docs/conventions.md`、`docs/security.md`、`docs/api.md`、`docs/development.md` 和 `docs/operations.md`，不能提前把目标写成已实现事实。

## 测试与验收

采用“开发期定向验证、阶段里程碑验证、最终集中验收”：

- 普通任务运行聚焦测试、编译或 typecheck。
- 三个自动化里程碑分别覆盖后端基础设施、前端结构、完整 Docker/HTTP 联调。
- 五个人工验收节点覆盖兼容性、基础设施、认证会话、管理体验和最终投产。
- 正常命令只保留摘要，失败时读取相关日志片段，避免重复消耗上下文。

详细场景见 [验收清单](./acceptance.md)。

### 人工验收

| 场景 | 怎么验收 | 预期 | 结果 |
| --- | --- | --- | --- |
| 阶段 0 兼容性 | 阅读 Spike 报告并复核依赖最小运行结果 | 每个候选依赖有明确 `GO/DEFER/REPLACE`，没有凭参考项目直接引入 | |
| 一期基础设施 | 启动应用并检查连接池、Redis、OpenAPI、SQL 摘要 | 无 Druid/Lettuce/JDK 序列化残留，核心功能可用 | |
| `clientId` 会话 | 在同客户端和不同客户端重复登录并强制下线 | 跨客户端并存，同客户端旧会话失效，可下线指定会话 | |
| 日志定位 | 执行成功和失败操作，用 traceId 联查日志 | 可看到安全元数据、IP/地点/设备，不出现敏感内容 | |
| 文件存储 | 分别使用 local 和 MinIO 上传、预览、删除 | 两种 Provider 均可用，切换后历史文件仍按原 Provider 访问 | |
| 二期表格体验 | 调整列显示、顺序和对齐方式，并在桌面、平板、手机操作 | 功能完整、无重叠；设置可在同一浏览器恢复 | 通过，2026-08-02（用户人工验收） |
| 二期缓存监控 | 查看 INFO、图表和不同保护级别缓存 | 安全命名空间始终隐藏，明文查看受权限和审计保护 | |
| 最终回归 | 执行完整自动化、Docker、HTTP 和 Playwright 验收 | 所有门槛通过，正式规范与实现一致 | |

## 状态（维护人自填）

- [ ] 需求已确认
- [ ] 联调完成
- [ ] 人工验收通过
