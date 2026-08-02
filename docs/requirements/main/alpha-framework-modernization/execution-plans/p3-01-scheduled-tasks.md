# P3-01 应用内基础定时任务执行记录

## 范围

本任务实现应用内低风险维护能力，不引入 SnailJob、Lock4j、独立任务中心或前端管理页面。

调度实现使用 Spring Framework 内置的 `@EnableScheduling` 与 `@Scheduled`，采用固定延迟
执行：默认首次延迟 5 分钟、后续每小时执行一次，可通过 `alpha.maintenance.*` 环境变量调整。
任务在当前应用进程内执行，不提供独立任务中心、分布式调度或跨实例协调能力。

已实现任务：

- 日志保留清理：分批处理登录日志、成功操作日志和已处理失败操作日志。
- 本地临时文件清理：只清理过期 `.upload-*.tmp` 文件。
- 存储一致性报告：只报告缺失对象和未知 Provider，不自动修复。
- Sa-Token Redis 索引修复：仅 Redisson 会话存储启用时检查过期索引。
- 运行健康摘要：输出 uptime 和 JVM 内存摘要。

## 安全边界

- 删除型任务默认 `dry-run=true`。
- 每轮任务都有批次上限，最大 1000。
- 日志保留不会删除未处理失败操作日志。
- 存储一致性只报告，不删除元数据或对象。
- 真实删除或任务失败时写入系统操作日志摘要；摘要只包含任务名、状态、扫描数量和影响数量。
- 可通过 `MAINTENANCE_ENABLED=false` 关闭全部维护调度。

## 验证

- `./mvnw -f alpha-server/pom.xml -Dtest=LogRetentionMaintenanceServiceTests,LogRetentionMaintenanceIntegrationTests,TempFileMaintenanceServiceTests,StorageConsistencyMaintenanceServiceTests,SessionIndexMaintenanceServiceTests test`：通过，7 项测试，0 失败，0 错误。
- `./mvnw -f alpha-server/pom.xml test`：通过，131 项测试，0 失败，0 错误，8 项因环境条件跳过。
- `./mvnw -f alpha-server/pom.xml package`：通过，131 项测试，0 失败，0 错误，8 项因环境条件跳过，并生成可执行 JAR。
- `docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet`：通过。
- `git diff --check`：通过。

## 当前状态

`COMPLETED`（2026-08-02）。用户已完成启动和运行验收：后端健康状态为 `UP`，
日志中 5 个维护任务均为 `status=OK`，删除型任务均为 `dryRun=true`，未发现敏感信息泄露或越界删除。
