# Alpha Vue

Alpha Vue 是一个轻量、响应式的全栈管理基础框架。后端采用 Java 21、Spring Boot 4、MyBatis-Plus、Sa-Token、Redis、MySQL 和 Flyway；前端采用 Vue 3、Vite、TypeScript、Ant Design Vue、Pinia/Vue 状态模式、Vue Router 和 Tailwind CSS。

已包含账号登录、可选图形验证码、Bearer Token 会话、RBAC、用户/角色/菜单/部门管理、本地或 MinIO 文件存储、普通文件与图片上传预览、登录与操作审计、traceId、响应式管理端及 403/404 页面。管理列表支持手动刷新，并在新增、编辑、删除和上传后自动重新加载。

## 快速开始

要求：Java 21、Maven 3.9+、Node 20+、pnpm 9+、Docker 与 Docker Compose。

```bash
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，将占位值替换为仅用于本地环境的值
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

启动后端：

```bash
# 在 IDE Run Configuration 或当前终端安全地配置 deploy/.env 中所需变量；
# 不要 source、打印或提交该文件。
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml spring-boot:run
```

启动前端（另一个终端）：

```bash
cd alpha-web
pnpm install
pnpm dev
```

访问 `http://localhost:5173`。初始账号为 `admin` / `admin123`，首次使用后应立即在个人中心修改密码。

## 验证

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml clean test package
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config --quiet
deploy/smoke-test.sh
```

Vite 开发服务器支持直接刷新任意前端路由，并将 `/api` 与 `/uploads` 代理到后端。生产静态服务器必须把未知前端路由回退到 `index.html`，同时将 `/api`、`/uploads` 转发到 Spring Boot。

本地服务地址：MySQL `127.0.0.1:3306`、Redis `127.0.0.1:6379`、MinIO API `http://127.0.0.1:9000`、MinIO Console `http://127.0.0.1:9001`。

更多说明见 [开发指南](docs/development.md)、[API 约定](docs/api.md)、[安全说明](docs/security.md)、[运行与发布手册](docs/operations.md) 和 [编码规范](docs/conventions.md)。首次推送前请依次完成后端测试/打包、前端质量检查、Compose 配置校验和真实 smoke test；所有检查通过后，再将经确认的文件按主题提交到 `codex/alpha-vue-foundation` 并推送。
