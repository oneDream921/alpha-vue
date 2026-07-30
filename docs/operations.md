# 运行与发布手册

## 发布前置条件

1. 仅在实现工作树完成改动，先检查 `git status --short`，确认不暂存 `deploy/.env`、上传目录、日志、构建产物以及其他无关的既有改动。
2. 为 MySQL、Redis、MinIO 配置生产专用凭据；Flyway 只新增迁移，绝不改写已发布迁移。
3. 在 CI 或干净环境依次执行后端 `test`、`package` 和前端 typecheck、test、lint、format、build。
4. 启动依赖服务与应用后运行 `deploy/smoke-test.sh`，检查健康接口、认证、鉴权、文件上传/预览/删除。
5. 最后复核变更范围，按主题提交；首次推送使用 `git push -u origin codex/alpha-vue-foundation`，不直接推送 `main`。

## 连接池、缓存与 SQL 监控

HikariCP 使用 Spring Boot BOM 管理的默认实现。开发池默认最小 2、最大 10，生产池默认最小 5、最大 20；空闲连接默认 10 分钟回收，连接最大生命周期默认 30 分钟。应基于数据库最大连接数、实例数和真实并发压测结果调整 `DB_POOL_MIN_IDLE`、`DB_POOL_MAX_SIZE`，不要以“越大越好”为目标。`DB_POOL_CONNECTION_TIMEOUT_MS` 和 MySQL 服务端连接超时必须协同配置，`DB_POOL_MAX_LIFETIME_MS` 必须小于数据库或代理的连接回收时间；`DB_POOL_LEAK_DETECTION_THRESHOLD_MS` 默认关闭，仅允许在受控诊断窗口临时设置。

SQL 日志页只保存当前进程内最近 SQL 摘要，不持久化、不跨实例聚合；采集开关和 Mapper 排除列表是单实例全局运行时状态，多实例部署时各实例互不同步。连接池指标通过 `/actuator/prometheus` 受控观测；SQL 文本脱敏与禁止任意 SQL 执行等安全边界见 [安全说明](security.md)。

Redis 使用 Lettuce 连接池。`REDIS_POOL_MAX_ACTIVE` 是单应用实例上限，`REDIS_POOL_MAX_WAIT` 默认 1 秒，避免连接耗尽时无限堆积请求；按 Redis 连接上限和副本数调整。Sa-Token 会话、验证码和登录失败窗口必须使用同一 Redis 集群/命名空间，并启用持久化与备份策略。

Redis 管理台用于在故障排查时按 `SCAN` 游标分批检查键空间，可通过前缀筛选缩小范围。删除会话、验证码或业务缓存键会立即影响线上状态，操作前必须完成二次确认。接口字段和查询参数见 [API 约定](api.md)，允许操作与脱敏边界见 [安全说明](security.md)。

## 观测与故障排查

- 探针：`/actuator/health/liveness`、`/actuator/health/readiness`。
- 指标：携带 Bearer Token 访问 `/actuator/prometheus`，并仅限内网采集端访问；健康探针公开，其余 Actuator 端点也必须在网关和应用层受控。
- 关联：响应头和响应体中的 `traceId` 可关联访问日志与应用日志。不要把密码、Token、验证码或 `.env` 内容附入工单。
- 连接耗尽：先检查 Hikari `hikaricp.connections.*` 指标、慢 SQL 与 MySQL `max_connections`，再调整池大小；不要通过无限 Redis 等待或盲目扩容掩盖问题。

## 边缘代理责任

应用只设置通用 API 安全头。Nginx/Ingress/API 网关必须负责 TLS、HSTS、CSP、限流、请求体上限、日志脱敏、Actuator 内网限制与可信代理头清洗。只有代理已清洗外部转发头时，应用才可依赖客户端 IP 头进行安全判断。
