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
| 日志     | `GET /logs/operations`、`GET /logs/operations/{id}`、`GET /logs/logins`、`PUT /logs/operations/{id}/handled` |
| Redis 管理 | `GET /monitor/redis/overview`、`GET /monitor/redis/metrics`、`GET /monitor/redis/keys`、`GET/DELETE /monitor/redis/key` |
| 在线用户 | `GET /monitor/online-users`、`DELETE /monitor/online-users/{userId}/sessions/{terminalIndex}` |
| SQL 监控 | `GET /monitor/sql/logs`、`DELETE /monitor/sql/logs`、`GET/PUT /monitor/sql/settings` |

文件上传支持 `txt`、`pdf`、`doc/docx`、`xls/xlsx` 与 `png/jpg/jpeg/gif/webp`。文件响应中的 `publicUrl` 表示当前可访问 URL：默认 `FILE_PUBLIC_ACCESS=false` 时返回短期 HMAC 签名的 `/api/files/{id}/content` 地址；仅显式启用公开访问时，本地存储返回 `/uploads/<uuid>.<ext>`，MinIO 返回配置的公开对象 URL。图片访问 URL 可直接用于预览和头像展示。

登录请求 `POST /auth/login` 必须包含 `username`、`password` 和已注册的 `clientId`。当前管理端固定使用 `clientId=pc-admin`；可选的 `deviceId` 和 `deviceName` 仅用于会话展示与追踪，不作为密钥。缺失、格式非法、未知或禁用的客户端返回 HTTP 400，不会静默补默认客户端。

个人头像上传只接受 `png/jpg/jpeg/gif/webp`，使用当前登录用户身份，不要求文件管理权限；上传成功后立即更新该用户头像。修改个人密码时，旧密码错误会返回“旧密码错误”，新旧密码相同会返回“新密码不能与旧密码相同”。

HTTP 状态与响应 `code` 一致：参数错误 400、未登录 401、无权限 403、登录锁定 429、未处理错误 500。响应头 `X-Trace-Id` 与响应体 `traceId` 可用于问题定位。

参数配置的值只能使用已发布定义目录中的 `file.*` 业务键。`system:config:list` 可查看值与定义；`system:config:create`、`system:config:update`、`system:config:delete` 分别控制值的新增、修改和删除；`system:config:define` 控制定义创建、修改和发布。定义包含 `BOOLEAN`、`INTEGER`、`ENUM`、`STRING` 类型、默认值、范围或枚举、敏感标记、动态标记和状态。敏感定义的默认值和配置值在普通响应中均为隐藏状态。

定义为 `DATA_ONLY` 时仅保存受控业务数据，不自动绑定应用行为。动态定义只允许使用已实现的文件运行时绑定：单文件上传大小、允许扩展名和私有文件访问期限；其定义必须保持已发布。值写入在事务提交后才发布或失效 Redis 缓存；禁用值会回退到定义默认值。未注册、未发布、类型或范围不合法的值一律返回 400“请求参数错误”，不会返回配置值、数据库错误或内部异常信息。

Redis 管理接受可选前缀筛选、键名关键词与 `SCAN` 游标（`cursor`、`count=1..100`），空前缀表示查询全库键空间。接口返回键名、分类、类型、TTL、大小估计、展示级别和值预览；展示级别为 `HIDDEN`、`MASKED` 或 `PLAIN`，已注册的缓存定义（包括验证码、失败计数和 Sa-Token 会话）可通过参数配置调整，默认级别仍为 `HIDDEN`。列表值使用省略号布局，允许展示的值可通过悬停查看当前预览；Sa-Token 会话值是 Kryo 序列化二进制，设置为 `PLAIN` 时不保证可读，可能显示乱码；未注册且命中密钥特征的键始终为 `HIDDEN`。删除接口仅返回确认文本，不回显键名。

`GET /monitor/redis/metrics` 需要 `monitor:redis:list` 权限，返回当前应用实例的采样状态、最后一次成功的内存和连接快照、按累计调用数排序的最多 10 条命令统计，以及最多 1,440 个趋势点。内存快照只返回白名单数值字段，包括 `usedMemoryBytes`、`usedMemoryRssBytes`、`usedMemoryPeakBytes`、`maxMemoryBytes` 和用于无 `maxmemory` 场景计算仪表盘比例的 `totalSystemMemoryBytes`。采样状态为 `DISABLED`、`COLLECTING`、`HEALTHY`、`DEGRADED` 或 `STALE`；关闭、首次采样或采样失败仍返回 200 和明确状态。接口不返回 Redis 地址、凭据、原始 INFO、键值或命令参数。

在线用户按 Sa-Token 受控会话索引分页返回，每行对应一个登录终端，包含账号、部门、clientId、设备、IP、浏览器、操作系统、登录时间、最后访问时间和不可逆 token 摘要。查询页大小限制为 100；定向下线需要 `monitor:online:kickout` 权限，只影响指定用户的指定终端。

SQL 监控返回当前进程内最近 SQL 摘要，支持 `limit=1..200`、`type=SELECT|INSERT|UPDATE|DELETE|UNKNOWN`、`keyword` 和 `slowOnly` 筛选。SQL 文本只保留 MyBatis 占位符，清除注释和字面量并限制单条摘要长度，不渲染真实参数值；摘要按固定容量和保留时间自动清理。清空接口仅清空内存队列，不影响数据库或审计日志。采集设置接口返回当前全局采集状态、已发现的 MyBatis statement 和排除列表；更新设置需要 `monitor:sql:control` 权限，重启后恢复默认“开启 + 全部记录”。连接池运行指标通过受控的 `/actuator/prometheus` 观测。

日志列表响应包含账号、结果、IP、地点、clientId、设备摘要、浏览器、操作系统和 traceId；操作日志另包含响应状态、耗时和业务错误码。操作日志详情需要 `log:operation:detail` 权限，才返回有界异常摘要、默认采集且可由注解关闭的结构化请求摘要和响应形状摘要。列表不返回这些详情字段，认证敏感信息不会落库。

开发环境启动后可访问 `/swagger-ui/index.html` 查看 OpenAPI 页面，也可按分组访问 `/v3/api-docs/{group}`（`auth`、`system`、`file`、`log`、`monitor`）。生产环境默认关闭 SpringDoc API 文档与 Swagger UI；如需临时诊断，应通过受控的运维变更并由网关限制访问，而不是直接公开接口文档。
