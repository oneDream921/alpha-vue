# 项目记忆索引

> 与代码冲突时以代码为准。详细内容按下方导航按需阅读，不要整目录灌进上下文。
> 任何写入或更新都必须先获得用户明确批准（例如回复「形成记忆」）。

## 工作区结构

| 路径 | 说明 |
|------|------|
| `alpha-web/` | Vue 3 管理端 |
| `alpha-server/` | Spring Boot 单体服务 |
| `deploy/` | 本地部署和 smoke test 配置 |
| `docs/` | 项目规范、设计和运维文档 |

## 记忆导航

| 任务 | 先读 |
|------|------|
| 业务规则 / 状态 / 术语 | [`domains/`](./domains/) |
| 前端实现与结构调整 | [`services/alpha-web/overview.md`](./services/alpha-web/overview.md) |
| 后端实现与模块落点 | [`services/alpha-server/overview.md`](./services/alpha-server/overview.md) |
| 本地启动 / 构建 / 端口 | `services/<名>/local-dev.md` |
| 前后端联调 / 跨服务字段 | [`contracts/frontend-backend-common.md`](./contracts/frontend-backend-common.md) |

## 域注册表

| 域 | 文档 | 一句话 |
|----|------|--------|
| 访问控制 | [`domains/auth-rbac.md`](./domains/auth-rbac.md) | 登录会话、RBAC 与权限边界 |
| 文件存储 | [`domains/file-storage.md`](./domains/file-storage.md) | 文件元数据、访问与删除规则 |
| 参数配置 | [`domains/system-config.md`](./domains/system-config.md) | 可运营业务配置的持久化边界 |
| Redis 运维 | [`domains/redis-management.md`](./domains/redis-management.md) | Redis 键空间的受限查询与单键处置 |
| 数据字典 | [`domains/data-dictionary.md`](./domains/data-dictionary.md) | 可维护枚举类型与字典项 |

新建域或服务文档时，先在本表或工作区结构表登记一行，再创建文件。模板见各目录下的 `_template*.md`。

## 维护约定

- **domains/**：业务语义（WHAT/WHY）；禁止类路径、端口、构建命令
- **services/<名>/**：单仓实现与启动（HOW）；前端/后端各占一名
- **contracts/**：跨仓或前后端边界格式
- 落盘前三问：去掉类名/端口/命令还成立？→ domains；边界格式？→ contracts；单仓 HOW/启动？→ services
- 禁止在 `docs/ai/` 下自建未约定的第四层目录
