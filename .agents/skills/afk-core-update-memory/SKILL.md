---
name: update-memory
description: 更新项目记忆 docs/ai（或 agent.yaml 中 customization.memory）。
  当用户说「形成记忆」「更新记忆」「记录下来」「写入记忆」「整理记忆」「补充记忆」 或要求持久化业务/实现认知时使用。用户未明确触发时不得修改记忆目录。
---

# 更新项目记忆

**索引**：记忆根目录下的 `README.md`（只改导航/登记，不写长文）  
**分层**：`domains/` · `services/<名>/` · `contracts/`

## 落盘前三问（必须）

1. 去掉类名、端口、构建命令后仍成立？→ **`domains/<domain>.md`**  
2. 两仓及以上或前后端边界线上的格式？→ **`contracts/<a>-<b>-<topic>.md`**  
3. 单仓构建、启动、包结构、类落点、本仓排查？→ **`services/<名>/overview.md` 或 `local-dev.md`**

答不上来时先问用户落哪一层，不准猜测。

## 执行步骤

1. Read 索引 `README.md`，确定层与目标路径。  
2. Read 目标文件；无则按对应 `_template*.md` 懒创建。  
3. 只写入**已确认**条目；争议未裁定的不写。  
4. 增量更新目标文件；与代码冲突处以代码为准。  
5. 新建文档时在 `README.md` 域注册表或导航中加一行链接。  
6. 自检后列出改动路径。

## 自检

- [ ] domains 中无 `.java` / `src/main` / 包路径堆砌  
- [ ] 启动/构建命令只在 `services/<名>/local-dev.md`  
- [ ] 跨端字段在 contracts，并与 domains/services 互链  
- [ ] 未新建未约定的第四层目录  
- [ ] README 已登记  

## 边界

- 仅用户明确触发时落盘；讨论阶段不写记忆。  
- 若尚在核对「现网 / 代码 / 记忆是否一致」，先走 `align-memory`，确认后再触发本 skill。
- 不把业务事实写进 `.project-agent/rules` 或框架托管文件。
