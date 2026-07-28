# Alpha Vue 文档导航

`docs/` 承载项目的正式规范、开发与运维文档。`docs/ai/` 是 Agent 的按需记忆索引，`docs/requirements/` 承载单项需求；两者都不替代本目录的正式规范。

## 正式规范

| 主题 | 文档 | 负责回答的问题 |
|------|------|----------------|
| 工程约定 | [`conventions.md`](./conventions.md) | 前后端、持久化、测试与交付应如何实现和评审？ |
| 前端规范 | [`frontend-conventions.md`](./frontend-conventions.md) | `alpha-web` 的目录、交互、权限、响应式和测试如何约束？ |
| API | [`api.md`](./api.md) | 路径、字段、认证、分页和 HTTP 行为是什么？ |
| 安全 | [`security.md`](./security.md) | 凭据、会话、授权、脱敏和运行边界如何保护？ |
| 开发 | [`development.md`](./development.md) | 本地环境、配置、日常命令和联调如何进行？ |
| 运维 | [`operations.md`](./operations.md) | 发布、生产运行、监控与故障处置如何进行？ |

## 其它文档

| 位置 | 职责 | 使用边界 |
|------|------|----------|
| [`ai/`](./ai/) | Agent 的领域、服务与契约记忆 | 只保留稳定摘要和导航；正式细节回链到本目录规范 |
| [`requirements/`](./requirements/) | 独立需求的范围、决策与验收 | 仅在明确形成需求文档时创建或更新 |
| [`releases/`](./releases/) | 已发布版本的记录 | 不承载当前工程规范 |

## 维护规则

- 每项规则只在一个正式文档中详细定义，其他文档只描述自身场景并链接到该来源。
- 接口字段与 HTTP 行为以 `api.md` 为准；安全策略以 `security.md` 为准；本地操作以 `development.md` 为准；生产运行和发布以 `operations.md` 为准。
- 代码与文档冲突时，以代码的实际行为为准，并更新对应的正式规范和必要的 Agent 记忆。
