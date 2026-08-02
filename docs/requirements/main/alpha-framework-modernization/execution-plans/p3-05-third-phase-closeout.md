# P3-05 第三期收口执行记录

## 当前状态

`COMPLETED`（2026-08-02）。自动化、配置、本地 HTTP 验证和用户最终人工验收均已完成；
G4-M04/G4-M06 已由用户明确取消并记录为 `N/A`，生产凭据复核和最终架构/安全/可维护性评审
已完成。

## 本次验证

- `./mvnw -f alpha-server/pom.xml test`：133 项测试，0 failure，0 error，8 项按环境跳过。
- `./mvnw -f alpha-server/pom.xml package`：构建成功。
- `pnpm --dir alpha-web typecheck`：通过。
- `pnpm --dir alpha-web test`：28 个测试文件、72 项测试通过。
- `pnpm --dir alpha-web lint`：通过。
- `pnpm --dir alpha-web format:check`：通过。
- `pnpm --dir alpha-web build`：构建成功；仅保留既有动态导入和 chunk 大小提示。
- `docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose --env-file deploy/.env --profile snailjob -f deploy/docker-compose.yml config --quiet`：通过。
- `deploy/.env` 未被 Git 跟踪；跟踪配置未发现 `change-me`、`replace-with` 或 `admin123` 默认值，
  关键密钥项均为非占位值且未输出。
- Compose 已为 MySQL、Redis、MinIO、SnailJob DB 和 SnailJob 服务提供健康检查；五个运行容器均为
  `running healthy`。SnailJob 使用 `/snail-job` 重定向可达性作为服务级探针，因为 2.0.2 未暴露
  `/actuator/health`。
- 当前 Alpha 实例 `/actuator/health` 返回 HTTP 200，liveness/readiness 均为 `UP`。
- `API_BASE_URL=http://127.0.0.1:8080/api SMOKE_USERNAME=<本地测试账号> SMOKE_PASSWORD=<本地测试密码> bash deploy/smoke-test.sh`：通过。
- 未授权 `/api/auth/profile` 返回 HTTP 401；响应头 `X-Trace-Id` 与响应体 `traceId` 一致，
  readiness 返回 HTTP 200/`UP`，完成一次故障定位关联演练。
- 最终架构、安全和可维护性复核通过：SnailJob 探针只检查 `/snail-job` 服务可达性，不新增业务
  API 或敏感数据输出；配置、健康检查、traceId 和取消项边界与当前代码一致。
- `./mvnw -f alpha-server/pom.xml -Dtest=LogRetentionMaintenanceIntegrationTests test`：通过；隔离 H2
  数据中真实删除 3 条过期登录/已处理操作日志，保留近期日志和未处理失败日志，批次上限为 10。
- `git diff --check`：通过。

## 已取消门禁

- G4-M04：用户取消，不纳入本期验收；删除型维护任务继续保持默认 `dryRun=true`。
- G4-M06：用户取消，不纳入本期验收；不执行备份恢复覆盖演练。

## 验收结论

- G4 最终放行：通过。用户于 2026-08-02 确认“没有问题，我验收通过了”。

## 边界结论

P3-03 Lock4j 的 `DEFER` 和 P3-04 数据库自增 ID 决策不阻断 G4；当前阻断项集中在生产运维
证据与人工授权，不需要新增 Lock4j 或 Snowflake 实现。
