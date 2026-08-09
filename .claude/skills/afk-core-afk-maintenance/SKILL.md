---
name: afk-maintenance
description: 接管、检查或维护已接入 Agent Framework Kit（AFK）的工作区：识别接入状态和所有权边界，
  诊断同步问题，在维护者本地源码演进与 npm 稳定分发模式之间选择正确入口。 用户要求检查/升级/修复 AFK、使用本地 AFK 更新托管项目，或新
  Agent 需要理解 AFK 接管方式时使用。
---

# AFK 托管维护

## 1. 识别状态

从工作区根检查以下文件，不要先扫描全部托管投影：

| 文件 | 含义 |
| --- | --- |
| `agent.yaml` | AFK 项目配置；单独存在时可能只完成了 `init`。 |
| `.agent-framework/manifest.json` | 当前锁定版本与托管路径的事实源。 |
| `.agent-framework/sync.mjs` | 接入完成后的项目本地日常同步入口。 |

- 三者齐全：视为已完整接入；读取 Manifest 的 `frameworkVersion` 与 `targets`。
- 只有 `agent.yaml`：说明尚未完成首次 `sync`，或状态不完整。
- Manifest 与本地脚本只缺其一：先报告不一致；仅在用户要求修复接入时使用 CLI 重建。
- 文件位置不明确时，先确定 workspace 根；多 Git workspace 不要把某个业务子仓误当成 AFK 根。

## 2. 遵守所有权

项目用户资产包括 `agent.yaml`、`.project-agent/`、`docs/ai/`、`docs/requirements/`，以及平台原生的 Agent / Subagent 配置。按用户目标修改，AFK 不应覆盖。

Manifest `targets` 是托管路径的权威清单。常见托管输出包括 `AGENTS.md`、`CLAUDE.md`、`.agents/skills/afk-*`、`.claude/skills/afk-*`、`.cursor/rules/afk-*`、`.cursor/skills/afk-*`、AFK Hook 与 `.agent-framework/sync.mjs`。不要直接编辑；应修改对应项目源或 AFK Core 后重新投影。

## 3. 选择入口

### npm 稳定分发模式

适用于跨设备、团队使用和已发布版本复现。修改 `agent.yaml`、`.project-agent/rules/` 或 `.project-agent/skills/` 后，使用项目锁定的已发布版本完成同一任务的同步闭环：

```bash
node .agent-framework/sync.mjs --dry-run
node .agent-framework/sync.mjs
```

这不是框架升级，不要自行改用 `@latest`。项目本地 `sync.mjs` 始终属于 npm 稳定分发模式；仅修改 `docs/ai/` 或 `docs/requirements/` 内容时无需为了投影而运行 sync。

只有用户明确要求升级已发布的 AFK 时，才使用私有 npm 包。先预览，再执行用户指定版本；未指定时才使用 `@latest`：

```bash
npx @onedream921/agent-framework-kit@latest upgrade --workspace . --dry-run
npx @onedream921/agent-framework-kit@latest upgrade --workspace .
```

缺少 Manifest 时不能运行 `upgrade`；用户要求首次接入或修复缺失状态时改用 `init` / `sync`。

### 维护者本地源码模式

这是维护者日常快速演进的正式方式，适用于高频迭代、真实项目试验和发布前验证。只有用户明确要求使用本地 AFK，或源码路径已由当前工作区上下文明确给出时才使用；不要广泛搜索或猜测路径。

先读取本地 AFK 的 `package.json` 版本、Git 状态和目标项目 Manifest。优先使用源码仓库的维护入口；它要求显式目标 workspace，只读取当前源码，不调用 npm。项目 Rule、Skill 或配置变化使用本地 `sync`；Core、Adapter 或框架能力变化使用本地 `upgrade`：

```bash
node /明确的/AFK/scripts/afk-local.mjs sync --workspace /目标/workspace --dry-run
node /明确的/AFK/scripts/afk-local.mjs upgrade --workspace /目标/workspace --dry-run
```

确认后才去掉 `--dry-run`。目标没有 Manifest 时，在用户要求首次接入或修复的前提下使用本地 `sync`。AFK 不自动扫描本地仓库，不写入项目级源码路径配置，也不建立来源优先级。该维护入口位于源码仓库的 `scripts/`，不会进入 npm 包。

本地源码运行会把其 `package.json` 版本写入目标 Manifest，但项目本地 `sync.mjs` 后续仍从 npm 获取该版本。使用本地未发布内容期间，继续使用同一本地 CLI，不要运行项目本地脚本。若本地源码含未发布改动，或无法证明它与 npm 同版本产物一致，必须明确说明当前项目处于本地源码模式；不得声称 npm 锁定版本已包含本地改动。准备跨设备或交给同事使用前，先发布完全一致的版本，或把项目升级回已发布版本。

## 4. 验证与停止条件

1. dry-run 出现 Conflict 时停止写入，报告冲突路径与所有权，不移动或覆盖用户文件。
2. 区分 Adapter Warning、项目提示与真实失败；不要为了消除平台不支持字段而篡改用户意图。
3. 实际执行后检查 Git diff，确认项目源与三端投影一致，没有业务子仓或平台原生资产被意外修改。
4. 不把 Registry Token 写入项目文件、命令输出或日志；认证失败时只报告需要用户级 npm 权限。
5. 汇报使用的 AFK 入口和版本、Create / Replace / Remove、警告、验证结果；无变化时明确说明托管投影未被重写。
