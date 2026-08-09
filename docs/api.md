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
| 系统配置 | `GET /system/settings/{group}`、`PUT /system/settings/{group}`、`GET /system/settings/file/credentials`、`POST /system/settings/security/keys/regenerate` |
| 字典     | `/system/dict-types` CRUD、`/system/dict-types/{typeId}/items`、`/system/dict-items/{id}`、`GET /system/dicts/{typeCode}/items`、`PUT /system/dicts/cache` |
| 文件     | `GET /files`、`POST /files/upload`、`GET /files/{id}/access-url`、`DELETE /files/{id}` |
| 日志     | `GET /logs/operations`、`GET /logs/operations/{id}`、`GET /logs/logins`、`PUT /logs/operations/{id}/handled` |
| Redis 管理 | `GET /monitor/redis/overview`、`GET /monitor/redis/metrics`、`GET /monitor/redis/keys`、`GET/DELETE /monitor/redis/key` |
| 在线用户 | `GET /monitor/online-users`、`DELETE /monitor/online-users/{userId}/sessions/{terminalIndex}` |
| SQL 监控 | `GET /monitor/sql/logs`、`DELETE /monitor/sql/logs`、`GET/PUT /monitor/sql/settings` |

文件上传支持 `txt`、`pdf`、`doc/docx`、`xls/xlsx` 与 `png/jpg/jpeg/gif/webp`。文件响应中的 `publicUrl` 表示当前可访问 URL：默认 `FILE_PUBLIC_ACCESS=false` 时返回短期 HMAC 签名的 `/api/files/{id}/content` 地址；仅显式启用公开访问时，本地存储返回 `/uploads/<uuid>.<ext>`，MinIO 返回配置的公开对象 URL。图片访问 URL 可直接用于预览和头像展示。

`GET /auth/captcha` 返回当前登录配置的 `type`（`numeric` 数字验证码或 `slider` 滑块验证）、验证码挑战及 `rememberMeEnabled`；滑块挑战额外返回背景图、拼图块和尺寸信息，目标位置只保存在服务端；验证码关闭时仍返回配置类型，但不创建挑战。

`GET /files/{id}/access-url` 需要 `file:list` 权限，仅返回指定文件新的短期访问地址，不返回对象存储凭据或文件内容；管理端用于刷新已持久化 Logo 的过期访问地址。

登录请求 `POST /auth/login` 必须包含 `username`、`password` 和已注册的 `clientId`。当前管理端固定使用 `clientId=pc-admin`；可选的 `deviceId` 和 `deviceName` 仅用于会话展示与追踪，不作为密钥。缺失、格式非法、未知或禁用的客户端返回 HTTP 400，不会静默补默认客户端。

个人头像上传只接受 `png/jpg/jpeg/gif/webp`，使用当前登录用户身份，不要求文件管理权限；上传成功后立即更新该用户头像。修改个人密码时，旧密码错误会返回“旧密码错误”，新旧密码相同会返回“新密码不能与旧密码相同”。

HTTP 状态与响应 `code` 一致：参数错误 400、未登录 401、无权限 403、登录锁定 429、未处理错误 500。响应头 `X-Trace-Id` 与响应体 `traceId` 可用于问题定位。

系统配置按站点、登录、文件、第三方登录、支付、安全、小程序和公众号分组。`system:setting:list` 可读取已注册字段，`system:setting:update` 可更新；敏感字段加密保存，读取仅返回是否已配置。文件存储支持 `local`、`minio`、`oss`、`cos`；支付仅提供本地模拟订单创建、查询和完成流程。

`GET /system/settings/file/credentials` 是唯一的文件存储凭据查看接口，必须具有 `system:setting:update` 权限，响应仅包含 `{ "accessKey": "...", "secretKey": "..." }`。管理端不会自动调用它；管理员点击“显示 Access Key 与 Secret Key”才临时填充当前表单，点击隐藏、切换配置页或保存后清除。普通 `GET /system/settings/file` 始终不返回密钥明文。

文件配置中的 `allowedExtensions` 使用逗号分隔的扩展名字符串保存，例如 `png,jpg,pdf`。管理端以标签多选输入：每输入一个扩展名后按回车确认，可连续添加多个值；也可使用逗号或空格分隔输入。保存时前端去除扩展名前的点号并转换为逗号分隔字符串，上传运行时按逗号解析并逐项校验，不能填写 MIME 类型或通配符。

