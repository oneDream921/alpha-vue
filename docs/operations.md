# 运行与发布手册

## 发布前置条件

1. 仅在实现工作树完成改动，先检查 `git status --short`，确认不暂存 `deploy/.env`、上传目录、日志、构建产物以及其他无关的既有改动。
2. 为 MySQL、Redis、MinIO 配置生产专用凭据；Flyway 只新增迁移，绝不改写已发布迁移。
3. 在 CI 或干净环境依次执行后端 `test`、`package` 和前端 typecheck、test、lint、format、build。
4. 启动依赖服务与应用后运行 `deploy/smoke-test.sh`，检查健康接口、认证、鉴权、文件上传/预览/删除。
5. 最后复核变更范围，按主题提交；首次推送使用当前专题分支，例如 `git push -u origin codex/<slug>`，不直接推送 `main`。

## 连接池、缓存与 SQL 监控

HikariCP 使用 Spring Boot BOM 管理的默认实现。开发池默认最小 2、最大 10，生产池默认最小 5、最大 20；空闲连接默认 10 分钟回收，连接最大生命周期默认 30 分钟。应基于数据库最大连接数、实例数和真实并发压测结果调整 `DB_POOL_MIN_IDLE`、`DB_POOL_MAX_SIZE`，不要以“越大越好”为目标。`DB_POOL_CONNECTION_TIMEOUT_MS` 和 MySQL 服务端连接超时必须协同配置，`DB_POOL_MAX_LIFETIME_MS` 必须小于数据库或代理的连接回收时间；`DB_POOL_LEAK_DETECTION_THRESHOLD_MS` 默认关闭，仅允许在受控诊断窗口临时设置。

SQL 日志页只保存当前进程内最近 SQL 摘要，不持久化、不跨实例聚合；采集开关和 Mapper 排除列表是单实例全局运行时状态，多实例部署时各实例互不同步。连接池指标通过 `/actuator/prometheus` 受控观测；SQL 文本脱敏与禁止任意 SQL 执行等安全边界见 [安全说明](security.md)。

业务 Redis 统一使用直接 Redisson 4.6.1 Client，连接池、连接超时、命令超时和重试由 `REDIS_POOL_MAX_ACTIVE`、`REDIS_POOL_MIN_IDLE`、`REDIS_CONNECT_TIMEOUT`、`REDIS_TIMEOUT`、`REDIS_RETRY_INTERVAL` 和 `REDIS_RETRY_ATTEMPTS` 控制；连接耗尽或 Redis 不可用时向适配器抛出明确基础设施异常，不返回假空值。生产业务键统一使用 `alpha:*`，不对旧业务键双读、双写或清理。Sa-Token 逻辑键使用稳定哈希映射，并以有界索引维持搜索语义。

Redisson Spring Cache 使用显式白名单对象 Codec，默认不缓存空值，并对已登记缓存使用 `REDIS_CACHE_TTL`（当前 10 分钟）。配置和字典缓存失效由业务提交成功边界触发。

Redis 管理台用于在故障排查时按 `SCAN` 游标分批检查键空间，可通过前缀筛选缩小范围。增强指标由每个应用实例独立每分钟执行一次只读 `INFO ALL` 采样，成功样本只保存在该实例内存，最多 24 小时或 1,440 点；应用重启清空，多实例之间不聚合。面板可用于在 10 分钟内查看内存、连接、命令吞吐和 Top 10 命令的异常，`DEGRADED` 表示最近采样失败，`STALE` 表示最后成功数据已超过两个采样间隔且至少两分钟。需要紧急回退时设置 `REDIS_METRICS_ENABLED=false` 并重启应用，既有概览、受限 `SCAN` 和单键删除保持可用。删除会话、验证码或业务缓存键会立即影响线上状态，操作前必须完成二次确认。接口字段和查询参数见 [API 约定](api.md)，允许操作与脱敏边界见 [安全说明](security.md)。

## 观测与故障排查

