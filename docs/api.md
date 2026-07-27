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
| SQL 监控 | `GET /monitor/sql/logs`、`DELETE /monitor/sql/logs`、`GET/PUT /monitor/sql/settings`、`GET /monitor/sql/druid-url` |

文件上传支持 `txt`、`pdf`、`doc/docx`、`xls/xlsx` 与 `png/jpg/jpeg/gif/webp`。文件响应中的 `publicUrl` 表示当前可访问 URL：默认 `FILE_PUBLIC_ACCESS=false` 时返回短期 HMAC 签名的 `/api/files/{id}/content` 地址；仅显式启用公开访问时，本地存储返回 `/uploads/<uuid>.<ext>`，MinIO 返回配置的公开对象 URL。图片访问 URL 可直接用于预览和头像展示。

个人头像上传只接受 `png/jpg/jpeg/gif/webp`，使用当前登录用户身份，不要求文件管理权限；上传成功后立即更新该用户头像。修改个人密码时，旧密码错误会返回“旧密码错误”，新旧密码相同会返回“新密码不能与旧密码相同”。

HTTP 状态与响应 `code` 一致：参数错误 400、未登录 401、无权限 403、登录锁定 429、未处理错误 500。响应头 `X-Trace-Id` 与响应体 `traceId` 可用于问题定位。

Redis 管理接受可选前缀筛选、键名关键词与 `SCAN` 游标（`cursor`、`count=1..100`），空前缀表示查询全库键空间。接口返回键名、分类、类型、TTL、大小估计和值预览；超出展示上限时通过 `valueTruncated` 标记。删除接口仅返回确认文本，不回显键名。

SQL 监控返回当前进程内最近 SQL 摘要，支持 `limit=1..200`、`type=SELECT|INSERT|UPDATE|DELETE|UNKNOWN`、`keyword` 和 `slowOnly` 筛选。SQL 文本只保留 MyBatis 占位符，不渲染真实参数值；清空接口仅清空内存队列，不影响数据库、审计日志或 Druid 指标。采集设置接口返回当前全局采集状态、已发现的 MyBatis statement 和排除列表；更新设置需要 `monitor:sql:control` 权限，重启后恢复默认“开启 + 全部记录”。Druid 入口接口只返回当前配置的监控路径和启用状态，访问 `/druid/**` 使用 Druid 自身登录保护。

开发环境启动后可访问 `/doc.html`（Knife4j）查看 OpenAPI 页面。生产环境默认关闭 OpenAPI 与 Knife4j；如需临时诊断，应通过受控的运维变更并由网关限制访问，而不是直接公开接口文档。
