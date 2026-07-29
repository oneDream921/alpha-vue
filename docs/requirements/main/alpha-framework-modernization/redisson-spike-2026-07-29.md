# S0-02 Redisson 兼容性 Spike

## 1. 执行摘要

| 项目 | 结果 |
| --- | --- |
| 执行日期 | 2026-07-29 |
| 目标环境 | Java 21、Spring Boot 4.0.0、Sa-Token 1.45.0、Redis 7 |
| 推荐版本 | Redisson 4.6.1 |
| 推荐接入 | 直接使用 `redisson` 与 `redisson-spring-cache` |
| 不推荐接入 | `redisson-spring-boot-starter` |
| 决策 | `GO WITH CONSTRAINTS`，按计划归入 `GO`，已获用户确认 |

本次只在 `/tmp/alpha-vue-redisson-spike` 和独立 Redis DB 中验证，没有修改生产代码、
配置、数据库或业务 Redis 数据。

结论不是“Starter 能启动即可采用”，而是：

1. Redisson 4.6.1 的直接 Client 路线可在当前 Spring Boot 4.0.0 基线上工作。
2. 全局 Client 默认使用 `StringCodec`，不得使用无约束通用对象 Codec。
3. Spring Cache 与 Sa-Token 对象分别使用显式类白名单的 `Kryo5Codec`。
4. 现有 JDK 序列化数据不兼容读取，正式迁移采用停机、定向清理和全会话失效。
5. 完整 Alpha Server 的依赖、故障恢复和真实认证回归仍属于 P1-03 验收，不由本 Spike
   代替。

## 2. 当前实现边界

当前 Alpha Redis 实现包含：

- Spring Data Redis 4.0.0 与 Lettuce 6.8.1；
- `RedisTemplate<String, Object>` 加 `JdkSerializationRedisSerializer` 保存 Sa-Token 对象；
- `StringRedisTemplate` 保存验证码、登录失败次数和系统配置；
- Jackson 2 JSON 字符串保存字典缓存；
- Lettuce 原生命令实现 Redis 监控。

已识别的旧 Key 前缀：

| 前缀 | 数据 |
| --- | --- |
| `auth:captcha:*` | 验证码 |
| `auth:login:failure:*` | 登录失败计数 |
| `system:config:*` | 系统配置缓存 |
| `system:dict:*` | 字典缓存 |
| `satoken:*` | Sa-Token 会话、Token 映射和索引 |

这些前缀并不属于 `alpha:*`。正式迁移不能只清理新命名空间。

## 3. 版本与接入方式

