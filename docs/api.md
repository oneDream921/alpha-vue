# API 约定

所有业务接口以 `/api` 开头，统一返回：

```json
{ "code": 200, "message": "ok", "data": {}, "traceId": "..." }
```

分页请求使用 `page`、`size`，`data` 包含 `records`、`total`、`page`、`size`。受保护接口使用 `Authorization: Bearer <token>`，不使用 Cookie 或 JWT。

| 范围     | 接口                                                              |
| -------- | ----------------------------------------------------------------- |
| 认证     | `GET /auth/captcha`、`POST /auth/login`、`POST /auth/logout`      |
| 当前用户 | `GET/PUT /auth/profile`、`POST /auth/avatar`、`PUT /auth/password`、`GET /auth/routes` |
| 用户     | `/system/users` CRUD、`PUT /{id}/roles`、`PUT /{id}/kickout`、`PUT /{id}/password` |
| 角色     | `/system/roles` CRUD、`GET/PUT /{id}/menus`                       |
| 菜单     | `/system/menus` CRUD                                              |
| 部门     | `/system/depts` CRUD                                              |
| 文件     | `GET /files`、`POST /files/upload`、`DELETE /files/{id}`          |
| 日志     | `GET /logs/operations`、`GET /logs/logins`、`PUT /logs/operations/{id}/handled` |
| Redis 管理 | `GET /monitor/redis/overview`、`GET /monitor/redis/keys`、`GET/DELETE /monitor/redis/key` |

文件上传支持 `txt`、`pdf`、`doc/docx`、`xls/xlsx` 与 `png/jpg/jpeg/gif/webp`。本地存储返回 `/uploads/<uuid>.<ext>`，MinIO 返回配置的公开对象 URL；图片 URL 可直接用于预览和头像展示。

个人头像上传只接受 `png/jpg/jpeg/gif/webp`，使用当前登录用户身份，不要求文件管理权限；上传成功后立即更新该用户头像。修改个人密码时，旧密码错误会返回“旧密码错误”，新旧密码相同会返回“新密码不能与旧密码相同”。

HTTP 状态与响应 `code` 一致：参数错误 400、未登录 401、无权限 403、登录锁定 429、未处理错误 500。响应头 `X-Trace-Id` 与响应体 `traceId` 可用于问题定位。

Redis 管理仅接受部署配置的受控前缀与 `SCAN` 游标（`cursor`、`count=1..100`）。接口只返回键名、分类、类型、TTL 与大小估计，不返回 Redis 值；删除接口仅返回确认文本，不回显键名。

开发环境启动后可访问 `/doc.html`（Knife4j）查看 OpenAPI 页面。生产环境默认关闭 OpenAPI 与 Knife4j；如需临时诊断，应通过受控的运维变更并由网关限制访问，而不是直接公开接口文档。
