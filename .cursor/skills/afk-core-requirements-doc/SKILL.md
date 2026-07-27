---
name: requirements-doc
description: 仅在用户明确表示要形成/编写需求文档时，按 docs/requirements 约定创建或更新 requirement.md
  并维护索引。 未使用触发语时不得创建或修改 docs/requirements/。落盘后默认不得自动编码。
---

# 形成需求文档

## 路径约定

| 产物 | 位置 |
| --- | --- |
| 需求文档 | `docs/requirements/<分支>/<slug>/requirement.md` |
| 文档模板 | `docs/requirements/_template/requirement.md`（若有） |
| 索引 | `docs/requirements/README.md` |
| SQL（若项目有 `sql/` 约定） | `sql/<分支>/<slug>.sql` |

**分支归档名**：在主业务 Git 仓目录执行 `git rev-parse --abbrev-ref HEAD`。多仓 workspace 先问用户哪个是主仓，**不得**猜测。Detached HEAD 时与用户约定归档名，或用 `_detached/`。`<slug>` 为 kebab-case 英文短名。

## 何时启用（强制）

**视为触发**：形成需求文档、编写需求文档、写需求文档、把需求记到 docs/requirements、按需求目录规范建文档。

**不视为触发**：仅讨论需求、改代码、评审、「整理一下」但未指明需求目录。不确定时先确认是否落盘。

## 落盘后禁止自动编码（强制）

完成 `requirement.md`、（若有）SQL 与索引更新后，**默认结束**，不得接续改源码。
例外：用户在**同一轮**明确写了「实现」「编码」「按文档开发」等。

## 文档结构

不要单独「元信息」章；分支/单号并入背景一两句；状态放文末勾选。

必须包含：

```markdown
## 背景
## 目标与范围
### 目标
### 非目标（本期不做）
## 数据与接口（含库表 / DDL）
## 实现要点（可选）
## 风险与待办
## 测试与验收
### 人工验收
| 场景 | 怎么验收 | 预期 | 结果 |
（结果列留空，由开发者填写）
## 状态（维护人自填）
- [ ] 需求已确认
- [ ] 联调完成
- [ ] 人工验收通过
```

**DDL / SQL**：若项目有 `sql/`，可执行 SQL 落 `.sql` 文件，正文用相对链接；无则在正文代码块写 DDL，并注明待人工审核后执行。

**人工验收**：结果列留空；禁止 Agent 代填「通过」或勾选文末状态。

## 从对齐稿映射

| 对齐稿 | requirement.md |
| --- | --- |
| 主题 + 现状 + 痛点 | 背景 |
| 目标 | 目标 |
| 非目标 | 非目标 |
| 风险与约束 | 风险与待办 |
| 人工验收草案 | 人工验收表 |
| Plan 文件清单（若有） | 实现要点 |

## 执行步骤

1. Read `docs/requirements/README.md`；若有 `_template/requirement.md` 作起点。
2. 确定分支归档名与 slug；新建 `docs/requirements/<分支>/<slug>/`。
3. 填写 `requirement.md`；人工验收结果列留空。
4. 若含 DDL 且项目有 `sql/` 约定：写脚本并互链。
5. 更新 `docs/requirements/README.md` 索引。
6. 提示：「请先复核需求文档；确认后再继续实现。」

## 不要做

- 未触发时不写 `docs/requirements/**`。
- 落盘后用户未明示编码时不自动实现。
- 不代填验收结果或状态勾选。
- 不因模板升级批量改写历史文档。
