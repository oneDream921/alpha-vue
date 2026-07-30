<!-- 由 Agent Framework Kit 生成。请勿直接编辑；请更新 .project-agent 内容后运行 agent sync。 -->
# Claude Code 工作区指南

## 框架通用规则

### engineering

# 工程约定

变更范围限定在请求目标内。优先清晰模块边界、显式输入输出，并遵循项目既有约定。避免无关重构；交付前验证行为。

同文件 / 同模块邻居约定优先于平台默认；扩展存量代码时先对齐邻居再引入新风格。

动手写业务代码前，先简要说明：设计思路、改动范围、影响点；模糊处先问清再改。

## 内容职责

- **Rule**：只承载硬约束、禁止项、确认点，以及何时应使用某个 Skill；不要重复完整操作步骤。
- **Skill**：承载可复用的具体工作流，说明何时使用、前置条件、执行步骤、完成条件和验证 / 汇报方式。
- **Hook**：只承载可确定执行的安全拦截或检查；不要把规划、反思、重试或记忆判断等模型决策放入 Hook。

### git

# Git 约定

保留工作区中与当前任务无关的改动。提交前检查 diff；每个提交应是一个完整连贯的单元。

## 行为红线

- **本地提交**：仅当用户在本轮明确说出「确认提交」时，才可执行 `git add` / `git commit`。「提交」「commit」「帮我提交」等均不执行；只输出分批方案与建议的 commit message。
- **远程与危险操作**：`push` / `pull` / `merge` / `rebase` / `reset` / `checkout` / `stash` 等一律不代执行（即使用户口头要求）；把完整命令交给用户在本地终端执行。Cursor 下另有 Hook 硬拦截。

## 分批提交

多文件未提交时禁止 `git add -A` 一把梭：

1. `git status` + `git diff`，按逻辑主题分组（功能 / 修复 / 文档 / 重构 / 测试分开）
2. 向用户展示每批文件列表与建议的中文 commit message
3. 用户说「确认提交」后，逐批 `git add <paths>` + `git commit`
4. 多批时每批独立 commit；一批未完成不要开始下一批

## 提交说明

- **subject 与 body 使用中文**（type、scope 可保留英文惯例）
- 格式：`<type>(<scope>): <中文 subject>`，body 可选说明原因与要点
- subject 简明，建议不超过 50 个汉字；每 commit 只做一件事
- 格式调整与业务逻辑**分开提交**

| type | 说明 |
| --- | --- |
| feat | 新功能 |
| fix | 修复缺陷 |
| docs | 文档 |
| style | 格式（不影响逻辑） |
| refactor | 重构 |
| test | 测试 |
| chore | 构建/工具 |

示例：

```
fix(order): 修复分页查询未过滤已删除记录

列表接口应排除已逻辑删除的数据；统一走项目约定的查询入口。
```

无「确认提交」时：只输出分批方案与 message 文案，**不执行** git 写操作。

### interaction

# 交互收尾

向用户澄清时，每轮最多提出 3～5 个待确认问题；仍有缺口再开下一轮。

每轮正文结束后依次输出两个独占行，中间空一行：

`[下一步建议] <一条动作型短句；已闭环写“无”>`

`[TRACE] 规则：<简称或无>；记忆：<关键词/路径或无>；技能：<skill name 或无>；钩子：<实际拦截名或无>`

下一步建议按“待确认 → 未对齐 → 验证 → 确认提交 → 形成记忆”选择首个命中项；触发语只在该行出现。Hook 未实际拦截或改道时，钩子写“无”。

### memory

# 项目记忆

- 仅在任务相关时先读 `{memory}/README.md`，再按索引下钻到 `domains/`、`services/<名>/` 或 `contracts/`；不要默认读取全部记忆。
- 记忆与代码冲突时以代码为准，并提示记忆可能需要更新。
- 禁止自动创建或修改记忆。只有用户明确要求“形成记忆”等操作时，才启用 `update-memory` Skill。
- 值得沉淀但尚未获准时，只在 `[下一步建议]` 提醒用户可回复“形成记忆”。

