# 后端设计

## 1. 适用边界

本文定义目标后端行为和分阶段落点，不是当前代码说明。实施时继续遵守现行三层职责、MyBatis XML、Flyway 追加迁移、中文公开错误和后端权限校验规则。

## 2. 阶段 0：兼容性 Spike

Spike 不进入主实现，不顺带重构业务代码。

### 2.1 Redisson

必须证明：

- Spring Boot 4 可以启动并创建 Redisson Client。
- Redisson 与项目当前 Jackson 3 HTTP 配置互不污染。
- 明确 Redis Codec 使用 Jackson 2、Jackson 3 或其他实现的依赖边界。
- 字符串、对象、集合和 TTL 读写正常。
- Spring Cache 可通过 Redisson 实现。
- Sa-Token DAO 的 String、Object、List 与过期语义可替换。
- 测试环境可以使用隔离 Redis 或明确的内存替身。

禁止仅以“能连上 Redis”判定通过。

### 2.2 SpringDoc 与 Therapi

必须证明：

- Controller 类和方法 Javadoc 可生成摘要与描述。
- DTO/VO 字段注释可以进入 Schema。
- Maven 编译器中的 Therapi Processor 与 Lombok、Spring 配置处理器共存。
- 不需要继承 SpringDoc 内部实现类。

Therapi 不通过时，保留 SpringDoc 并使用最少量 `@Tag`、`@Operation`、`@Schema`，不得阻塞一期。

### 2.3 后续候选

Spring Boot Admin、SnailJob 和 Lock4j 分别 Spike，不共享“Starter 能启动”这一弱结论。每项都必须覆盖其核心运行路径。

## 3. 一期设计

### 3.1 HikariCP

- 使用 Spring Boot 默认 HikariCP。
- 删除 Druid Starter、Servlet、Filter、配置、前端入口、权限、文档和环境变量。
- Hikari 指标通过 Actuator/Micrometer 暴露。
- 连接池配置只包含必要参数：最大连接、最小空闲、连接超时、空闲超时、最大生命周期和泄漏诊断开关。
- 不复制 Druid 的 WallFilter 语义；SQL 安全依赖最小数据库权限、参数绑定、代码评审和受控 Mapper。
- Flyway 使用独立迁移连接语义，不能因连接池替换改变迁移顺序。

验收重点：启动、Flyway、事务回滚、分页、自定义 Mapper XML、连接池指标。

### 3.2 Redisson 干净迁移

迁移完成态：

- 删除 Lettuce、`RedisTemplate`、`StringRedisTemplate` 和 JDK 序列化。
- Redisson 是唯一业务 Redis Client。
- Alpha Key 统一使用 `alpha:<domain>:<purpose>:<identifier>`。
- 不保留新旧双写或旧格式兼容层。
- 迁移窗口按白名单清理旧前缀 `auth:captcha:*`、`auth:login:failure:*`、
  `system:config:*`、`system:dict:*`、`satoken:*`，所有旧登录会话失效。

建议领域端口：

```text
framework/redis/                Redisson 配置、Codec、Key 规则
framework/cache/                Spring Cache 配置
modules/auth/...                captcha、login failure、session index
modules/system/...              dict、config、client cache
modules/monitor/...             受限 Redis 诊断
```

禁止：

- 万能静态 `RedisUtils`。
- 业务代码直接拼接无命名空间 Key。
- 在一个 Adapter 中混合认证、字典、配置和监控语义。
- 使用 `KEYS`。

### 3.3 Spring Cache

一期只接入：

- 客户端定义。
- 字典。
- 已登记系统参数。

要求：

- 每个缓存声明名称、Key 结构、TTL、空值策略和失效入口。
- 变更数据库后在事务成功边界失效缓存。
- 不通过缓存隐藏数据库唯一约束或业务一致性问题。
- 缓存读取失败必须有明确降级或失败策略，不能无声返回假空值。

