# Upstream 来源与许可

## 来源

- 仓库：https://github.com/mattpocock/skills
- 固定版本：`2ab958093e83e0ec752e6c1c5932da465bf23e0c`
- 原始文件：`skills/productivity/grill-me/SKILL.md`
- 原始文件：`skills/productivity/grilling/SKILL.md`
- 引入日期：2026-07-30
- 对照说明更新日期：2026-08-12

## AFK 本地改造

- 将原先的入口 Skill 与逐题工作流合并为一个 `grill-me`，避免依赖跨平台 Command；不拆独立 `grilling` Skill。
- 保留决策依赖树、每题给出推荐、事实自行核实、用户确认后再行动等核心设计。
- 借鉴上游后续的 frontier / 分轮思想：在本 Skill 中固化 frontier 选人规则；本 Skill 默认仍一次只问一个；`requirements-explore` 的分轮模式复用同一规则，每轮最多 3～5 题。
- 增加 AFK 的探索状态、共同理解确认门和需求探索返回约定。
- 决策题用 `Qn` 标题 + 短 `A/B/C…`（`短结论：短方向`）+ `[下一步建议] … 推荐理由`，与全局交互协议一致；不采用上游 ❓/➡️ 题面，也不自动写入 CONTEXT.md / ADR。
- 开场「目标」继承用户本轮真实需求，不把特定业务口径写成 Skill 固定内容；后续题先展示 `Qn 锁定` 再问下一项。
- 本地版本随 AFK Core 投影，可继续独立演进，不在运行时依赖上游仓库。

## 与上游的分叉点

- 上游现将追问算法放在 model-invoked `grilling`，`grill-me` 仅为薄入口；AFK 保持合并后的单一 Skill。
- 上游 grilling 默认每轮问整条 frontier；AFK 默认单题，分轮仅作为需求探索的显式选项，且设 3～5 题上限。
- AFK 不引入 `grill-with-docs`、`domain-modeling`、`ask-matt`、`wayfinder` 等相邻 Skill。

## MIT License

Copyright (c) 2026 Matt Pocock

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
