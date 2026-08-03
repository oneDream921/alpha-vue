# 安全说明

- 密码仅保存 BCrypt 哈希；修改密码后当前会话立即注销。
- 登录必须使用数据库注册且启用的 `clientId`；当前管理端使用 `pc-admin`。`deviceId` 和 `deviceName` 仅作为受长度限制的会话元数据，不作为认证凭据。
- Token 仅从 Authorization Header 读取，禁止 Cookie 和请求体 Token。
- Redis 保存 Sa-Token 会话、登录失败窗口和一次性验证码；普通会话 8 小时，记住我 7 天，无操作 30 分钟失效。
- 登录失败按账号与 IP 原子计数，默认 5 次后锁定 15 分钟。
- `SUPER_ADMIN` 是唯一全权限绕过角色且不可删除；其他访问由后端权限校验决定。
- 禁用或软删除账号后，已签发 Token 的下一次请求立即失效；管理员可主动踢下线。
- 操作审计异步写入；`@OperationLog` 默认保存结构化、脱敏和截断后的请求摘要，入口可显式关闭请求和响应摘要，响应只保存状态和形状摘要。密码、Token、Cookie、验证码、请求体、上传正文及 secret/key 字段不落审计库，硬性禁采集规则优先于注解默认值。
- 操作审计事件默认经 `alpha:audit:operation:v1` Redis Stream 投递，Consumer Group 仅在数据库写入成功后 ACK；重复事件由 `event_id` 幂等，失败消息会重试，超过上限进入死信 Stream。Redis 不可用时普通操作降级为有界异步数据库写入。
- 应用内维护任务的真实删除或失败会记录系统操作日志摘要；摘要仅包含任务名、状态、扫描数量和影响数量，不包含请求体、Token、Redis 值、文件内容或连接信息。
- 登录和操作日志保存有界的 clientId、设备摘要、User-Agent 解析结果、traceId、业务错误码、IP 与地点快照；异常摘要会限制长度并脱敏密码、Token、Cookie、验证码、secret/key 等字段。
- 客户端 IP 默认取 socket 对端地址；只有 `TRUSTED_PROXY_ADDRESSES` 明确列出的代理对端才允许使用 `X-Forwarded-For` 的首个地址，未配置时不信任转发头。
- 外部 IP 地点使用 `IP_LOCATION_XDB` 指向的 ip2region 离线 XDB 查询；XDB 未配置或查询失败时返回“未知”，不影响业务请求。
- 参数配置只允许定义目录中已发布的 `file.*` 业务键。敏感定义仅可作为数据存储，默认值和实际值不进入普通响应、请求审计或应用日志；动态定义只能使用代码已有的文件业务绑定，不能绑定 Spring、Server、数据源、Redis、存储提供者、Sa-Token、连接信息或诊断开关。
- 上传使用 UUID 对象键，校验扩展名、MIME、图片签名、大小和安全路径；删除顺序为先对象后元数据。
- 文件默认私有：本地静态映射和 MinIO 匿名读取默认关闭，接口返回短期 HMAC 签名 URL。必须配置 `FILE_ACCESS_TOKEN_SECRET`；仅在明确设置 `FILE_PUBLIC_ACCESS=true` 时才允许公开读取。
- 每个请求生成 traceId，写入 MDC、响应头和统一响应；生产日志按天滚动并保留 30 天。
- API 响应设置 `nosniff`、拒绝 iframe、Referrer-Policy、Permissions-Policy 与 `Cache-Control: no-store`；CSP、TLS、HSTS 和限流由边缘反向代理统一配置。
- 数据库连接池使用 Spring Boot BOM 默认 HikariCP，并通过受 Bearer 鉴权与网络边界保护的 Actuator/Micrometer 暴露 Hikari 指标；泄漏检测默认关闭，仅在受控诊断场景通过 `DB_POOL_LEAK_DETECTION_THRESHOLD_MS` 临时开启。Redis 连接池采用有限等待，Sa-Token 键检索使用带上限的 `SCAN`，禁止在生产路径使用 `KEYS`。
- Redisson Client 默认使用 `StringCodec`；对象 Codec 按缓存和 Sa-Token 场景分别使用显式类白名单，不兼容读取旧 JDK 序列化数据。生产 Sa-Token DAO 使用 SHA-256 物理键映射和受控索引，完整实现对象、字符串、会话、TTL 与有界搜索。所有新业务键必须使用 `alpha:*`，不得与旧业务键双读或双写。
- Redis 运维台使用 Redisson 带上限的 `SCAN` 查询全库键，值预览按代码注册的 `HIDDEN`、`MASKED`、`PLAIN` 级别处理；`REDIS_MASK_VALUES=true` 时所有值至少按 `MASKED` 返回。验证码、失败计数和 Sa-Token 会话也可通过已注册配置切换展示级别，默认仍为 `HIDDEN`；未注册且命中密钥特征的键始终为 `HIDDEN`，且不反序列化 Redis 中的对象。展示级别配置只允许使用已发布的缓存定义，变更沿用参数配置权限、审计和恢复默认值能力。Redis 指标采样仅执行只读 `INFO ALL` 并映射白名单字段，响应不包含地址、凭据、原始 INFO、键值或命令参数。禁止 `KEYS`、`FLUSHDB`、`FLUSHALL`、任意键写入及批量删除。删除单键需 `monitor:redis:delete` 权限、二次确认并记录审计。
- 在线用户页只读取 Sa-Token 的受控会话索引并限制单页数量，不执行无边界 Redis 扫描；token 只展示 SHA-256 摘要。定向下线按用户和终端索引执行，需 `monitor:online:kickout` 权限并记录审计，不影响其他 client 会话。
- SQL 日志页只展示进程内最近 SQL 摘要，SQL 文本保留 `?` 占位符，禁止渲染或记录真实参数值、请求体、密码、Token、验证码和 secret/key 字段。采集开关和 Mapper 过滤是当前进程全局运行时设置，修改需要 `monitor:sql:control` 权限。SQL 日志页不得提供任意 SQL 执行能力；如需数据库诊断，只能生成供人工审核的脚本或通过受控运维流程执行。
- 应用内日志保留清理只删除超过保留期的登录日志、成功操作日志和已处理失败操作日志；未处理失败操作日志不自动删除。临时文件清理仅限本地上传过程中遗留的 `.upload-*.tmp` 文件。所有删除型维护任务默认 dry-run，并有单轮批次上限。
- SpringDoc OpenAPI 与 Swagger UI 仅在 `dev` profile 开启，认证登录和验证码在 OpenAPI 中显式标记为 `security: []`，其他业务接口继承 Bearer 鉴权。生产默认关闭文档端点；`/actuator/health/**` 可供存活/就绪探针访问，其他 Actuator 端点必须由网关或内部网络和应用鉴权共同保护。
- `deploy/.env`、生产凭据、Token 和真实个人数据不得提交。MinIO 应用凭据不得复用 root 凭据。

生产部署必须设置独立强密码、启用验证码、使用 HTTPS、由反向代理限制上传体积，并在边缘配置 TLS、HSTS、CSP、受信任代理与速率限制。不得把 `X-Forwarded-For` 直接当作客户端 IP，除非该请求确实来自受信任的反向代理。