### 3.4 类型化配置注册

建议接口：

```java
public interface ConfigDefinition<T> {
    String key();
    Class<T> type();
    T defaultValue();
    T parse(String rawValue);
    void validate(T value);
    boolean dynamic();
    boolean sensitive();
}
```

实现不必完全采用此签名，但必须满足同等语义。

读取流程：

1. 代码按 Key 找到定义。
2. 从 Spring Cache 读取已发布值。
3. 解析并校验。
4. 缺失时使用明确默认值。
5. 更新时先校验、再持久化、提交后失效缓存。

不允许通过任意字符串 Key 动态绑定 Spring Bean、数据源、Redis Codec 或密钥。

### 3.5 最小客户端模型

一期只有公开 `clientId`：

- `pc-admin`
- 无 `clientKey`
- 无 `clientSecret`
- 无授权类型

登录前：

1. 校验验证码和基础频率限制。
2. 校验 `clientId` 存在且启用。
3. 校验账号、密码和账号状态。
4. 使用账号、IP 和客户端维度记录失败窗口。

登录成功：

1. 服务端会话固定绑定 `clientId` 和 `deviceType`。
2. 同用户、同 `clientId` 只允许一个有效会话。
3. 同用户、不同 `clientId` 可以并存。
4. 保存会话索引和登录快照。
5. 返回 Token，不返回内部会话 Key。

并发不变量：

- 两个实例同时处理同用户、同客户端登录时，最终只能有一个有效会话。
- 新会话生效后，旧 Token 下一次访问必须失败。
- 不能先踢旧会话再因为新登录失败导致用户完全无会话。

具体 Sa-Token API、device 值和原子交换方式必须通过专项设计与并发测试确定，执行模型不得自行用 `synchronized` 代替多实例一致性。

### 3.6 在线用户

在线列表按会话而不是按用户聚合，至少包含：

- 会话标识（不可回显原始 Token）。
- 用户 ID、账号和昵称。
- `clientId`、设备类型。
- IP、归属地、浏览器、操作系统。
- 登录时间、最后活动时间、绝对过期时间。

强制下线：

- 只终止选中会话。
- 禁用/删除用户、关键权限变化可以终止该用户全部会话。
- 操作必须记录操作日志，但不得保存 Token。

### 3.7 日志元数据

一期操作日志保存：

- `traceId`
- 用户与部门快照
- `clientId`
- 模块、业务类型
- Java 方法、HTTP 方法、URI
- IP、归属地、浏览器、操作系统
- 响应状态、耗时
- 异常处理状态和有界异常堆栈

一期不保存请求参数、请求体或响应体。

登录日志保存：

- `traceId`
- 用户 ID、账号
- `clientId`、设备类型
- 登录、登出、踢下线等事件
- 成功/失败和固定原因码
- IP、归属地、浏览器、操作系统、User-Agent

IP 与设备：

- Hutool UserAgent 能力用于本地解析。
- ip2region 使用离线 XDB。
- 内网返回“内网 IP”，未知返回“未知”。
- 只信任配置过的反向代理。
- 解析和异步日志失败不能影响业务。

### 3.8 SQL 摘要监控

- 继续使用 MyBatis Interceptor。
- 有界内存队列，不写数据库。
- SQL 始终保留 `?` 占位符。
- 记录 statement ID、命令类型、表名、耗时、结果数量、成功/失败和 `traceId`。
- 开发默认全量；生产默认只保留慢 SQL 和失败 SQL。
- 删除所有 Druid 入口与字段。
- 不提供执行 SQL、查看真实参数或跨实例聚合。

### 3.9 文件存储

`StorageProvider` 继续表达：

- store
- open
- delete
- public URL 或受控访问能力

调整：

- Spring 自动收集 Provider，不在 `FileService` 构造器写死实现列表。
- `sys_file.storage_provider` 决定历史对象读取和删除所用 Provider。
- 开发 profile 默认 local。
- Compose smoke 和生产推荐 MinIO。
- 切换默认 Provider 只影响新文件。
- 不自动迁移历史对象。

