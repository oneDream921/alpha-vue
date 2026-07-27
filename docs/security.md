# 安全说明

- 密码仅保存 BCrypt 哈希；修改密码后当前会话立即注销。
- Token 仅从 Authorization Header 读取，禁止 Cookie 和请求体 Token。
- Redis 保存 Sa-Token 会话、登录失败窗口和一次性验证码；普通会话 8 小时，记住我 7 天，无操作 30 分钟失效。
- 登录失败按账号与 IP 原子计数，默认 5 次后锁定 15 分钟。
- `SUPER_ADMIN` 是唯一全权限绕过角色且不可删除；其他访问由后端权限校验决定。
- 禁用或软删除账号后，已签发 Token 的下一次请求立即失效；管理员可主动踢下线。
- 操作审计异步写入，仅记录元数据。密码、Token、Cookie、验证码、请求体、上传正文及 secret/key 字段不落审计库。
- 上传使用 UUID 对象键，校验扩展名、MIME、图片签名、大小和安全路径；删除顺序为先对象后元数据。
- 文件默认私有：本地静态映射和 MinIO 匿名读取默认关闭，接口返回短期 HMAC 签名 URL。必须配置 `FILE_ACCESS_TOKEN_SECRET`；仅在明确设置 `FILE_PUBLIC_ACCESS=true` 时才允许公开读取。
- 每个请求生成 traceId，写入 MDC、响应头和统一响应；生产日志按天滚动并保留 30 天。
- API 响应设置 `nosniff`、拒绝 iframe、Referrer-Policy、Permissions-Policy 与 `Cache-Control: no-store`；CSP、TLS、HSTS 和限流由边缘反向代理统一配置。
- 数据库连接池使用 Druid，并通过 Druid、Actuator/Micrometer 暴露运行指标；Druid 默认启用 `stat`，如需 `wall` 过滤器必须显式开启并先验证 Flyway 与数据库用户权限。Redis 连接池采用有限等待，Sa-Token 键检索使用带上限的 `SCAN`，禁止在生产路径使用 `KEYS`。
- Redis 运维台使用带上限的 `SCAN` 查询全库键，默认由 `REDIS_MASK_VALUES=true` 脱敏所有值；关闭后只显示非敏感值。会话、验证码和疑似密钥始终脱敏，且不反序列化 Redis 中的对象。禁止 `KEYS`、`FLUSHDB`、`FLUSHALL`、任意键写入及批量删除。删除单键需 `monitor:redis:delete` 权限、二次确认并记录审计。
- SQL 日志页只展示进程内最近 SQL 摘要，SQL 文本保留 `?` 占位符，禁止渲染或记录真实参数值、请求体、密码、Token、验证码和 secret/key 字段。采集开关和 Mapper 过滤是当前进程全局运行时设置，修改需要 `monitor:sql:control` 权限。SQL 日志页不得提供任意 SQL 执行能力；如需数据库诊断，只能生成供人工审核的脚本或通过受控运维流程执行。
- Druid 监控使用 Druid 自身登录页；开发环境可默认开启，生产默认关闭。生产若临时开启，必须设置独立强密码，并由网关/IP 白名单/内网访问共同限制 `/druid/**`，不得直接公网暴露。
- OpenAPI/Knife4j 仅在 `dev` profile 开启。生产默认关闭；`/actuator/health/**` 可供存活/就绪探针访问，其他 Actuator 端点必须由网关或内部网络和应用鉴权共同保护。
- `deploy/.env`、生产凭据、Token 和真实个人数据不得提交。MinIO 应用凭据不得复用 root 凭据。

生产部署必须设置独立强密码、启用验证码、使用 HTTPS、由反向代理限制上传体积，并在边缘配置 TLS、HSTS、CSP、受信任代理与速率限制。不得把 `X-Forwarded-For` 直接当作客户端 IP，除非该请求确实来自受信任的反向代理。