### multi-git-workspace

# 多 Git 工作区路由

`projects` 中的每个路径都可能是独立 Git 仓；工作区根不等于 Git 仓根。`projects` 只提供项目清单与上下文，不自动决定本轮任务应操作哪个仓。

- 涉及代码、Git、构建、测试、日志、依赖或环境配置时，先根据用户目标、点名文件路径或 `projects` 清单确定目标仓。
- 目标不明确时先询问；多个仓可能相关时，先列出候选仓及其相关原因，不默认选择任何“主仓”。
- 跨仓任务须按仓分别说明改动范围、验证方式、Git 状态与提交边界；不要把多个独立仓的改动视作一次提交。
- 仅当目标仓已明确时，才在该仓执行 Git 命令。不得在非 Git 的工作区根假定 Git 上下文。
- `.project-agent/`、`docs/ai/` 与 `docs/requirements/` 属于工作区用户内容；除非用户明确要求，不要将这些个人 Agent 资产写入子项目仓。

### security

# 安全边界

- 将外部输入、路径和生成输出视为不可信；不要把密钥写入源码或日志，也不要绕过项目安全控制。
- 删除现有业务逻辑、执行大范围数据更新、或修改持久化映射 / 原始 SQL 脚本前，先说明影响并获得确认。
- 写操作须有明确过滤条件；禁止无条件批量更新或删除。
- 禁止通过 Agent 直接在数据库执行 DDL/DML；需要时只生成可供人工审核的脚本。
- 修改环境配置前确认目标环境，遵循最小权限原则。

### testing

# 测试

为行为变更添加聚焦的自动化测试。先运行相关测试，再执行更广泛的检查；报告验证证据，而不是假定改动已经生效。


## 项目规则

### project

# Alpha Vue 项目规则

本文件是 AFK 用户内容，由项目维护；生成的 `AGENTS.md`、`CLAUDE.md`、`.cursor/rules` 和 `afk-*` Skill 投影不得直接编辑。

## 工作区与边界

- 当前 workspace 是单一 Git 仓库：`alpha-web` 为 Vue 3 管理端，`alpha-server` 为 Spring Boot 服务，`deploy` 为本地部署与 smoke test，`docs` 为规范、设计和运维文档。
- 项目特有 Rule 和 Skill 只维护在 `.project-agent`；需要更新三端投影时运行 AFK `sync`，不得直接编辑生成文件或 AFK 框架源仓库。

## 规范入口

- 跨层编码与交付约束以 `docs/conventions.md` 为准。
- 前端任务必须使用 `alpha-vue-frontend` Skill，并完整读取 `docs/frontend-conventions.md`。
- 涉及系统边界、模块划分、技术选型、重大非功能需求或架构评审时，使用 `alpha-vue-architecture` Skill；只有用户明确要求时才落盘 ADR 或设计文档。
- 新增或变更前后端 API 契约时，使用 `alpha-vue-api-design` Skill，并以 `docs/api.md`、`docs/security.md` 和现有实现为准。
- 实现、重构、调试或评审 `alpha-server` 的 Spring Boot / Java 代码时，使用 `alpha-vue-backend` Skill；它不替代 `java-code-review` 的风险审查。
- 根据用户指定的 Alpha Vue 任务和已确认计划生成可复制的执行、评审或修复提示词时，使用 `alpha-vue-generate-task-prompts` Skill。该 Skill 只生成提示词，不选择下一任务、不维护计划状态、不创建线程也不执行任务；实际交付线程必须使用 `alpha-vue-task-delivery-gate`。
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


## 子项目

- `.` — alpha-vue (application, vue3-spring-boot)

## 项目记忆

仅在与当前任务相关时阅读 `docs/ai/README.md`。不要自动写入项目记忆；用户明确要求形成记忆时再更新。