连接、Bucket 和密钥走环境变量；上传大小、类型白名单、私有访问期限等业务规则在配置注册机制完成后接入 `sys_config`。

## 4. 二期设计

### 4.1 数据脱敏

- VO 字段使用明确脱敏注解。
- Jackson Contextual Serializer 根据固定权限判断。
- 策略或权限服务失败时默认脱敏。
- 超级管理员也通过明确权限获得业务原文，不使用隐式绕过。
- 密码、Token 和密钥不进入 VO，不能依赖脱敏注解补救。

### 4.2 操作日志参数详情

只有安全评审通过后启用：

- 普通管理写操作可以分别配置请求与响应摘要。
- 递归删除凭据字段，业务个人数据只保存脱敏值。
- 登录、密码、验证码、Token、文件和密钥接口硬性禁用内容采集。
- 请求和响应分别设置硬上限，超限标记截断。
- 查询列表响应只保存数量与状态，不保存记录集合。
- 清理器异常时放弃内容，不回退保存原文。
- 查看详情需要独立权限，并记录查看审计。

### 4.3 缓存数据分级

每个登记缓存定义：

- `HIDDEN`
- `MASKED`
- `PLAIN`

认证、验证码、在线状态、失败计数、锁和密钥永久 `HIDDEN`。普通业务缓存可以在受控配置中调整。明文查看单独授权并审计，但审计日志不保存明文值。

## 5. 三期设计

### 5.1 应用内定时任务

- 默认使用 Spring 定时能力，不增加独立调度服务。
- 日志清理任务分批删除，支持 dry-run，并输出扫描数、删除数、跳过数和耗时。
- 登录日志、成功操作日志默认 180 天；已处理异常 365 天；未处理异常不自动删除。

### 5.2 SnailJob 重评

SnailJob 当前为 `DEFER`。只有应用内任务不能满足真实的多实例协调、可靠重试或可视化
运维时，才补齐失败链路、重试、恢复和退出成本证据。重新评审为 `GO` 后才允许创建
独立 Server、Schema 和 Executor 配置。

### 5.3 Lock4j 重评

Lock4j 当前为 `DEFER`。只有出现真实并发业务入口、现有约束和直接 Redisson 锁不足，
并重新评审为 `GO` 后才引入：

- 注解或 LockTemplate 只在 Service 业务入口。
- 锁键统一命名空间。
- 禁止为普通 CRUD 机械加锁。
- 必须有并发、超时、异常释放和多实例测试。

### 5.4 ID 决策

正式业务数据进入前完成：

- 若继续自增，记录多实例下数据库生成主键的接受理由。
- 若使用 Snowflake，明确 worker/datacenter 分配、Long 字符串契约、测试数据和数据重建方案。
- 当前数据可丢弃时优先重建；已有正式数据时另建 ID 映射需求，不夹带实施。

## 6. 多数据源扩展边界

一期不引入动态数据源：

- 数据源配置集中于 `framework/mybatis`。
- Service 只依赖 Mapper 和 `@Transactional`。
- Mapper 按领域分包。
- 业务代码不读取数据源名或 Hikari 对象。
- Flyway 只管理主业务库。

真实第二数据源出现后，再定义独立 Mapper 扫描、事务管理器和迁移位置。

## 7. 后端完成标准

- 没有旧技术残留、双写和兼容分支。
- 每个状态迁移和失败路径有聚焦测试。
- 真实 HTTP Bearer 测试覆盖 `clientId` 会话。
- Redis 迁移测试覆盖 TTL、集合、并发和旧会话失效。
- 日志测试证明硬禁止字段不落库。
- local 与 MinIO 都完成真实上传、预览、删除和切换验证。
- 正式规范只在行为落地后同步更新。
