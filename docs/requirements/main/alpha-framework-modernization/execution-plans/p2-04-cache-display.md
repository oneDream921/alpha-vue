# P2-04 缓存展示分级执行计划

## 目标与边界

为 Redis 运维台的值预览建立统一的 `HIDDEN`、`MASKED`、`PLAIN` 展示级别。默认普通缓存按 `MASKED` 处理，认证、验证码、失败计数、会话和疑似密钥默认按 `HIDDEN` 处理；已注册敏感命名空间可通过受控配置调整，未知疑似密钥仍强制 `HIDDEN`。

本任务不改变业务缓存读写、不反序列化 Redis 对象、不开放任意 Redis 命令，也不新增缓存专用数据表。配置复用现有 `sys_config`，仅提供代码注册的字典缓存和普通业务缓存定义。

## 实施结果

1. 在 `modules/monitor` 增加展示级别枚举和缓存策略注册表。
2. 统一 Redis 列表与详情的策略决策，避免底层 Redis 适配器和业务服务出现不同脱敏结果。
3. 新增 `cache.display.dictionary` 与 `cache.display.business` 两个已发布 ENUM 配置定义，默认值均为 `MASKED`。
4. 已注册敏感命名空间默认 `HIDDEN`，允许受控配置切换到 `MASKED` 或 `PLAIN`；全局 `REDIS_MASK_VALUES=true` 时非隐藏值至少为 `MASKED`，未知敏感键仍强制 `HIDDEN`。
5. 前端 Redis 键列表和详情抽屉显示“完全隐藏 / 已脱敏 / 明文”状态标签。

## 验证矩阵

| 类别 | 证据 |
| --- | --- |
| 后端 | Redis 展示策略单测、敏感命名空间默认隐藏但可配置、未知敏感键强制隐藏、接口字段和权限回归 |
| 前端 | Redis service 类型、页面标签、typecheck、test、lint、format、build |
| 数据库 | Flyway V23 在 H2 测试环境成功执行，配置定义为 ENUM 且默认 `MASKED` |
| 浏览器 | 本地真实登录后 Redis 键管理页显示 `HIDDEN` 与 `MASKED` 标签；1440、1024、390 视口无横向溢出 |

## 当前状态

状态：`COMPLETED`

敏感缓存说明：验证码、登录失败窗口和 Sa-Token 会话分别由 `cache.display.captcha`、
`cache.display.login-failure`、`cache.display.session` 控制，默认仍为 `HIDDEN`，可按需改为
`MASKED` 或 `PLAIN`。当 `REDIS_MASK_VALUES=true` 时，非隐藏级别会被上限保护为 `MASKED`；
未注册但命中 token、secret、password 等敏感特征的键仍强制 `HIDDEN`。

自动化检查、本地真实页面检查和用户人工验收已完成；G3-M04 已记录为通过。`PLAIN` 的服务端行为由自动化测试覆盖，当前本地实例保留全局脱敏开关，因此页面中不展示明文样例。`HIDDEN` 只显示“完全隐藏”，不会误显示“已截断”。

## 回退与风险

将展示级别配置恢复为默认值即可回退；Sa-Token 会话使用 Kryo 序列化二进制，通常应保持 `HIDDEN` 或 `MASKED`，配置为 `PLAIN` 可能看到乱码，不代表数据损坏。若 Redis 管理页面异常，可继续使用已有键元数据、SCAN、单键删除权限和审计能力。
