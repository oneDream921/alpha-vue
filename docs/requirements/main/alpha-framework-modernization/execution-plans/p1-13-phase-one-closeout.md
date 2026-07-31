# P1-13 第一期收口执行计划

## 目标

以当前已合并的 P1-01 至 P1-12 为基线，完成第一期的正式文档、残留清理、全量自动化验证、真实 HTTP 冒烟与独立安全/兼容/范围复核，使 P1-13 达到可提交状态。未由用户完成的浏览器桌面/移动端人工验收和一期放行确认保持未通过。

## 已核对基线

- 分支：`codex/p1-13-phase-one-closeout`，基线提交 `3c79e99`，开始时工作区干净。
- 后端和前端本地进程已运行；P1-11 local/MinIO 文件闭环、P1-12 Bearer 登录闭环已有前序真实验证记录。
- README、开发、安全、运维、API 和现代化架构文档已经描述 Hikari、Redisson、clientId、SQL 摘要和 local/MinIO 存储边界；已核对架构文档且未发现需要修改的失效内容，本任务只修正实际检出的正式文档或脚本残留。

## 范围与非目标

范围：`README.md`、`docs/{development,security,operations,api}.md`、现代化架构文档、现代化计划与验收记录，以及验证所需的非破坏性命令和 HTTP 请求。

非目标：不启动 P2；不修改数据库结构、不执行 DDL/DML；不修改 `deploy/.env`；不以自动化替代浏览器移动端人工验收；不将未证明的 G1/G2 项标为通过。

## 执行顺序

1. 检索已移除依赖、配置、端点和文档残留；仅清理已证实失效的正式内容。
2. 运行后端 `test`、`package`，前端 `typecheck`、`test`、`lint`、`format:check`、`build`，以及 Compose 配置校验。
3. 使用已运行的本地服务执行登录、Bearer profile/routes、会话、日志、缓存、SQL 摘要、文件和数据库相关 HTTP 冒烟；临时数据仅经应用 API 创建和清理。
4. 由独立强模型复核差异、自动化证据、会话/安全/数据边界和任务范围；如有阻断问题，最多两轮在本任务范围内修复并复核。
5. 汇总可复现证据，更新实施计划和验收文档中已支持的自动化项。人工验收清单保留给用户，等待 `测试通过` 后再更新对应状态。

## 验证矩阵

| 类别 | 证据 |
| --- | --- |
| 后端 | `./mvnw -f alpha-server/pom.xml test`、`package` 与 Surefire 报告 |
| 前端 | `pnpm --dir alpha-web typecheck/test/lint/format:check/build` |
| 部署 | `docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet` |
| HTTP | 登录获取 Bearer、`profile`、`routes`、会话/日志/SQL/文件受控请求及无敏感输出 |
| 人工 | 1440x900、1024x768、390x844 下的登录、在线用户、登录日志、操作日志和 SQL 监控 |

## 停止条件与回退

若全量验证失败、HTTP 冒烟显示鉴权或数据边界问题、或独立复核发现会话/安全阻断项，则仅修复 P1-13 范围内的问题并重新验证。此任务不创建或执行不可逆数据库操作；文档与代码变更均可通过放弃当前专题分支回退。
