# Alpha Vue 项目规则

本文件是 AFK 用户内容，由项目维护；生成的 `AGENTS.md`、`CLAUDE.md`、`.cursor/rules` 和 `afk-*` Skill 投影不得直接编辑。

## 工作区与边界

- 当前 workspace 是单一 Git 仓库：`alpha-web` 为 Vue 3 管理端，`alpha-server` 为 Spring Boot 服务，`deploy` 为本地部署与 smoke test，`docs` 为规范、设计和运维文档。
- 项目特有 Rule 和 Skill 只维护在 `.project-agent`；需要更新三端投影时运行 AFK `sync`，不得直接编辑生成文件或 AFK 框架源仓库。

## Git / 分支约定

- 生产分支：main
- 预发分支：无
- 测试分支：无
- 专题分支基线：main
- 合入目标分支：main
- 功能分支：codex/<slug>
- 修复分支：codex/fix-<slug>
- release 分支：无
- hotfix 分支：无
- 合入偏好：优先 PR
- `<slug>`：kebab-case 英文短名；阶段任务可沿用 `p<阶段>-<序号>-<主题>`，并尽量与需求文档 slug 一致。

## 规范入口

- 跨层编码与交付约束以 `docs/conventions.md` 为准。
- 前端任务必须使用 `alpha-vue-frontend` Skill，并完整读取 `docs/frontend-conventions.md`。
- 涉及系统边界、模块划分、技术选型、重大非功能需求或架构评审时，使用 `alpha-vue-architecture` Skill；只有用户明确要求时才落盘 ADR 或设计文档。
- 新增或变更前后端 API 契约时，使用 `alpha-vue-api-design` Skill，并以 `docs/api.md`、`docs/security.md` 和现有实现为准。
- 实现、重构、调试或评审 `alpha-server` 的 Spring Boot / Java 代码时，使用 `alpha-vue-backend` Skill；它不替代 `java-code-review` 的风险审查。
- 根据用户指定的 Alpha Vue 任务和已确认计划生成可复制的执行、评审、修复或状态同步提示词时，使用 `alpha-vue-generate-task-prompts` Skill。该 Skill 只生成提示词，不选择下一任务、不维护计划状态、不创建线程也不执行任务；它必须按任务范围判断是否使用 `alpha-vue-task-delivery-orchestrator` 及其他项目 Skill。需要连续交付编排时，由 orchestrator 在能力可用时自动完成强模型规划、快速实施、集中验证、可复用强模型评审、最多两轮批量修复、用户测试和状态同步；默认使用项目脚本启动、使用测试与 curl 验证，不执行浏览器自动化。
- 启动或重启本地项目时，使用 `alpha-vue-local-start` Skill。
- API、安全、开发和发布任务分别读取 `docs/api.md`、`docs/security.md`、`docs/development.md` 和 `docs/operations.md`。

## 工程约束

- 前端保持 Vue 3、严格 TypeScript、Ant Design Vue、Pinia、Vue Router、Axios、Vite 和 UnoCSS；页面不得直接调用 Axios，前端权限不替代后端授权。Ant Design Vue 负责业务控件，UnoCSS/CSS 仅负责布局、间距和响应式组合；UnoCSS reset 保持关闭。
- 后端按 `common`、`framework`、`modules/<domain>` 分层；Controller、Service、Mapper、Entity、DTO、VO 各守职责。Controller 默认继承 `framework.web.BaseController` 复用统一响应与请求上下文，但不得把业务 CRUD、实体暴露或持久化逻辑放进控制层基类。
- 数据库运行监控采用 Spring Boot BOM 默认 HikariCP、受控 Actuator/Micrometer 指标与应用内 SQL 日志页；SQL 日志只保留占位符 SQL 摘要，不记录真实参数值，不提供页面执行 SQL。SQL 采集开关和 Mapper 排除列表只作为进程内运行时排查能力。泄漏检测默认关闭，仅允许在受控诊断窗口临时开启。
- Flyway 迁移只追加，不修改已运行版本。

## 验证入口

- 前端：`pnpm --dir alpha-web typecheck`、`test`、`lint`、`format:check`、`build`。
- 后端：`./mvnw -f alpha-server/pom.xml test` 和 `package`。
- 部署配置：`docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet`；涉及真实联调时再运行 `deploy/smoke-test.sh`。
- 页面变更还需验收桌面、平板和手机视口。
