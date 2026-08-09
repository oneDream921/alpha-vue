# 需求文档索引

本目录按需求主题保存轻量需求包。用户明确要求落盘需求基线时，需求包默认只有 `requirement.md`；技术方案和正式测试验收文档按需形成，不是必经流程。Plan 默认留在对话中并包含可验证的实施任务，不单独创建 `plan.md` 或 `task.md`；需要跨会话实施或交接时，可在 `design.md` 按需记录稳定的实施顺序。

## 目录约定

```text
docs/requirements/
├── README.md
├── _template/
│   ├── requirement.md
│   ├── design.md
│   └── acceptance.md
└── <分支>/<slug>/
    ├── requirement.md       # 默认需求基线
    ├── design.md            # 可选：技术方案
    └── acceptance.md        # 可选：测试验收
```

- `<分支>`：主业务仓当前 Git 分支名（多仓时先确认主仓）。
- `<slug>`：kebab-case 英文短名。
- `requirement.md` 是“做什么”的唯一依据；`design.md` 和 `acceptance.md` 不得扩大需求范围。
- `design.md` 只记录“怎么做”的稳定结论；实施顺序按需保留，不维护负责人或完成状态。
- `acceptance.md` 用于需要独立执行、交接或留痕的完整验收；普通需求直接使用 `requirement.md` 的验收标准和 Plan 的验证方式。
- 形成任何文档都不自动授权规划、实现或测试；同一轮明确要求组合动作时可以连续完成。
- 三个文档互不自动触发。用户明确要求形成对应文档后才落盘。
- 详细 DDL 如需落盘，优先遵循项目 SQL 目录约定；项目未配置时使用 `scripts/sql/<分支>/<slug>.sql`，并从 `design.md` 或相关文档链接。

## 索引表

| 分支 | slug | 标题 | 状态 | 文档 |
| --- | --- | --- | --- | --- |
| `main` | `alpha-framework-modernization` | Alpha Vue 框架现代化与分阶段能力建设 | P1-05 已合并，G1 剩余 Redis/缓存/会话及阶段放行确认项 | [requirement.md](./main/alpha-framework-modernization/requirement.md) |

落盘新的 `requirement.md` 时在上表追加一行。只有 `design.md` 或 `acceptance.md`、尚未形成主需求文档时，不创建虚假的索引项。
