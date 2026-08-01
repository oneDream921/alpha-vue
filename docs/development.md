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
| `APP_TIME_ZONE`                         | `Asia/Shanghai`                | 应用与数据库连接会话时区                   |
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
| `DB_POOL_MIN_IDLE` / `DB_POOL_MAX_SIZE` | dev: `2` / `10`，prod: `5` / `20` | Hikari 最小空闲与最大连接数            |
| `DB_POOL_CONNECTION_TIMEOUT_MS`         | `30000`                           | Hikari 获取连接的最大等待时间          |
| `DB_POOL_IDLE_TIMEOUT_MS`               | `600000`                          | Hikari 空闲连接回收时间                |
| `DB_POOL_MAX_LIFETIME_MS`               | `1800000`                         | Hikari 连接最大生命周期                |
| `DB_POOL_LEAK_DETECTION_THRESHOLD_MS`   | `0`                               | Hikari 泄漏检测阈值；`0` 表示关闭       |
| `SQL_LOG_MAX_ENTRIES`                   | `200`                            | SQL 日志页保留的进程内最近 SQL 摘要数量 |
| `SQL_SLOW_THRESHOLD_MS`                 | dev: `500`，prod: `1000`          | 慢 SQL 判断阈值                         |
| `SQL_LOG_RETENTION_MS`                  | `1800000`                        | SQL 摘要最长保留时间；仅保留进程内数据 |
| `SQL_LOG_MAX_SQL_LENGTH`                | `4096`                           | 单条 SQL 摘要最大字符数                |
| `AUDIT_CAPTURE_BUSINESS_EXCEPTION_STACK` | `false`                         | 是否在审计日志中记录自定义业务异常堆栈 |
| `REDIS_POOL_MAX_ACTIVE`                 | dev: `8`，prod: `16`           | Redisson 最大连接池大小                   |
| `REDIS_POOL_MAX_WAIT`                   | `1s`                           | Redis 连接耗尽时最大等待时间               |
| `REDIS_DATABASE`                         | `0`                            | Redisson 独立 Client 使用的 Redis DB       |
| `REDIS_RETRY_INTERVAL`                   | `1s`                           | Redisson 命令重试间隔                      |
| `REDIS_RETRY_ATTEMPTS`                   | `2`                            | Redisson 命令重试次数                      |
| `REDIS_CACHE_TTL`                        | `10m`                          | P1-03 验证缓存默认 TTL                     |
| `REDIS_METRICS_ENABLED`                  | `true`                         | 是否启用 Redis INFO 指标采样               |
| `REDIS_METRICS_SAMPLE_INTERVAL_MS`       | `60000`                        | 指标采样间隔（毫秒）                       |
| `REDIS_METRICS_RETENTION_MS`             | `86400000`                     | 成功样本进程内最长保留时间（毫秒）         |
| `REDIS_METRICS_MAX_SAMPLES`              | `1440`                         | 成功样本进程内最大保留数量                 |

项目使用直接 Redisson 4.6.1 Client 和 Spring Cache 集成，不使用
`redisson-spring-boot-starter`。默认 Codec 是 `StringCodec`；缓存和 Sa-Token 对象边界使用代码登记的 Kryo5 白名单 Codec。验证码、登录失败、系统配置、系统字典、Sa-Token 会话和 Redis 监控均使用 Redisson，生产键统一为 `alpha:*`，不读取或清理旧前缀。

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

## 本地启停脚本

```bash
# 启动完整本地环境：先后端健康检查，再启动前端
scripts/start-local.sh

# 单独启动或停止前端、后端
scripts/start-backend.sh
scripts/stop-backend.sh
scripts/start-frontend.sh
scripts/stop-frontend.sh

# 单独启动或停止 MySQL、Redis、MinIO 依赖容器
scripts/start-dependencies.sh
scripts/stop-dependencies.sh
```

脚本安全读取 `deploy/.env`，不会 shell source 该文件。停止脚本只处理对应应用的非 Docker 进程，不会停止 MySQL、Redis 或 MinIO 依赖容器。

Vite 将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。Flyway 在后端启动时自动迁移数据库，并使用独立连接执行迁移，不复用业务连接池；不手工修改已发布迁移。开发 profile 启用 SpringDoc 和 Swagger UI，可访问 `/swagger-ui/index.html` 及 `/v3/api-docs/{group}`；生产 profile 关闭文档相关路径。生产静态服务器需要为 Vue Router 配置 `index.html` 回退，但不得把 `/api` 或 `/uploads` 回退为前端页面。

管理端 `SQL 日志` 页面用于查看当前后端进程的最近 SQL 摘要；采集开关和 Mapper 勾选只影响当前进程。Hikari 指标通过受控的 `/actuator/prometheus` 观测。SQL 日志的保留边界和故障处置见 [运行与发布手册](operations.md)，敏感数据限制见 [安全说明](security.md)。

Redis 管理页的增强指标默认每分钟执行一次只读 `INFO ALL` 采样，最多在当前应用进程保留 24 小时或 1,440 个成功样本。设置 `REDIS_METRICS_ENABLED=false` 可关闭采样和增强面板，不影响原有 Redis 概览、受限 `SCAN` 和单键删除能力。

## 存储切换

本地模式设置 `FILE_STORAGE_PROVIDER=local`；默认私有访问不会注册 `FILE_LOCAL_PUBLIC_URL` 的静态资源映射，而是通过短期签名接口读取对象。MinIO 模式设置 `FILE_STORAGE_PROVIDER=minio`、应用访问密钥与 bucket，默认同样通过应用签名接口读取。只有明确设置 `FILE_PUBLIC_ACCESS=true` 时，才直接开放本地静态映射或使用 MinIO 公开 URL。上传校验在存储提供方之前执行，两种模式共享扩展名、MIME、图片签名和大小限制。

Docker Compose 的一次性 `minio-init` 服务会为本地环境创建 bucket、独立应用用户和 bucket 级读写策略；对象通过应用生成的短期签名 URL 预览，不要求 bucket 匿名读取。生产环境应由部署平台预建 bucket 和最小权限凭据，不以 root 凭据启动应用。

`deploy/smoke-test.sh` 会上传普通文本和真实 PNG，检查列表、图片访问 URL 与删除，然后清理测试记录。涉及真实联调、部署配置或切换存储模式时执行；切换存储模式后应分别验证一次。

## 参数配置开发边界

`sys_config_definition` 是参数配置的受控定义目录，管理端只能新增 `file.*` 业务定义。定义支持 Boolean、Integer、Enum、String 和默认值、范围/枚举、敏感与动态标记；密码、密钥、Token、连接参数和基础设施语义会被拒绝。自定义定义默认为 `DATA_ONLY`，不会自动改变运行时行为。现有动态绑定仅服务于文件上传大小、允许扩展名和私有访问期限，并且必须保持已发布。

配置值变更先通过定义校验，再写入 `sys_config`；事务提交成功后才更新或失效 Redis 缓存。敏感定义不会回显默认值或配置值，操作审计只记录固定脱敏标记。文件上传的业务限制以该定义目录为准，`FILE_MAX_SIZE_BYTES` 等环境变量仍是 HTTP 容器请求体上限，不构成可管理的动态策略。

运行参数、指标和发布顺序见 [运行与发布手册](operations.md)。
