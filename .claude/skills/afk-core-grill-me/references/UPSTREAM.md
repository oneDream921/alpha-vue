# Upstream 来源与许可

## 来源

- 仓库：https://github.com/mattpocock/skills
- 固定版本：`2ab958093e83e0ec752e6c1c5932da465bf23e0c`
- 原始文件：`skills/productivity/grill-me/SKILL.md`
- 原始文件：`skills/productivity/grilling/SKILL.md`
- 引入日期：2026-07-30

## AFK 本地改造

- 将原先的入口 Skill 与逐题工作流合并为一个 `grill-me`，避免依赖跨平台 Command。
- 保留逐题决策树、每题给出推荐、事实自行核实、用户确认后再行动等核心设计。
- 增加 AFK 的探索状态、动态方向格式、共同理解确认门和需求探索返回约定。
- 本地版本随 AFK Core 投影，可继续独立演进，不在运行时依赖上游仓库。

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
