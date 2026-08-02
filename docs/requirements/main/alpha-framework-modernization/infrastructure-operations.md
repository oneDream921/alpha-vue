# 基础设施与运维设计

## 1. 阶段边界

| 阶段 | 运行组件 |
| --- | --- |
| 当前/一期 | MySQL、Redis；开发可使用 local 文件，Compose smoke 使用 MinIO |
| 二期 | 可选 Spring Boot Admin Server |
| 三期 | 应用内受控定时任务；SnailJob 在 2026-08-02 重评为 `GO` 后作为独立调度扩展接入 |

Spring Boot Admin 和 SnailJob 不进入一期 Docker 默认启动链路；SnailJob 仅通过
`snailjob` Compose profile 和显式 Alpha Client 配置启用。

## 2. 本地开发

目标是最快启动：

- `FILE_STORAGE_PROVIDER=local` 作为开发默认。
- MySQL、Redis 使用 Docker。
- MinIO 可按文件任务或 smoke 需要启动。
- 后端和前端继续使用现有脚本管理。
- 不因未来运维扩展增加一期本地必启容器。

MinIO 必须保留真实验证路径，不能因为开发默认 local 而失去生产推荐实现的测试。

## 3. 生产与 Compose

- Compose/生产推荐 MinIO。
- MinIO 应用凭据与 root 凭据分离。
- MySQL、Redis、MinIO 凭据只走环境或部署密钥。
- Actuator 非健康端点只允许内网、网关或独立认证访问。
- Redis、MySQL 和 MinIO 不直接暴露公网。
- 可信代理列表由部署配置提供。

## 4. Hikari 与数据库

一期替换 Druid：

- 池大小根据数据库上限、实例数和真实并发确定。
- 连接超时必须有界。
- 最大生命周期小于数据库或代理连接回收时间。
- 泄漏检测只用于受控诊断，不能长期使用过低阈值。
- 连接池指标通过 Micrometer。

不保留 Druid 监控入口、账号、白名单和环境变量。

## 5. Redis

### 一期

- Redisson 是唯一业务 Client。
- 迁移窗口先只读盘点，再按白名单清理旧前缀 `auth:captcha:*`、
  `auth:login:failure:*`、`system:config:*`、`system:dict:*`、`satoken:*`。
- 所有用户重新登录。
- Redis Monitor 提供只读 INFO、受限 SCAN 和受控单键删除。
- 单键删除需要权限、二次确认和操作日志。

### 二期

- 详细 INFO 文本视图。
- 命令、内存等 ECharts 当前快照。
- 登记缓存的三级值策略。
- 普通明文查看使用独立权限并产生查看审计。

永久禁止：

- `KEYS`
- `FLUSHDB`
- `FLUSHALL`
- 任意命令终端
- 任意值编辑
- 安全命名空间明文查看

## 6. 日志

### 应用文件日志

- 使用 Logback 按日期和大小滚动。
- 生产默认保留 30 天，具体值走部署配置。
- 每行包含 `traceId`。
- 不输出请求体或凭据。

### 数据库日志

- 一期保存登录与操作元数据。
- 三期默认由应用内 Spring 定时任务执行保留清理。
- 登录日志 180 天。
- 成功操作日志 180 天。
- 已处理异常日志 365 天。
- 未处理异常日志不自动删除。

清理：

- 每批默认最多 1000 条。
- 每批独立短事务。
- 输出扫描、删除、跳过和耗时。
- 达到单次上限时成功结束并等待下次调度，不无限占用数据库。

## 7. Spring Boot Admin

进入二期的前提：

- Boot 4 Server/Client Spike 通过。
- 独立进程能监控 `alpha-server`。
- 认证和网络边界明确。
- Alpha 业务服务宕机时 Admin Server 仍可访问。

职责：

- 健康、JVM、线程、日志级别和 Hikari 指标。
- 不读取业务数据库。
- 不替代 Alpha 操作日志、SQL 摘要和 Redis 诊断。

Alpha Web 只提供受权限控制的外部入口，不重复开发 Admin UI。

## 8. SnailJob

SnailJob 已于 2026-08-02 重评为 `GO`，但仍不进入默认启动链路。正式启用的前提：

- Boot 4 Client 成功链路 Spike 通过，并补齐失败、超时、重试和恢复验证。
- Server 使用独立 Schema。
- 原生管理端认证和网络边界明确。
- Alpha Executor 注册和手动任务验证通过。
- 多实例场景、维护成本和退出方案已形成书面决策。

采用后可承载的任务：

- 系统健康检查：无业务副作用。
- 日志清理：按保留策略分批执行。

不放入：

- RuoYi 测试任务。
- 支付、工作流示例。
- 没有业务所有者的缓存预热。
- 自动删除孤儿文件。

## 9. 文件存储

- 开发默认 local。
- Compose/生产推荐 MinIO。
- Provider 连接信息走部署配置。
- 上传业务规则在类型化配置注册后可动态调整，但不能超过 multipart 硬上限。
- 存储切换不自动迁移历史文件。
- 未来迁移任务必须先报告、再显式执行，并支持校验与重试。

## 10. 发布门槛

一期发布前：

- 当前正式规范已从 Druid/Lettuce/Knife4j 更新到实际实现。
- Redis 迁移窗口、回滚条件和会话失效公告明确。
- local 与 MinIO smoke 均通过。
- `clientId` 并发会话真实 HTTP 测试通过。
- Actuator 网络边界验证完成。

后续运维扩展不得降低一期启动速度；默认开发脚本只启动当前任务真正需要的组件。
