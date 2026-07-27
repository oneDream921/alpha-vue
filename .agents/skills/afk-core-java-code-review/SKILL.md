---
name: java-code-review
description: Review Java changes for security, correctness, data, and regression
  risks. Default output is High/Medium risk points only. Use when the user says
  「帮我 review」「代码评审」「review 这次改动」「看看有没有漏洞/风险」, or asks to review Java code /
  inspect changes before merge. Optional convention check only when the user
  explicitly asks.
---

# Java Code Review

默认：**只报风险点**（High / Medium）。不默认巡检注释规范，不默认改代码。

## Scope

- **默认只读**；仅用户明确说「帮我修」「review 并修复」等时才改。改代码前须说明设计思路、改动范围、影响点；涉及 SQL/Mapper、删除逻辑或大范围数据变更时仍先确认。

## Workflow

### ① 锁定改动范围（必做）

产出「本轮审查清单」，只围绕清单 + 必要调用方：

1. 用户点名的文件 / 目录 / PR / commit 优先  
2. 否则在相关 Git 仓根执行 `git status` + `git diff`（含 staged / unstaged；用户说「含未跟踪」再看 untracked）  
3. 多仓 workspace：无法确定仓根时先问用户；多仓均有改动且未指明时先列摘要再问审哪几个  
4. 无 git 改动且用户未指定 → 先问审什么，不要猜全仓  

### ② 上下文（记忆可降级）

1. **尝试**读记忆索引（默认 `docs/ai/README.md`）  
2. 可读时按改动域按需下钻；不要默认读全套  
3. 读失败 / 不存在 → **转纯代码审查**；TRACE 记忆写 `无`；必要时仅在 `[下一步建议]` 提示「形成记忆」  
4. 记忆与代码冲突 → **以代码为准**  

记忆是增强信号，不是前置门禁。

### ③ 风险审查

| 类 | 关注 |
|----|------|
| 安全 | 注入、越权、敏感信息、不可信输入（安全相关宁可 Medium，勿标 Low 丢掉） |
| 正确性 | 分支 / 空值 / 状态 / 边界缺陷 |
| 数据 | SQL/Mapper、批量更新删除、逻辑删除遗漏 |
| 回归 | 明显破坏既有调用 / 契约 |

只输出 **High / Medium**。默认不做 JavaDoc/风格 nit；用户明确要求「顺便查规范」时再单独成节，并对照项目 `.project-agent/rules`。

### ④ 输出

```markdown
## 风险点

审查范围：`path1`, `path2`, …（N 个文件）

### High
- `path:line` — 一句话风险
  > 可选：一句改法

### Medium
- `path:line` — 一句话风险
```

无 High/Medium 时：`本轮改动未发现 High/Medium 风险点。审查范围：N 个文件。`

正文结束后仍输出 `[下一步建议]` + `[TRACE]`；技能字段填 `java-code-review`。
