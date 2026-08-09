# Alpha Vue 本地启停脚本

所有 `*.sh` 脚本均可在仓库根目录的终端执行，也可在 IDEA 中直接打开后点击运行。运行前请确认 `deploy/.env` 已配置完成，并包含 `SYSTEM_SETTINGS_MASTER_KEY`：该值必须是 64 位十六进制字符串，用于加密数据库中的系统配置凭据。可先从 `deploy/.env.example` 复制配置，再填写本地值。

## 一键启动：`start-all.sh`

```bash
scripts/start-all.sh
```

启动完整本地环境：先检查并启动所需依赖，再启动后端；只有后端健康检查通过后，才启动前端。

## 一键停止：`stop-all.sh`

```bash
scripts/stop-all.sh
```

先停止前端，再停止后端。不会停止 MySQL、Redis、MinIO 或 Docker Desktop。

## Docker Compose 启动：`start-dependencies.sh`

```bash
scripts/start-dependencies.sh
```

启动 `alpha-vue-idea` Compose 项目中的 MySQL、Redis、MinIO 依赖容器。已经运行的容器会被复用。

## Docker Compose 停止：`stop-dependencies.sh`

```bash
scripts/stop-dependencies.sh
```

停止 `alpha-vue-idea` Compose 项目中的 MySQL、Redis、MinIO 容器；容器与数据卷会保留，可通过 `start-dependencies.sh` 再次启动。

> `start-backend.sh` 发现依赖端口缺失时，会自动调用 `start-dependencies.sh`，通常不需要手动先运行它。

如需操作其他 Compose 项目名，可临时覆盖：

```bash
COMPOSE_PROJECT_NAME=其他项目名 scripts/stop-dependencies.sh
```

## 后端启动：`start-backend.sh`

```bash
scripts/start-backend.sh
```

安全读取 `deploy/.env`，检查依赖，然后启动 Spring Boot 后端并等待 `http://localhost:${SERVER_PORT:-8080}/actuator/health` 健康检查通过。若缺少或格式错误的 `SYSTEM_SETTINGS_MASTER_KEY`，后端会在启动前失败并将原因写入日志。

## 后端停止：`stop-backend.sh`

```bash
scripts/stop-backend.sh
```

停止脚本启动的后端进程组，并清理后端端口上的非 Docker 监听进程。

## 前端启动：`start-frontend.sh`

```bash
scripts/start-frontend.sh
```

启动 Vue 开发服务器，默认地址为 `http://localhost:5173`，并等待该地址可访问。

## 前端停止：`stop-frontend.sh`

```bash
scripts/stop-frontend.sh
```

停止脚本启动的前端进程组，并清理前端端口上的非 Docker 监听进程。

## 兼容启动入口：`start-local.sh`

```bash
scripts/start-local.sh
```

与 `start-all.sh` 的启动效果相同：按顺序调用后端与前端启动脚本。`alpha-vue-local-start` Skill 调用的是 `start-all.sh`。

## 内部公共库：`common.sh`

`common.sh` 供其他脚本加载，不单独运行。它负责安全读取环境变量、管理 PID 与日志、识别 Docker 进程、检查端口和健康状态。

后端与前端日志、PID 文件位于 `${TMPDIR:-/tmp}/alpha-vue-local-start/`。

不要提交 `deploy/.env`，也不要在日志或聊天中粘贴其中的密钥、密码和连接凭据。
