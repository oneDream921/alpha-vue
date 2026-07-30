# 项目专属 Skill 调整说明

## 1. 原则

- 不修改 AFK 核心 Skill、生命周期或生成机制。
- `.project-agent` 是 Alpha 项目专属规则和 Skill 的唯一维护源。
- `.agents`、`AGENTS.md`、`CLAUDE.md` 和 Cursor 规则是生成投影，不手工编辑。
- Skill 描述执行流程和检查清单；正式技术规则仍由 `docs/` 承载。
- 不为每个依赖创建一个 Skill。

## 2. 一期调整

优先更新现有项目 Skill，不立即新增大量 Skill。

### `alpha-vue-architecture`

增加：

- 长期蓝图与当前阶段分离。
- Spring Boot 4 兼容 Spike 闸门。
- 单体与独立运维扩展判断标准。
- 试点先于全量平台化。

### `alpha-vue-backend`

替换旧内容：

- Druid -> Hikari。
- Lettuce/RedisTemplate -> Redisson 领域 Adapter。
- Knife4j 文档注解 -> SpringDoc 基线；Therapi 由兼容结论决定。

增加：

- `clientId` 会话不变量。
- Redis Codec 与命名空间检查。
- 类型化配置注册。
- 日志安全摘要与 IP/设备快照边界。
- SQL 摘要不得记录参数。

### `alpha-vue-api-design`

增加：

- 登录必填 `clientId`。
- 会话级在线用户和强制下线。
- ID 与时间契约变更必须独立评审。
- 用户偏好、缓存明文查看等接口属于二期，不得提前混入一期。

### `alpha-vue-frontend`

一期增加：

- 认证存储规则。
- `clientId=pc-admin` 注入。
- Profile、权限和菜单刷新恢复。
- Service 按领域渐进拆分。
- Ant Table 继续作为一期标准。

二期在 VXE 单页试点通过后再增加：

- 当前试点页面的列设置约束。
- 跨设备偏好约束。
- ECharts 验收。

只有相同表格规则在至少两个真实页面稳定复用后，Skill 才能加入公共表格组件规范，
不得预先规定 `AlphaGrid`。

### `alpha-vue-local-start`

一期更新：

- 开发默认 local 文件存储。
- MySQL、Redis 是默认依赖。
- MinIO 按任务启动并纳入 smoke。
- 不默认启动 Spring Boot Admin 和 SnailJob。

### 任务提示词与任务交付

新增两个职责分离的项目 Skill：

| Skill | 使用线程 | 职责 |
| --- | --- | --- |
| `alpha-vue-generate-task-prompts` | 用户用于准备任务提示词的强模型线程 | 根据用户指定的任务和已确认计划，生成用户可复制的快速模型执行提示词或强模型评审/修复提示词 |
| `alpha-vue-task-delivery-gate` | 用户手工创建的单任务执行线程 | 按已确认任务包实施、验证、评审并遵守提交与合并授权门禁 |

`alpha-vue-generate-task-prompts` 只生成提示词，不选择下一任务、不核对或维护整体计划进度，
不更新 `implementation-plan.md` 或 `acceptance.md`，也不创建、派发、等待或管理线程，不调用
子代理，不实现生产代码。用户负责指定任务，并把生成的提示词复制到自行选择模型的新线程。
该 Skill 可以只读检查实际 Git、代码、计划和验收材料，使提示词包含准确的范围、验证命令和
停止条件。

每次只根据用户请求生成一种提示词：

1. `EXECUTION`：为已确认任务生成快速模型执行提示词；
2. `REVIEW`：为指定实现生成独立强模型评审提示词；
3. `REMEDIATION`：为用户已接受的问题生成限定当前任务的修复提示词。

任务不明确时只反馈缺失信息，不代替用户选择任务或扩大范围。

## 3. 新增 Skill 的条件

只有同时满足以下条件才新增项目专属 Skill：

1. 任务有独立触发语义。
2. 执行流程跨越多个文件或组件。
3. 现有 Skill 加入后会明显混淆职责。
4. 至少预期重复使用两次。

候选：

| Skill | 最早阶段 | 创建条件 |
| --- | --- | --- |
| `alpha-vue-infrastructure` | 一期 | Redisson/Hikari/存储任务使 backend Skill 无法保持清晰 |
| `alpha-vue-observability` | 二期 | Spring Boot Admin、Redis 详细监控和日志策略同时进入实施 |
| `alpha-vue-job` | 三期 | 后续补齐失败链路、架构结论变为 `GO`，并开始真实任务 |
| `alpha-vue-client-auth` | 一期后复盘 | `clientId` 会话流程需要被小程序或第二客户端复用 |

不提前创建空 Skill。

## 4. 规则更新时机

1. 需求和 Plan 文档先确认。
2. 实施任务完成并通过测试。
3. 更新当前正式规范。
4. 更新项目 Rule 和 Skill。
5. 执行 AFK `sync --dry-run`。
6. 执行 AFK `sync`。
7. 再次 dry-run，并解析 Manifest、Codex、Claude、Cursor 投影。

项目 Skill 不得把尚未实现的目标写成当前事实。阶段性目标可以链接本需求目录，但必须注明“待实施”。

## 5. 普通模型执行约束

项目 Skill 应要求普通执行模型：

- 一次只执行实施计划中的一个任务。
- 先检查任务前置条件和当前 Git 状态。
- 不扩大文件范围。
- 不修改无关现有改动。
- 只运行任务指定的聚焦测试。
- 遇到兼容性结论、数据库语义、会话并发或日志安全不明确时停止并升级评审。
- 完成后报告实际文件、测试和未完成项，不自动进入下一任务。

## 6. 强模型复核点

必须强模型或等价人工复核：

- Spring Boot 4 依赖 Spike 结论。
- Redis 序列化和 Sa-Token DAO 切换。
- 同客户端单会话并发正确性。
- 日志敏感字段清理。
- Flyway DDL 和数据迁移。
- VXE 是否推广。
- SnailJob 删除类任务。
- Snowflake 投产决策。

## 7. AFK 不变项

- requirements-explore 的单问题循环不改。
- requirements-doc 的落盘边界不改。
- planning、debugging、java-code-review 和 update-memory 不改。
- 不把本项目经验反向写入 AFK 核心，除非另有明确的 AFK 框架需求。
