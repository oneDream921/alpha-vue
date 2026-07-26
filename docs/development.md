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
| `FILE_LOCAL_PUBLIC_URL`                 | `/uploads`                     | 本地文件公开访问路径，必须是应用内绝对路径 |
| `FILE_MAX_SIZE_BYTES`                   | `10485760`                     | 单文件最大字节数                           |
| `FILE_MAX_REQUEST_SIZE_BYTES`           | `12582912`                     | multipart 单请求最大字节数                 |
| `MINIO_ENDPOINT`                        | `http://localhost:9000`        | MinIO API                                  |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 无                             | MinIO 应用凭据，不使用 root 凭据           |
| `SA_TOKEN_TIMEOUT`                      | `28800`                        | 普通会话秒数                               |
| `SA_TOKEN_ACTIVE_TIMEOUT`               | `1800`                         | 无操作超时秒数                             |
| `DB_POOL_MIN_IDLE` / `DB_POOL_MAX_SIZE` | dev: `2` / `10`，prod: `5` / `20` | HikariCP 最小空闲与最大连接数           |
| `REDIS_POOL_MAX_ACTIVE`                 | dev: `8`，prod: `16`           | Lettuce 最大活动连接数                     |
| `REDIS_POOL_MAX_WAIT`                   | `1s`                           | Redis 连接耗尽时最大等待时间               |

## 日常命令

```bash
# 后端测试与打包
mvn -f alpha-server/pom.xml test
mvn -f alpha-server/pom.xml package

# 前端质量检查
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
```

Vite 将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。Flyway 在后端启动时自动迁移数据库，不手工修改已发布迁移。生产静态服务器需要为 Vue Router 配置 `index.html` 回退，但不得把 `/api` 或 `/uploads` 回退为前端页面。

## 存储切换

本地模式只需设置 `FILE_STORAGE_PROVIDER=local`，后端会把 `FILE_LOCAL_ROOT` 以只读资源映射到 `FILE_LOCAL_PUBLIC_URL`。MinIO 模式设置 `FILE_STORAGE_PROVIDER=minio`、应用访问密钥、bucket 与可选公开 URL。上传校验在存储提供方之前执行，两种模式共享扩展名、MIME、图片签名和大小限制。

Docker Compose 的一次性 `minio-init` 服务会为本地环境创建 bucket、独立应用用户和 bucket 级读写策略，并开放对象下载以支持预览。生产环境应由部署平台预建 bucket 和最小权限凭据，不以 root 凭据启动应用。

`deploy/smoke-test.sh` 会上传普通文本和真实 PNG，检查列表、图片公开访问与删除，然后清理测试记录。切换存储模式后应分别执行一次。

运行参数、指标和发布顺序见 [运行与发布手册](operations.md)。