`POST /system/settings/security/keys/regenerate` 只生成一对新的 RSA 密钥，并在本次受保护响应中返回 `{ "publicKey": "...", "privateKey": "..." }`，不会自动写入数据库。前端将公钥和 PKCS#8 私钥直接回填到安全配置表单，用户确认后点击“保存”通过 `PUT /system/settings/security` 持久化。数据库只保存公钥明文和加密后的私钥，普通 `GET /system/settings/security` 响应不包含私钥明文。

第三方登录提供 `GET /auth/oauth/{github|wechat|alipay}/authorize` 和对应 `callback`：授权 URL 带一次性、十分钟有效的 state；回调仅映射不可变外部 subject。首次身份创建禁用本地账号并返回 `PENDING_APPROVAL`，管理员启用并分配角色后才能换取管理端会话。公众号回调使用 `GET/POST /wechat/official-account/callback` 验签；自定义菜单通过 `POST /system/settings/official-account/menu/publish` 发布。

定义为 `DATA_ONLY` 时仅保存受控业务数据，不自动绑定应用行为。动态定义只允许使用已实现的文件运行时绑定：单文件上传大小、允许扩展名和私有文件访问期限；其定义必须保持已发布。值写入在事务提交后才发布或失效 Redis 缓存；禁用值会回退到定义默认值。未注册、未发布、类型或范围不合法的值一律返回 400“请求参数错误”，不会返回配置值、数据库错误或内部异常信息。

Redis 管理接受可选前缀筛选、键名关键词与 `SCAN` 游标（`cursor`、`count=1..100`），空前缀表示查询全库键空间。接口返回键名、分类、类型、TTL、大小估计、展示级别和值预览；展示级别为 `HIDDEN`、`MASKED` 或 `PLAIN`。验证码、失败计数、Sa-Token 会话和未注册且命中密钥特征的键始终为 `HIDDEN`。删除接口仅返回确认文本，不回显键名。

`GET /monitor/redis/metrics` 需要 `monitor:redis:list` 权限，返回当前应用实例的采样状态、最后一次成功的内存和连接快照、按累计调用数排序的最多 10 条命令统计，以及最多 1,440 个趋势点。内存快照只返回白名单数值字段，包括 `usedMemoryBytes`、`usedMemoryRssBytes`、`usedMemoryPeakBytes`、`maxMemoryBytes` 和用于无 `maxmemory` 场景计算仪表盘比例的 `totalSystemMemoryBytes`。采样状态为 `DISABLED`、`COLLECTING`、`HEALTHY`、`DEGRADED` 或 `STALE`；关闭、首次采样或采样失败仍返回 200 和明确状态。接口不返回 Redis 地址、凭据、原始 INFO、键值或命令参数。

在线用户按 Sa-Token 受控会话索引分页返回，每行对应一个登录终端，包含账号、部门、clientId、设备、IP、浏览器、操作系统、登录时间、最后访问时间和不可逆 token 摘要。查询页大小限制为 100；定向下线需要 `monitor:online:kickout` 权限，只影响指定用户的指定终端。

SQL 监控返回当前进程内最近 SQL 摘要，支持 `limit=1..200`、`type=SELECT|INSERT|UPDATE|DELETE|UNKNOWN`、`keyword` 和 `slowOnly` 筛选。SQL 文本只保留 MyBatis 占位符，清除注释和字面量并限制单条摘要长度，不渲染真实参数值；摘要按固定容量和保留时间自动清理。清空接口仅清空内存队列，不影响数据库或审计日志。采集设置接口返回当前全局采集状态、已发现的 MyBatis statement 和排除列表；更新设置需要 `monitor:sql:control` 权限，重启后恢复默认“开启 + 全部记录”。连接池运行指标通过受控的 `/actuator/prometheus` 观测。

日志列表响应包含账号、结果、IP、地点、clientId、设备摘要、浏览器、操作系统和 traceId；操作日志另包含响应状态、耗时和业务错误码。操作日志详情需要 `log:operation:detail` 权限，才返回有界异常摘要、默认采集且可由注解关闭的结构化请求摘要和响应形状摘要。列表不返回这些详情字段，认证敏感信息不会落库。

开发环境启动后可访问 `/swagger-ui/index.html` 查看 OpenAPI 页面，也可按分组访问 `/v3/api-docs/{group}`（`auth`、`system`、`file`、`log`、`monitor`）。生产环境默认关闭 SpringDoc API 文档与 Swagger UI；如需临时诊断，应通过受控的运维变更并由网关限制访问，而不是直接公开接口文档。
