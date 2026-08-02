# P3-03/P3-04 Lock4j 与 ID 策略正式决策记录

## 决策日期

2026-08-02

## 决策结论

### P3-03 Lock4j：`DEFER`

当前不引入 Lock4j，不新增依赖、注解、LockTemplate、锁配置、数据库表或管理页面。

保留以下触发条件：只有出现真实的跨实例并发业务入口，且数据库唯一约束、业务幂等或现有
Redisson 能力不足以保证正确性时，才重新评估为 `GO`。重新评估后必须明确锁键命名空间、
超时与异常释放策略，并补充并发、多实例、超时和失败恢复测试；普通 CRUD 不机械加锁。

本次不采用 Lock4j 的理由：

- 当前已交付能力是受控维护任务，SnailJob 已负责任务调度、重试和执行记录；
- 现有业务写入未发现需要独立分布式互斥才能成立的真实跨实例场景；
- 候选 Spike 已记录 Lock4j 基线偏旧，当前架构优先使用数据库约束、任务幂等或直接封装
  Redisson，避免重复锁抽象。

### P3-04 ID 策略：继续数据库自增 `AUTO_INCREMENT`

当前正式业务表继续使用数据库 `BIGINT AUTO_INCREMENT` 主键，Java 实体使用 `Long`，不切换
Snowflake，不新增 worker/datacenter 配置，不执行历史数据迁移，不新增 ID 映射表。

接受边界：

- 数据库负责单库主键生成和唯一性；
- 多实例应用可以并行写入同一数据库，但不把数据库 ID 当作跨库全局时间有序 ID；
- 对外 JSON 的 Long 继续遵守现有字符串序列化约束，前端不假设 ID 连续或可计算；
- 若未来进入多数据库写入、分库分表或明确需要跨库 ID 生成，必须另立 ID 迁移需求，先
  设计 worker 分配、字符串契约、数据重建/映射和回滚，再实施。

选择数据库自增的理由：

- 当前所有业务主键和 Flyway 基线已经统一为 `BIGINT AUTO_INCREMENT`；
- 当前没有跨库写入或 Snowflake 的真实收益场景；
- 保持现有数据库、实体、接口和前端契约，避免无收益的全链路迁移风险。

## 证据范围

- `alpha-server/src/main/resources/db/migration/V1__initial_schema.sql` 及后续迁移使用
  `BIGINT PRIMARY KEY AUTO_INCREMENT`；
- `SystemEntity`、`SysLoginLog`、`SysOperLog` 使用 MyBatis-Plus `IdType.AUTO` 与 `Long`；
- `docs/requirements/main/alpha-framework-modernization/candidate-capabilities-spike-2026-07-29.md`
  已记录 Lock4j 为 `DEFER`，优先数据库约束或直接使用 Redisson；
- `docs/requirements/main/alpha-framework-modernization/frontend-design.md` 已要求 ID 契约
  不假设连续性，并为后续 Long 字符串统一保留边界。

## 对 G4 的影响

P3-03 的 `DEFER` 不阻断 G4；P3-04 已完成当前投产前的策略记录。G4 剩余工作集中在生产
配置、健康检查、升级步骤、故障定位演练及全量验证；备份恢复和真实删除门禁已由用户取消，
不包含 Lock4j 或 Snowflake 实施。
