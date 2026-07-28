# frontend-backend-common · 通用 HTTP 契约

## 范围

- 协议：HTTP/JSON
- 调用方：`alpha-web`
- 被调方：`alpha-server`

## 字段与枚举

| 字段 | 类型/枚举 | 说明 |
|------|-----------|------|
| 请求路径 | `/api/...` | 所有业务接口的统一前缀 |
| `Authorization` | `Bearer <token>` | 受保护接口的认证头；不使用 Cookie 或 JWT |
| `code` | HTTP 对应状态码 | 统一响应中的业务状态 |
| `message` | string | 面向用户的公开消息 |
| `data` | 泛型对象 | 成功响应数据 |
| `traceId` | string | 与 `X-Trace-Id` 对应的问题定位标识 |
| `page`、`size` | number | 分页请求参数 |
| `records`、`total`、`page`、`size` | 分页对象字段 | 分页响应数据 |
| 权限码 | `domain:resource:action` | 菜单、前端按钮和后端授权使用同一语义 |

## 错误码与约定

| 码 | 含义 |
|----|------|
| 400 | 参数或业务前置条件不满足 |
| 401 | 未登录或会话失效；前端清理完整认证状态 |
| 403 | 已登录但无权限 |
| 429 | 登录锁定 |
| 500 | 未处理错误；不返回内部细节 |

## 相关文档

- 前端实现：[`../services/alpha-web/overview.md`](../services/alpha-web/overview.md)
- 后端实现：[`../services/alpha-server/overview.md`](../services/alpha-server/overview.md)
- 访问控制：[`../domains/auth-rbac.md`](../domains/auth-rbac.md)
- 权威 API 说明：`docs/api.md`
