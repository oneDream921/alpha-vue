# P3-05 第三期收口执行记录

## 当前状态

`READY_FOR_ACCEPTANCE`（2026-08-02）。自动化、配置和本地 HTTP 验证已完成；G4-M04/G4-M06
已由用户明确取消并记录为 `N/A`，生产凭据复核、最终评审和用户最终验收仍未完成。

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
- 当前 Alpha 实例 `/actuator/health` 返回 HTTP 200，liveness/readiness 均为 `UP`。
- `API_BASE_URL=http://127.0.0.1:8080/api SMOKE_USERNAME=admin SMOKE_PASSWORD=admin123 bash deploy/smoke-test.sh`：通过。
- `./mvnw -f alpha-server/pom.xml -Dtest=LogRetentionMaintenanceIntegrationTests test`：通过；隔离 H2
  数据中真实删除 3 条过期登录/已处理操作日志，保留近期日志和未处理失败日志，批次上限为 10。
- `git diff --check`：通过。

## 已取消门禁

- G4-M04：用户取消，不纳入本期验收；删除型维护任务继续保持默认 `dryRun=true`。
- G4-M06：用户取消，不纳入本期验收；不执行备份恢复覆盖演练。

## 仍需完成门禁

- G4 生产配置复核：本次只校验 Compose 模板和 profile 可解析，未把本地 `deploy/.env` 内容
  当作生产凭据审计证据，也未输出任何敏感值。
- G4 健康检查与故障定位：需确认 MySQL、Redis、MinIO 和任务组件的健康检查覆盖，并完成一次
  日志、指标和 traceId 关联的故障定位演练。
- G4 最终放行：强模型最终架构/安全/可维护性评审和用户最终验收仍待完成。

## 边界结论

P3-03 Lock4j 的 `DEFER` 和 P3-04 数据库自增 ID 决策不阻断 G4；当前阻断项集中在生产运维
证据与人工授权，不需要新增 Lock4j 或 Snowflake 实现。
