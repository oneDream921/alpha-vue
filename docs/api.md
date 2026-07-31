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
| 参数配置 | `GET/POST/PUT/DELETE /system/configs`、`GET/POST/PUT /system/configs/definitions` |
| 字典     | `/system/dict-types` CRUD、`/system/dict-types/{typeId}/items`、`/system/dict-items/{id}`、`GET /system/dicts/{typeCode}/items`、`PUT /system/dicts/cache` |
| 文件     | `GET /files`、`POST /files/upload`、`DELETE /files/{id}`          |
| 日志     | `GET /logs/operations`、`GET /logs/logins`、`PUT /logs/operations/{id}/handled` |
| Redis 管理 | `GET /monitor/redis/overview`、`GET /monitor/redis/keys`、`GET/DELETE /monitor/redis/key` |
| 在线用户 | `GET /monitor/online-users`、`DELETE /monitor/online-users/{userId}/sessions/{terminalIndex}` |
| SQL 监控 | `GET /monitor/sql/logs`、`DELETE /monitor/sql/logs`、`GET/PUT /monitor/sql/settings` |

文件上传支持 `txt`、`pdf`、`doc/docx`、`xls/xlsx` 与 `png/jpg/jpeg/gif/webp`。文件响应中的 `publicUrl` 表示当前可访问 URL：默认 `FILE_PUBLIC_ACCESS=false` 时返回短期 HMAC 签名的 `/api/files/{id}/content` 地址；仅显式启用公开访问时，本地存储返回 `/uploads/<uuid>.<ext>`，MinIO 返回配置的公开对象 URL。图片访问 URL 可直接用于预览和头像展示。

登录请求 `POST /auth/login` 必须包含 `username`、`password` 和已注册的 `clientId`。当前管理端固定使用 `clientId=pc-admin`；可选的 `deviceId` 和 `deviceName` 仅用于会话展示与追踪，不作为密钥。缺失、格式非法、未知或禁用的客户端返回 HTTP 400，不会静默补默认客户端。

个人头像上传只接受 `png/jpg/jpeg/gif/webp`，使用当前登录用户身份，不要求文件管理权限；上传成功后立即更新该用户头像。修改个人密码时，旧密码错误会返回“旧密码错误”，新旧密码相同会返回“新密码不能与旧密码相同”。

HTTP 状态与响应 `code` 一致：参数错误 400、未登录 401、无权限 403、登录锁定 429、未处理错误 500。响应头 `X-Trace-Id` 与响应体 `traceId` 可用于问题定位。

参数配置的值只能使用已发布定义目录中的 `file.*` 业务键。`system:config:list` 可查看值与定义；`system:config:create`、`system:config:update`、`system:config:delete` 分别控制值的新增、修改和删除；`system:config:define` 控制定义创建、修改和发布。定义包含 `BOOLEAN`、`INTEGER`、`ENUM`、`STRING` 类型、默认值、范围或枚举、敏感标记、动态标记和状态。敏感定义的默认值和配置值在普通响应中均为隐藏状态。

定义为 `DATA_ONLY` 时仅保存受控业务数据，不自动绑定应用行为。动态定义只允许使用已实现的文件运行时绑定：单文件上传大小、允许扩展名和私有文件访问期限；其定义必须保持已发布。值写入在事务提交后才发布或失效 Redis 缓存；禁用值会回退到定义默认值。未注册、未发布、类型或范围不合法的值一律返回 400“请求参数错误”，不会返回配置值、数据库错误或内部异常信息。

Redis 管理接受可选前缀筛选、键名关键词与 `SCAN` 游标（`cursor`、`count=1..100`），空前缀表示查询全库键空间。接口返回键名、分类、类型、TTL、大小估计和值预览；超出展示上限时通过 `valueTruncated` 标记。删除接口仅返回确认文本，不回显键名。

在线用户按 Sa-Token 受控会话索引分页返回，每行对应一个登录终端，包含账号、部门、clientId、设备、IP、浏览器、操作系统、登录时间、最后访问时间和不可逆 token 摘要。查询页大小限制为 100；定向下线需要 `monitor:online:kickout` 权限，只影响指定用户的指定终端。

SQL 监控返回当前进程内最近 SQL 摘要，支持 `limit=1..200`、`type=SELECT|INSERT|UPDATE|DELETE|UNKNOWN`、`keyword` 和 `slowOnly` 筛选。SQL 文本只保留 MyBatis 占位符，清除注释和字面量并限制单条摘要长度，不渲染真实参数值；摘要按固定容量和保留时间自动清理。清空接口仅清空内存队列，不影响数据库或审计日志。采集设置接口返回当前全局采集状态、已发现的 MyBatis statement 和排除列表；更新设置需要 `monitor:sql:control` 权限，重启后恢复默认“开启 + 全部记录”。连接池运行指标通过受控的 `/actuator/prometheus` 观测。

日志响应包含账号、结果、IP、地点、clientId、设备摘要、浏览器、操作系统和 traceId；操作日志另包含响应状态、耗时、业务错误码和有界异常摘要。日志不返回请求参数、请求体、响应体或认证敏感信息。

开发环境启动后可访问 `/swagger-ui/index.html` 查看 OpenAPI 页面，也可按分组访问 `/v3/api-docs/{group}`（`auth`、`system`、`file`、`log`、`monitor`）。生产环境默认关闭 SpringDoc API 文档与 Swagger UI；如需临时诊断，应通过受控的运维变更并由网关限制访问，而不是直接公开接口文档。
