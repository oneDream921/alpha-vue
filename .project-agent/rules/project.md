# Alpha Vue 项目规则

本文件是 AFK 用户内容，由项目维护；生成的 `AGENTS.md`、`CLAUDE.md`、`.cursor/rules` 和 `afk-*` Skill 投影不得直接编辑。

## 工作区与边界

- 当前 workspace 是单一 Git 仓库：`alpha-web` 为 Vue 3 管理端，`alpha-server` 为 Spring Boot 服务，`deploy` 为本地部署与 smoke test，`docs` 为规范、设计和运维文档。
- 项目特有 Rule 和 Skill 只维护在 `.project-agent`；需要更新三端投影时运行 AFK `sync`，不得直接编辑生成文件或 AFK 框架源仓库。

## 规范入口

- 跨层编码与交付约束以 `docs/conventions.md` 为准。
- 前端任务必须使用 `alpha-vue-frontend` Skill，并完整读取 `docs/frontend-conventions.md`。
- API、安全、开发和发布任务分别读取 `docs/api.md`、`docs/security.md`、`docs/development.md` 和 `docs/operations.md`。

## 工程约束

- 前端保持 Vue 3、严格 TypeScript、Ant Design Vue、Pinia、Vue Router、Axios、Vite 和 Tailwind CSS；页面不得直接调用 Axios，前端权限不替代后端授权。
- 后端按 `common`、`framework`、`modules/<domain>` 分层；Controller、Service、Mapper、Entity、DTO、VO 各守职责。
- Flyway 迁移只追加，不修改已运行版本。

## 验证入口

- 前端：`pnpm --dir alpha-web typecheck`、`test`、`lint`、`format:check`、`build`。
- 后端：`./mvnw -f alpha-server/pom.xml test` 和 `package`。
- 部署配置：`docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet`；涉及真实联调时再运行 `deploy/smoke-test.sh`。
- 页面变更还需验收桌面、平板和手机视口。
