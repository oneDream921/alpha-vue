# P3-02 SnailJob 正式接入执行记录

## 决策

2026-08-02 用户确认将 SnailJob 从 `DEFER` 转为 `GO`，开始正式接入。

采用 SnailJob 2.0.2 独立 Server、独立 MySQL Schema 和 Alpha Client/Executor 的边界：

- Alpha 不嵌入 SnailJob Server，不复制管理端页面。
- Alpha 注册 `alphaMaintenanceJob`，执行一个有界维护周期。
- SnailJob 任务失败由 `ExecuteResult.failure` 返回，交由平台记录和重试。
- SnailJob 启用时自动停用 Spring 维护调度器，避免双重执行。
- SnailJob 默认关闭，P3-01 Spring 调度保留为回退路径。

## 实现

- Maven 引入 `com.aizuda:snail-job-client-starter:2.0.2` 和
  `com.aizuda:snail-job-client-job-core:2.0.2`。
- 新增可选 `SnailJobConfiguration`，启用时要求显式配置 group token。
- 新增 `SnailJobMaintenanceExecutor`，暴露 `alphaMaintenanceJob`。
- Compose 增加 `snailjob` profile，使用固定镜像 `opensnail/snail-job:2.0.2` 和独立
  `snailjob-db` 容器。
- 本地环境默认仍使用 Spring `@Scheduled`，通过 `SNAIL_JOB_ENABLED=true` 切换。

## 已完成验证

- SnailJob Client 2.0.2 依赖树解析通过。
- Alpha 后端编译通过。
- SnailJob Executor 成功/失败返回路径测试通过，2 项无失败。
- 原有维护任务聚焦测试和 H2 集成测试通过。
- Compose 基础配置和 `snailjob` profile 配置解析通过。
- Compose 已挂载经过处理的官方 2.0.2 MySQL 初始化表结构；首次创建数据库卷时执行，已有数据卷不会重复执行。
- 官方 SnailJob `vsj2.0.2` Server 源码 24 个模块构建通过。
- SnailJob Server HTTP 管理端返回 200，独立数据库初始化完成（23 张表）。
- Alpha Client 已注册在线实例 `192.168.0.125:17889`，管理端成功触发 `alphaMaintenanceJob`。
- 真实任务批次 `taskBatchId=1` 执行成功，5 个维护子任务均为 `status=OK`，且删除型任务保持 `dryRun=true`。

## 待完成验收

- 验证 SnailJob 失败重试、超时、停机恢复和任务日志清理。
- 由用户确认管理端普通任务验收结果，并补充失败链路演练结果。

## 当前状态

`READY_FOR_ACCEPTANCE`。Client/Executor 注册和一次真实任务触发已完成；失败重试、超时、停机恢复和日志清理仍是后续人工验收项。
