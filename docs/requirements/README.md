# 需求文档索引

本目录存放可归档的需求规格（`requirement.md`）。由 skill **`requirements-doc`** 在用户明确说「形成需求文档」等触发语时维护；讨论需求时不自动落盘。

## 目录约定

```text
docs/requirements/
├── README.md                 # 本索引
├── _template/
│   └── requirement.md        # 新建时的章节模板
└── <分支>/<slug>/
    └── requirement.md
```

- `<分支>`：主业务仓当前 Git 分支名（多仓时先确认主仓）。
- `<slug>`：kebab-case 英文短名。
- 若项目另有 `sql/` 约定，DDL 脚本放在 `sql/<分支>/`，并在需求文档中相对链接。

## 索引表

| 分支 | slug | 标题 | 状态 | 文档 |
| --- | --- | --- | --- | --- |
| （暂无） | | | | |

落盘新文档时在上表追加一行。