- 探针：`/actuator/health/liveness`、`/actuator/health/readiness`。
- 指标：携带 Bearer Token 访问 `/actuator/prometheus`，并仅限内网采集端访问；健康探针公开，其余 Actuator 端点也必须在网关和应用层受控。
- 文档：仅开发 profile 暴露 `/swagger-ui/index.html` 和 `/v3/api-docs/{group}`；生产 profile 应验证 `/v3/api-docs`、`/v3/api-docs/{group}`、`/swagger-ui.html` 与 `/swagger-ui/index.html` 均返回 404。
- 关联：响应头和响应体中的 `traceId` 可关联访问日志与应用日志。不要把密码、Token、验证码或 `.env` 内容附入工单。
- 连接耗尽：先检查 Hikari `hikaricp.connections.*` 指标、慢 SQL 与 MySQL `max_connections`，再调整池大小；不要通过无限 Redis 等待或盲目扩容掩盖问题。

## 边缘代理责任

应用只设置通用 API 安全头。Nginx/Ingress/API 网关必须负责 TLS、HSTS、CSP、限流、请求体上限、日志脱敏、Actuator 内网限制与可信代理头清洗。只有代理已清洗外部转发头时，应用才可依赖客户端 IP 头进行安全判断。

审计日志使用 `TRUSTED_PROXY_ADDRESSES` 配置受信代理对端；未配置时应用只记录 socket 对端 IP。内网地址标记为“内网 IP”，外部地址使用 `IP_LOCATION_XDB` 指向的 ip2region 离线 XDB 查询，未配置或查询失败时显示“未知”。

操作日志详情通过 `log:operation:detail` 独立权限访问。带有 `@OperationLog` 的入口默认采集摘要；不需要采集的入口显式关闭 `saveRequest` 和 `saveResponse`，硬性敏感路径仍不可采集。失败时丢弃摘要，不回退保存原始请求或响应；异常堆栈和摘要均有字段长度上限。

操作日志队列使用 Redis Streams Consumer Group。数据库写入成功后才确认消息；消费者异常时由 Pending 消息接管机制重新处理，超过重试上限的消息进入死信 Stream。Redis 不可用时普通操作使用有界异步数据库降级，必须通过 Redis、数据库和死信指标监控投递健康度。

## 应用内维护任务

应用内维护任务由后端进程固定延迟调度，不依赖 SnailJob。当前任务包括：

- 日志保留：按 `created_at` 分批选择登录日志，以及成功或已处理的操作日志；未处理失败操作日志不会被删除。
- 本地临时文件清理：只处理本地存储根目录下过期的 `.upload-*.tmp` 文件，不删除业务对象。
- 存储一致性报告：分批打开未删除文件元数据对应对象，只报告缺失对象和未知 Provider，不自动修复。
- Sa-Token 索引修复：仅在 Redisson 会话存储启用时检查搜索索引，默认只报告过期索引。
- 运行健康摘要：记录 uptime 和 JVM 内存摘要，不包含凭据、环境变量或连接信息。

删除型任务默认 `dry-run=true`，生产启用真实删除前应先观察至少一轮候选数量和日志摘要。每轮任务有批次上限，
达到上限即结束并等待下次调度，不会无限占用数据库或文件系统。真实删除或任务失败时，会写入一条系统操作
日志摘要；摘要只包含任务名、状态、扫描数量和影响数量，不包含请求体、Token、Redis 值或文件内容。

常用回退方式：

- 设置 `MAINTENANCE_ENABLED=false` 并重启应用，可关闭全部维护调度。
- 设置对应 `*_DRY_RUN=true` 并重启应用，可保留报告但停止真实删除。
- 降低 `*_BATCH_SIZE` 可收窄单轮影响范围。

### SnailJob 调度扩展

SnailJob Server 是独立组件，不嵌入 Alpha Server，也不复用 Alpha 业务 Schema。当前固定使用
`opensnail/snail-job:2.0.2`，开发环境通过 `snailjob` Compose profile 启动，并使用独立的
`snailjob-db` 数据库容器。Alpha 侧仅注册 `alphaMaintenanceJob` Executor。

首次创建 `snailjob-mysql-data` 时会挂载 `deploy/snailjob-schema.sql` 初始化官方 2.0.2 表结构；已有数据卷不会重复执行初始化脚本。若数据库已存在但未初始化，请先由数据库管理员审核该脚本后执行，禁止直接删除已有 SnailJob 数据卷。

启用 SnailJob 前必须配置 `SNAIL_JOB_ENABLED=true`、`SNAIL_JOB_TOKEN` 及 Server RPC 地址，
并设置 `MAINTENANCE_SPRING_SCHEDULER_ENABLED=false`。SnailJob 不可用时的回退方式是关闭
SnailJob 并重新启用 Spring 调度器；切换期间只允许一个调度来源运行。
