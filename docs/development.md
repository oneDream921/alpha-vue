# 开发指南

## 目录

- `alpha-server`：Spring Boot 单体服务，按 `common`、`framework`、`modules` 分包。
- `alpha-web`：Vue 单页管理端，API、状态、路由、布局和页面按职责组织。
- `deploy`：本地 Docker Compose、环境变量示例和 smoke test。
- `docs`：设计、接口、安全和工程约定。

## 环境变量

将 `deploy/.env.example` 复制为被 Git 忽略的 `deploy/.env`。必须配置 MySQL、Redis、MinIO 的本地凭据。应用使用以下可选变量：

| 变量                                    | 默认值                         | 用途                                       |
| --------------------------------------- | ------------------------------ | ------------------------------------------ |
| `SERVER_PORT`                           | `8080`                         | 后端端口                                   |
| `CAPTCHA_ENABLED`                       | dev 为 `false`，prod 为 `true` | 登录图形验证码                             |
| `FILE_STORAGE_PROVIDER`                 | `local`                        | `local` 或 `minio`                         |
| `FILE_LOCAL_ROOT`                       | `uploads`                      | 本地文件目录                               |
| `FILE_LOCAL_PUBLIC_URL`                 | `/uploads`                     | 启用公开访问时的本地 URL 前缀，必须是应用内绝对路径 |
| `FILE_PUBLIC_ACCESS`                    | `false`                        | 是否直接公开存储对象；默认使用短期签名 URL |
| `FILE_ACCESS_TOKEN_SECRET`              | 无                             | 私有文件短期访问签名密钥，默认模式必须配置 |
| `FILE_ACCESS_TOKEN_TTL`                 | `5m`                           | 私有文件访问 URL 有效期                    |
| `FILE_MAX_SIZE_BYTES`                   | `10485760`                     | 单文件最大字节数                           |
| `FILE_MAX_REQUEST_SIZE_BYTES`           | `12582912`                     | multipart 单请求最大字节数                 |
| `MINIO_ENDPOINT`                        | `http://localhost:9000`        | MinIO API                                  |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 无                             | MinIO 应用凭据，不使用 root 凭据           |
| `SA_TOKEN_TIMEOUT`                      | `28800`                        | 普通会话秒数                               |
| `SA_TOKEN_ACTIVE_TIMEOUT`               | `1800`                         | 无操作超时秒数                             |
| `DB_POOL_MIN_IDLE` / `DB_POOL_MAX_SIZE` | dev: `2` / `10`，prod: `5` / `20` | Druid 最小空闲与最大连接数              |
| `DRUID_ENABLED`                         | dev: `true`，prod: `false`        | 是否启用 Druid 监控 servlet             |
| `DRUID_USERNAME` / `DRUID_PASSWORD`     | dev: `alpha` / `alpha-druid`      | Druid 监控登录账号；生产必须独立配置强密码 |
| `DRUID_ALLOW`                           | `127.0.0.1`                       | Druid 监控 IP 白名单                    |
| `DRUID_FILTERS` / `DRUID_WALL_ENABLED`  | `stat` / `false`                 | Druid 过滤器；需要 WallFilter 时显式开启 |
| `SQL_LOG_MAX_ENTRIES`                   | `200`                            | SQL 日志页保留的进程内最近 SQL 摘要数量 |
| `SQL_SLOW_THRESHOLD_MS`                 | dev: `500`，prod: `1000`          | 慢 SQL 判断阈值                         |
| `REDIS_POOL_MAX_ACTIVE`                 | dev: `8`，prod: `16`           | Lettuce 最大活动连接数                     |
| `REDIS_POOL_MAX_WAIT`                   | `1s`                           | Redis 连接耗尽时最大等待时间               |

## 日常命令

```bash
# 后端测试与打包
./mvnw -f alpha-server/pom.xml test
./mvnw -f alpha-server/pom.xml package

# 前端质量检查
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
```

Vite 将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。Flyway 在后端启动时自动迁移数据库，并使用独立连接执行迁移，不复用 Druid 业务连接池；不手工修改已发布迁移。生产静态服务器需要为 Vue Router 配置 `index.html` 回退，但不得把 `/api` 或 `/uploads` 回退为前端页面。

开发环境后端启动后可访问 `/druid/index.html` 查看 Druid 监控，默认账号来自 `DRUID_USERNAME` / `DRUID_PASSWORD`。管理端 `SQL 日志` 页面展示当前后端进程内最近 SQL 摘要；SQL 保留 `?` 占位符，不展示真实参数值。

## 存储切换

本地模式设置 `FILE_STORAGE_PROVIDER=local`；默认私有访问不会注册 `FILE_LOCAL_PUBLIC_URL` 的静态资源映射，而是通过短期签名接口读取对象。MinIO 模式设置 `FILE_STORAGE_PROVIDER=minio`、应用访问密钥与 bucket，默认同样通过应用签名接口读取。只有明确设置 `FILE_PUBLIC_ACCESS=true` 时，才直接开放本地静态映射或使用 MinIO 公开 URL。上传校验在存储提供方之前执行，两种模式共享扩展名、MIME、图片签名和大小限制。

Docker Compose 的一次性 `minio-init` 服务会为本地环境创建 bucket、独立应用用户和 bucket 级读写策略；对象通过应用生成的短期签名 URL 预览，不要求 bucket 匿名读取。生产环境应由部署平台预建 bucket 和最小权限凭据，不以 root 凭据启动应用。

`deploy/smoke-test.sh` 会上传普通文本和真实 PNG，检查列表、图片访问 URL 与删除，然后清理测试记录。涉及真实联调、部署配置或切换存储模式时执行；切换存储模式后应分别验证一次。

运行参数、指标和发布顺序见 [运行与发布手册](operations.md)。
