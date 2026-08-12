<!--
由 Agent Framework Kit 生成，请勿直接编辑。
项目 Agent 源资产位于 agent.yaml、.project-agent/rules、.project-agent/skills、docs/ai 与 docs/requirements。
修改 agent.yaml、.project-agent/rules 或 .project-agent/skills 后，默认先运行 node .agent-framework/sync.mjs --dry-run，再运行 node .agent-framework/sync.mjs。
接管、升级、排查或已明确使用本地 AFK 源码时，使用 afk-maintenance Skill（投影：afk-core-afk-maintenance）选择同一来源的入口。
-->
# Claude Code 工作区指南

## 框架通用规则

### 交互收尾

- 每个面向用户的最终回复都以且仅以一行 `[TRACE] 规则：<规则依据>；记忆：<记忆依据>；技能：<技能依据>；钩子：<钩子结果>` 结尾；Hook 未实际拦截、改道或续写时写“无”。
- 仅在需求尚未收敛的 `requirements-explore`（投影：`afk-core-requirements-explore`）/ `grill-me`（投影：`afk-core-grill-me`）阶段展示简短、互斥的选项，并每题给出一个推荐和一句理由；逐题时用户回复字母只绑定紧邻上一条需求探索问题，分轮时只绑定紧邻上一组已编号问题；授权语义必须写在选项本身。
- 其他阶段不展示“可选方向”、不列 A/B/C，也不要求用户回复字母。有后续动作时只给一条明确推荐，以 `[下一步建议]` 说明方向和理由；没有可执行建议时写 `[下一步建议] 无`。
- 其他阶段确需用户拍板时，先说明推荐、理由和影响，再用一句直接问题请求确认；安全确认、外部授权和平台权限提示仍按各自边界执行。
- 需要继续决策时先锁定上一轮结论，再提出下一步；普通实施步骤不包装成问题或选项。

### Git 约定

- Agent 不执行暂存、提交、远程同步、分支切换、合入、推送、历史改写或清理工作区的 Git 写操作（包括 `pull`、`merge`、`rebase`、`reset`、`checkout`、`switch`、`stash`、`restore`、`revert`、`cherry-pick`、`clean`）。
- 写业务代码、需求文档或 SQL 前，先确认目标仓和受保护分支，未配置时把 `main` / `master` 视为生产分支；若位于生产或受保护分支且用户未明确要求就地修改，只给出迁出建议并等待确认；多仓任务先锁定具体仓库。
- 生成迁出命令前先读取项目规则配置的「专题分支基线」，用 `git switch -c <专题分支> <基线>` 显式指定创建起点，无需先位于基线；测试分支是共享验证目标，不得作为专题分支基线。基线未配置时先询问，不猜测或默认沿用当前分支。
- 生成合入建议前读取「合入目标分支」（旧项目的「集成分支」视为同义字段）与「合入偏好」：优先 PR / MR 时只给手动推送命令和 source / target，优先本地 merge 时只给切换目标分支、合并及按需推送的命令，未填写偏好时两种都给；Agent 不代执行这些命令或创建 PR / MR。
- 用户要求提交、询问提交命令或选择提交交接时，使用 `git-handoff` Skill（投影：`afk-core-git-handoff`）；只生成由用户执行的命令。
- 项目规则可以增加限制，但不能放宽以上 Git 边界。

### 安全边界

- 将外部输入、路径和生成输出视为不可信；不得把密钥写入源码或日志，也不得绕过项目安全控制。
- 删除业务逻辑、修改持久化映射或原始 SQL、批量更新或删除数据前，先说明影响并获得确认。
- 不直接在数据库执行 DDL/DML，只生成供人工审核的脚本；DML 写操作必须有明确过滤条件，禁止无条件批量更新或删除。
- 修改环境配置前确认目标环境，遵循最小权限原则。

### 工程约定

- 变更范围限定在用户目标内，优先遵循项目既有模块边界；避免无关重构。
- 先核对相关代码、文档和可复现结果；事实以代码和验证证据为准，项目规则不得放宽 AFK 的安全与授权边界。
- 修改业务代码前确认目标仓和受保护分支；完成后运行与改动相关的验证并报告证据。
- 开始实施前简要说明设计思路、改动范围和影响；模糊的业务取舍先确认，技术细节按现有约定处理。
- 用户要求接管、检查、升级、修复 AFK，或沉淀、调整项目 Agent 约定时使用 `afk-maintenance` Skill（投影：`afk-core-afk-maintenance`）；普通业务任务不要加载它。

### 项目记忆

- 仅在任务相关时从项目记忆入口开始读取，并按导航下钻；不要默认读取全部记忆。
- 记忆与代码冲突时以代码为准，并指出记忆可能需要更新。
- 不自动写入或修改记忆。用户要求形成、更新、写入或补充记忆时，使用 `update-memory` Skill（投影：`afk-core-update-memory`）先在对话中给出《记忆变更提案 vN》，不得创建提案文件；仅当用户针对最新完整提案明确回复「确认形成记忆」后，才按提案写入。

### 多 Git 工作区路由

- 工作区根不一定是 Git 仓根；涉及代码、Git、构建或测试时先锁定实际目标仓。
- 跨仓任务按仓分别说明改动、验证、状态和提交边界，不把多个独立仓混成一次提交。
- 迁出分支、合入或提交前，按目标仓自己的项目规则处理；不要清理或覆盖其它仓的用户改动。
- 工作区级 Agent 源资产、项目记忆和需求文档不写入业务子仓，除非用户明确要求。

