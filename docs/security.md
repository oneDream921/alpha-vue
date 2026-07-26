# 安全说明

- 密码仅保存 BCrypt 哈希；修改密码后当前会话立即注销。
- Token 仅从 Authorization Header 读取，禁止 Cookie 和请求体 Token。
- Redis 保存 Sa-Token 会话、登录失败窗口和一次性验证码；普通会话 8 小时，记住我 7 天，无操作 30 分钟失效。
- 登录失败按账号与 IP 原子计数，默认 5 次后锁定 15 分钟。
- `SUPER_ADMIN` 是唯一全权限绕过角色且不可删除；其他访问由后端权限校验决定。
- 禁用或软删除账号后，已签发 Token 的下一次请求立即失效；管理员可主动踢下线。
- 操作审计异步写入，仅记录元数据。密码、Token、Cookie、验证码、请求体、上传正文及 secret/key 字段不落审计库。
- 上传使用 UUID 对象键，校验扩展名、MIME、图片签名、大小和安全路径；删除顺序为先对象后元数据。
- 本地公开文件仅映射配置根目录，路径不可遍历；公开 URL 不包含原始文件名。MinIO bucket 仅开放对象读取，应用凭据限制在目标 bucket。
- 每个请求生成 traceId，写入 MDC、响应头和统一响应；生产日志按天滚动并保留 30 天。
- API 响应设置 `nosniff`、拒绝 iframe、Referrer-Policy、Permissions-Policy 与 `Cache-Control: no-store`；CSP、TLS、HSTS 和限流由边缘反向代理统一配置。
- 数据库连接池使用 HikariCP，并通过 Actuator/Micrometer 暴露运行指标；Redis 连接池采用有限等待，Sa-Token 键检索使用带上限的 `SCAN`，禁止在生产路径使用 `KEYS`。
- OpenAPI/Knife4j 仅在 `dev` profile 开启。生产默认关闭；`/actuator/health/**` 可供存活/就绪探针访问，其他 Actuator 端点必须由网关或内部网络和应用鉴权共同保护。
- `deploy/.env`、生产凭据、Token 和真实个人数据不得提交。MinIO 应用凭据不得复用 root 凭据。

生产部署必须设置独立强密码、启用验证码、使用 HTTPS、由反向代理限制上传体积，并在边缘配置 TLS、HSTS、CSP、受信任代理与速率限制。不得把 `X-Forwarded-For` 直接当作客户端 IP，除非该请求确实来自受信任的反向代理。