Redisson 官方文档声明其 Spring Boot Starter 支持 Spring Boot 4.0.x，并提供独立的
Spring Cache 集成，参见 [Redisson Spring integration](https://redisson.pro/docs/integration-with-spring/)。

实际验证结果：

| 方案 | 结果 | 决策 |
| --- | --- | --- |
| Starter 4.6.1 | Boot 4.0.0 可启动并连接 Redis | 不采用 |
| 直接 Client 4.6.1 | 8 项核心测试通过 | 采用 |
| Redisson 4.0.0 | 无对应版本的 `redisson-spring-cache` | 淘汰 |
| Redisson 4.1.0 | 通用 Jackson 3 对象回读仍失败 | 淘汰 |

Starter 4.6.1 会引入：

```text
redisson-spring-boot-starter
├── spring-boot-starter-data-redis
└── redisson-spring-data-41
    └── spring-data-redis
```

直接接入的目标依赖为：

```text
redisson:4.6.1
redisson-spring-cache:4.6.1
```

验证依赖树中没有 Spring Data Redis、Lettuce 和 Jackson 2。HTTP 仍使用 Boot 4.0.0
管理的 Jackson 3.0.2。

## 4. Codec 决策

### 4.1 已淘汰方案

| Codec | 结果 | 原因 |
| --- | --- | --- |
| `JsonJackson3Codec` | 失败 | `Object`、final record、不可变 List 和 SaSession 无法稳定回读 |
| `JsonJacksonCodec` | 失败 | 显式引入 Jackson 2 后仍有相同的异构对象类型问题 |
| 无参 `Kryo5Codec` | 功能通过，安全淘汰 | 未要求类注册，形成不受控反序列化边界 |

Redisson 的 Codec 类型及默认选择见
[Data serialization](https://redisson.pro/docs/data-and-services/data-serialization/)。
无约束 Kryo 反序列化还存在已知安全风险，参见
[GitHub Security Lab advisory](https://securitylab.github.com/advisories/GHSL-2023-053_Redisson/)。

### 4.2 推荐分层

| 场景 | Codec | 约束 |
| --- | --- | --- |
| Client 默认、验证码、计数、配置字符串 | `StringCodec` | 禁止默认通用对象反序列化 |
| Sa-Token String API | `StringCodec` | `get/set/update/delete` 必须显式覆盖 |
| Sa-Token Object API | 白名单 `Kryo5Codec` | 只登记 Sa-Token 与实际集合类型 |
| Spring Cache | 独立白名单 `Kryo5Codec` | 只登记已批准缓存 DTO |
| 业务 Hash/List | 按领域显式 Codec | 不继承隐式全局对象 Codec |

反向测试已证明：未登记对象写入会被拒绝。

白名单不是一次性常量。Sa-Token、Redisson、JDK 或缓存 DTO 升级时，必须先运行
“旧版本写入、新版本读取”兼容测试；缓存命名应包含格式版本，并仅保存可整体失效的
短生命周期数据。

## 5. 验证结果

最终 Reactor 共执行 9 项测试：

| 模块 | 数量 | 结果 |
| --- | ---: | --- |
| Starter 对照组 | 1 | 通过 |
| 直接 Client 候选方案 | 8 | 通过 |

直接 Client 覆盖：

- Boot 4 Jackson 3 与 Redisson 对象 Codec 隔离；
- 白名单允许登记类型并拒绝未登记类型；
- String、Hash、List 和 TTL；
- 10 路并发 Lua 登录失败次数预留，最多接受 5 次；
- `RLock` 并发互斥和释放后重新获取；
- Spring Cache 字符串 TTL；
- Spring Cache 对象、禁止缓存空值、`evict`、`clear` 和不同 TTL 配置；
- Sa-Token String、Object、List、SaSession、TTL、`size=-1` 检索；
- Sa-Token 创建登录会话、按 Token 取得登录 ID、注销后 Token 失效；
- 更新不存在的对象不会复活 Key，更新已有对象保留 TTL；
- 临界 `PTTL == 0` 或已过期时拒绝更新，避免会话变成永久 Key；
- `timeout <= 0` 和永久有效语义。

最终命令：

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn \
  -f /tmp/alpha-vue-redisson-spike/pom.xml clean test
```

结果：`BUILD SUCCESS`，Starter 1 项、直接 Client 8 项，0 失败、0 错误。

## 6. Sa-Token DAO 正式实现约束

正式实现不得复制一个“所有方法都走 Object Codec”的万能 DAO。必须满足：

1. String API 与 Object API 使用不同 Codec。
2. `update` 仅在 Key 存在时执行，并原子保留原 TTL，防止注销并发下复活会话。
   原 TTL 为 `-1` 时保持永久，正数时恢复原 TTL，`0`、`-2` 或其他无效值时拒绝更新。
3. `NEVER_EXPIRE`、`NOT_VALUE_EXPIRE`、`timeout=0` 和正 TTL 语义分别处理。
4. `searchData` 使用有界 `SCAN`，支持 `size=-1`，不使用 `KEYS`。
5. 新版 Redisson 正式实现使用非弃用扫描 API。
6. 登录 ID 按 Sa-Token 实际契约作为字符串读取，不假设返回 `Long`。
7. 生产集成测试覆盖登录、鉴权、注销、踢下线、会话 TTL 和并发更新。

## 7. 迁移与回滚

### 7.1 上线迁移

1. 停止所有旧 Alpha 实例，禁止 Lettuce/JDK 与 Redisson 新格式并行写入。
2. 备份并记录 Alpha 自有旧前缀数量，只允许前缀白名单。
3. 使用 `SCAN` 加 `UNLINK`，必要时退化为 `DEL`，定向清理第 2 节列出的旧 Key。
4. 禁止使用 `FLUSHDB` 或 `FLUSHALL`，避免影响共享 Redis 中的其他应用。
5. 新 Key 统一使用 `alpha:<domain>:<purpose>:<identifier>`。
6. 所有旧登录会话失效，用户重新登录。
7. 启动新实例后验证登录、验证码、失败计数、字典、配置和在线用户。

### 7.2 回滚

1. 停止新实例。
2. 定向清理新 `alpha:*` 中由本次迁移登记的 Redis 前缀。
3. 恢复旧版本；不得让旧 JDK DAO 尝试读取 Kryo 数据。
4. 用户再次登录。

不做双写、不做旧 JDK 数据兼容读取，也不在共享 Redis 上执行全库清理。

## 8. 正式接入的剩余验收

以下事项不阻断 S0-02 技术路线结论，但必须在 P1-03 完成：

- 在完整 `alpha-server` 依赖树中复核 Netty、Reactor 和 Spring Framework 版本；
- 使用 Spring Boot 4.0.0 BOM 管理传递依赖，不私自覆盖到 Redisson 自身构建版本；
- 验证连接失败、Redis 重启、命令超时和 Client 关闭；
- 按实际部署需要验证密码、TLS 或集群配置；
- 提供健康检查、指标和脱敏后的连接诊断；
- 将临时 DAO 转为领域 Adapter，并补完整后端测试与真实 HTTP 回归；
- 执行迁移前缀盘点脚本的只读预演和回滚演练。

若完整应用出现二进制不兼容、白名单无法收敛或必须长期并存两套 Redis Client，
P1-03 必须停止并将本决策改为 `DEFER` 或 `REPLACE`。

## 9. 环境恢复

- 临时容器 `alpha-redisson-spike-redis` 已停止并删除；
- 临时目录 `/tmp/alpha-vue-redisson-spike` 已删除；
- 本地端口 `26379` 已释放；
- 生产代码、配置和业务 Redis 数据未修改。

## 10. 最终结论

S0-02 结论为 `GO WITH CONSTRAINTS`，在当前计划三态中归入 `GO`：

- 允许后续 P1-03 按本文方案设计 Redisson 正式接入；
- 不允许直接采用 Starter；
- 不允许无参 `Kryo5Codec`；
- 不允许兼容读取旧 JDK 序列化；
- 不代表 G0 已通过；
- 已于 2026-07-29 获得用户确认，S0-02 关闭；
- 不代表 G0 已通过，也未自动开始 S0-03。