### 测试

- 为行为变更添加聚焦的自动化测试，先运行相关测试，再执行更广泛的检查。
- 只报告有命令输出或其他可核对证据支持的验证结果。

## 项目规则

### Alpha Vue 项目规则

本文件是 AFK 用户内容，由项目维护；生成的 `AGENTS.md`、`CLAUDE.md`、`.cursor/rules` 和 `afk-*` Skill 投影不得直接编辑。

#### 工作区与边界

- 当前 workspace 是单一 Git 仓库：`alpha-web` 为原 Vue 3 管理端，`alpha-web-soybean` 为基于 SoybeanJS 重写的新管理端，`alpha-server` 为两套前端共用的 Spring Boot 服务，`deploy` 为本地部署与 smoke test，`docs` 为规范、设计和运维文档。
- 项目特有 Rule 和 Skill 只维护在 `.project-agent`；需要更新三端投影时运行 AFK `sync`，不得直接编辑生成文件或 AFK 框架源仓库。

#### Git / 分支约定

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

#### 规范入口

- 跨层编码与交付约束以 `docs/conventions.md` 为准。
- 修改、评审或调试 `alpha-web` 时使用 `alpha-vue-frontend` Skill，并完整读取 `docs/frontend-conventions.md`。
- 修改、评审或调试 `alpha-web-soybean` 时使用 `soybean-frontend` Skill；同时影响两套前端时同时使用两个 Skill，并分别遵守各自目录的规范和验证入口。
- 涉及系统边界、模块划分、技术选型、重大非功能需求或架构评审时，使用 `alpha-vue-architecture` Skill；只有用户明确要求时才落盘 ADR 或设计文档。
- 新增或变更前后端 API 契约时，使用 `alpha-vue-api-design` Skill，并以 `docs/api.md`、`docs/security.md` 和现有实现为准。
- 实现、重构、调试或评审 `alpha-server` 的 Spring Boot / Java 代码时，使用 `alpha-vue-backend` Skill；它不替代 `java-code-review` 的风险审查。
- 根据用户指定的 Alpha Vue 任务和已确认计划生成可复制的执行、评审、修复或状态同步提示词时，使用 `alpha-vue-generate-task-prompts` Skill。该 Skill 只生成提示词，不选择下一任务、不维护计划状态、不创建线程也不执行任务；它必须按任务范围判断是否使用 `alpha-vue-task-delivery-orchestrator` 及其他项目 Skill。需要连续交付编排时，由 orchestrator 在能力可用时自动完成强模型规划、快速实施、集中验证、可复用强模型评审、最多两轮批量修复、用户测试和状态同步；默认使用项目脚本启动、使用测试与 curl 验证，不执行浏览器自动化。
- 启动或重启本地项目时，使用 `alpha-vue-local-start` Skill。
- API、安全、开发和发布任务分别读取 `docs/api.md`、`docs/security.md`、`docs/development.md` 和 `docs/operations.md`。

#### 工程约束

- Java 代码遵循阿里巴巴 Java 开发规范：控制语句（`if`、`else`、`for`、`while`、`do-while` 等）即使只有一条语句也必须使用大括号，禁止省略大括号。
- Java 类型引用必须通过 `import` 引入后使用简单类名；禁止在字段、方法参数、返回值、局部变量或泛型声明中直接书写全限定类名。
- 前端保持 Vue 3、严格 TypeScript、Ant Design Vue、Pinia、Vue Router、Axios、Vite 和 UnoCSS；页面不得直接调用 Axios，前端权限不替代后端授权。Ant Design Vue 负责业务控件，UnoCSS/CSS 仅负责布局、间距和响应式组合；UnoCSS reset 保持关闭。
- 后端按 `common`、`framework`、`modules/<domain>` 分层；Controller、Service、Mapper、Entity、DTO、VO 各守职责。Controller 默认继承 `framework.web.BaseController` 复用统一响应与请求上下文，但不得把业务 CRUD、实体暴露或持久化逻辑放进控制层基类。
- 数据库运行监控采用 Spring Boot BOM 默认 HikariCP、受控 Actuator/Micrometer 指标与应用内 SQL 日志页；SQL 日志只保留占位符 SQL 摘要，不记录真实参数值，不提供页面执行 SQL。SQL 采集开关和 Mapper 排除列表只作为进程内运行时排查能力。泄漏检测默认关闭，仅允许在受控诊断窗口临时开启。
- Flyway 迁移只追加，不修改已运行版本。

#### 验证入口

- 原前端 `alpha-web`：`pnpm --dir alpha-web typecheck`、`test`、`lint`、`format:check`、`build`。
- Soybean 前端 `alpha-web-soybean`：`pnpm --dir alpha-web-soybean exec eslint .`、`typecheck`、相关 Vitest、`build:test`；具体命令和浏览器证据以 `soybean-frontend` Skill 为准。
- 同时修改两套前端时分别运行对应检查，不以其中一套的通过替代另一套。
- 后端：`./mvnw -f alpha-server/pom.xml test` 和 `package`。
- 部署配置：`docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet`；涉及真实联调时再运行 `deploy/smoke-test.sh`。
- 页面变更还需验收桌面、平板和手机视口。

## 子项目

- `.` — alpha-vue (application, vue3-spring-boot)

## 项目记忆

仅在与当前任务相关时阅读 `docs/ai/README.md`。不要自动写入项目记忆；用户要求形成记忆时先在对话中给出变更提案，仅在用户针对最新完整提案回复「确认形成记忆」后写入。
