# S0-04 候选能力兼容性 Spike

## 1. 执行摘要

| 候选项 | 验证版本 | 技术结论 | 阶段决策 |
| --- | --- | --- | --- |
| Spring Boot Admin | 4.0.4 | `GO WITH CONSTRAINTS` | 归入 `GO`，只允许第二期独立部署 |
| SnailJob | 2.0.2 | Boot 4 成功链路 `PASS` | `DEFER`，出现调度刚需后补失败链路 |
| Lock4j | 2.2.7 | `DEFER` | 不进入基线，优先直接封装 Redisson |
| VXE Table | 4.20.9，VXE PC UI 4.16.23 | `GO WITH CONSTRAINTS` | 归入 `GO`，只允许第二期单页试点 |

执行日期为 2026-07-29，目标基线为 Java 21、Spring Boot 4.0.0、Vue 3.5 和
Ant Design Vue 4.2。

本次所有验证都在 `/tmp/alpha-vue-s0-04` 隔离目录以及临时 Docker 容器中完成，
没有修改 Alpha 生产代码、运行配置、数据库、Redis 数据或依赖。

本结论区分技术兼容与架构采用，不是“能运行就加入第一期”：

- Spring Boot Admin 不进入业务单体；
- SnailJob 不进入第一期或第二期；
- Lock4j 不加入依赖；
- VXE 不全量替换 Ant Design Vue Table；
- S0-04 不代表 G0 已通过，也不自动开始 S0-05。

## 2. 版本、许可证与维护状态

| 候选项 | 许可证 | 兼容与维护依据 | 判断 |
| --- | --- | --- | --- |
| Spring Boot Admin 4.0.4 | Apache-2.0 | 官方兼容矩阵将 4.0.x 对应 Spring Boot 4.0.x；4.0.4 文档完整 | 可采用同 Boot 次版本线 |
| SnailJob 2.0.2 | Apache-2.0 | 2026-07-24 标签源码直接使用 Spring Boot 4.0.3、Java 21 | 当前活跃，但运维成本高 |
| Lock4j 2.2.7 | Apache-2.0 | 当前版本父工程仍基于 Spring Boot 2.7.7、Java 8，发布基线明显偏旧 | 功能可用，不适合作为 Alpha 新基线 |
| VXE Table 4.20.9 | MIT | 官方 V4 文档要求 Vue 3.2+；版本发布活跃 | 兼容，但体积与双 UI 栈成本显著 |
| VXE PC UI 4.16.23 | MIT | 与 VXE Table V4 配套 | 只按需引入 |

官方资料：

- [Spring Boot Admin 兼容矩阵与许可证](https://github.com/codecentric/spring-boot-admin)
- [Spring Boot Admin 4.0.4 客户端注册](https://docs.spring-boot-admin.com/4.0.4/docs/getting-started/client-registration/)
- [Spring Boot Admin Actuator 安全](https://docs.spring-boot-admin.com/4.0.4/docs/security/actuator-security/)
- [Spring Boot Admin CSRF 边界](https://docs.spring-boot-admin.com/4.0.4/docs/security/csrf-protection/)
- [SnailJob 服务端部署](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html)
- [SnailJob 执行器](https://snailjob.opensnail.com/docs/guide/job/job_executor.html)
- [Lock4j 官方仓库](https://github.com/baomidou/lock4j)
- [VXE UI V4 官方文档](https://vxeui.com/)
- [VXE Table 官方仓库](https://github.com/x-extends/vxe-table)

不能用 RuoYi Plus 是否采用某组件代替上述版本和运行证据。

## 3. Spring Boot Admin

### 3.1 验证设计

隔离创建两个最小应用：

```text
/tmp/alpha-vue-s0-04/sba/
├── admin/     # Spring Boot Admin Server，端口 26180
└── client/    # Spring Boot Actuator Client，端口 26181
```

Server 与 Client 均使用 Spring Boot 4.0.0，Admin 使用 4.0.4。验证采用：

- Admin Server 表单和 Basic Authentication；
- Client Actuator Basic Authentication；
- Client 注册元数据携带访问凭据；
- Admin 仅忽略注册、实例和 Actuator 所需 CSRF 路径；
- Client 只暴露 `health`、`info`、`metrics`、`loggers`；
- 未暴露 `env`。

### 3.2 实际结果

两个应用均 `clean package` 成功，并完成真实双进程注册：

| 检查 | 结果 |
| --- | --- |
| Client 注册状态 | 实例 `3032465497a0`，状态 `UP` |
| Admin `/instances` 未认证 | HTTP 401 |
| Admin `/instances` 已认证 | HTTP 200 |
| Client `/actuator/health` 未认证 | HTTP 401 |
| Client `/actuator/health` 已认证 | HTTP 200 |
| Client `/actuator/env` 已认证 | HTTP 404 |
| Admin UI 已认证 | HTTP 200 |
| Admin 代理读取 Client health | 未认证 401，已认证 200 |
| 注册元数据中的密码 | 返回 `******` |

Spring Boot 4 中 `EndpointRequest` 的包已变为
`org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest`。
正式实现不得复制 Spring Boot 3 的旧 import。

### 3.3 决策约束

结论为 `GO WITH CONSTRAINTS`，按计划归入 `GO`：

1. 只在第二期创建独立 `alpha-extensions/monitor-admin` 进程。
2. Admin 故障不得影响 `alpha-server` 启动和业务请求。
3. Actuator 暴露白名单、独立凭据、网络访问控制和生产 TLS 必须同时落地。
4. 禁止暴露 `env`、`configprops`、`heapdump`、`shutdown` 等高风险端点。
5. 注册元数据掩码不等于传输安全，凭据仍必须通过部署 Secret 提供。
6. 第二期若现有 Actuator 指标已足够，允许继续延期，不因 Spike 通过强制引入。

## 4. SnailJob

### 4.1 验证设计

使用官方 `vsj2.0.2` 标签源码，提交：

```text
cacc2b698c1880552d4b0c3582b44713dc350566
```

验证环境：

- Server：SnailJob 2.0.2、Spring Boot 4.0.3、Java 21；
- Client：Spring Boot 4.0.0、SnailJob Client 2.0.2；
- MySQL：独立 `mysql:8.0.42` 容器，宿主端口 26306；
- 管理端 HTTP：26300；
- Server gRPC：26888；
- Client HTTP：26301；
- Client gRPC：26889；
- 隔离 namespace、group 和 token。

Client 提供 `alphaSpikeJob` 执行器，并通过官方 OpenAPI 创建固定时间任务后立即触发。

### 4.2 源码构建

执行：

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn \
  -pl snail-job-server-starter -am clean package -DskipTests
```

结果：

- 24 个 Reactor 模块全部成功；
- 总耗时 4 分 42 秒；
- 生成 116 MB 的 `snail-job-server-exec.jar`；
- UI 构建步骤复用了源码仓内预构建资源；
- 独立管理界面 `/snail-job/` 返回 HTTP 200。

这证明 Boot 4 可构建和运行，也说明它不是一个轻量 starter，而是一套独立调度平台。

### 4.3 真实任务链路

Boot 4.0.0 Client `clean package` 成功并完成：

1. Client 连接 Server；
2. Client gRPC 服务启动；
3. `alphaSpikeJob` 执行器注册；
4. OpenAPI 创建任务，返回 `jobId=1`；
5. OpenAPI 立即触发返回 `triggered=true`；
6. 执行器计数从 0 变为 1；
7. Client 日志记录任务执行成功；
8. Server 日志记录调度成功；
9. MySQL 任务、批次、任务明细和执行器记录全部落表。

数据库证据：

| 表 | 关键结果 |
| --- | --- |
| `sj_job` | `Alpha Spike Job`，`alphaSpikeJob`，启用 |
| `sj_job_task_batch` | 批次状态 3，执行完成 |
| `sj_job_task` | `CLUSTER_TASK`，任务状态 3 |
| `sj_job_executor` | `alphaSpikeJob`，Java 执行器 |

### 4.4 未覆盖边界

本次任务显式设置 `maxRetryTimes=0`，只证明注册、创建、手动触发、成功执行和落表。
没有证明：

- 失败重试和退避；
- 幂等与重复执行；
- 执行超时和强制终止；
- Server 或 Client 停机后的错过调度恢复；
- 失败日志保留和清理；
- 管理端鉴权与任务参数安全。

因此它满足“Boot 4 成功链路兼容”判断，但没有完成目标架构中包含失败重试的正式接入闸门。

### 4.5 决策约束

技术兼容性为 `PASS`，架构阶段决策为 `DEFER`：

1. 使用 SnailJob 自带独立管理界面，不在 Alpha 前端复制调度控制台。
2. Server、数据库 Schema、端口、凭据、备份和升级均由独立运维边界负责。
3. Alpha 只接入 Client 和业务执行器，不把 Server 嵌入业务进程。
4. 出现多实例调度、可靠重试或可视化运维刚需后，才允许重新启动正式评估。
5. 重新评估必须先补失败重试、幂等、超时、停机重启、错过调度和日志清理测试。
6. 必须明确任务执行权限、参数白名单和敏感信息边界。
7. 第一、二期继续使用 Spring `@Scheduled` 承担少量本地维护任务。

## 5. Lock4j

### 5.1 验证设计

隔离应用使用：

- Spring Boot 4.0.0；
- Lock4j 2.2.7；
- 直接创建 Redisson 4.0.0 Client；
- 独立 Redis 7.2.8 容器。

没有使用 Alpha 现有 Redis 凭据，也没有访问业务 Key。

### 5.2 自动化结果

执行：

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn \
  -f /tmp/alpha-vue-s0-04/lock4j/pom.xml clean test
```

3 项测试全部通过：

1. Boot 4 自动配置可创建 `LockTemplate`；同 Key 并发最大活动数为 1；
   获取超时抛出 `LockException`。
2. 业务方法抛异常后锁会释放，后续调用可以成功获取。
3. `autoRelease=false`、300 ms 过期时，其他线程先被阻断，约 450 ms 后可获取。

首轮连接现有 Redis 因需要认证而失败。验证没有读取现有密码，而是切换到独立临时
Redis 后通过。这是环境隔离修正，不是 Lock4j 兼容问题。

### 5.3 决策

结论为 `DEFER`：

- 功能兼容不等于适合成为长期基线；
- 2.2.7 的构建基线仍是 Spring Boot 2.7.7、Java 8；
- Alpha 第一期没有必须跨实例互斥的业务场景；
- Redisson 自身已经提供 `RLock`，再增加注解层会扩大隐式行为和版本耦合。

出现真实跨实例锁需求时，优先为明确业务场景编写可注入的 Redisson 锁适配器，显式处理
锁 Key、等待时间、租约、续期、线程所有权和失败语义。只有直接适配产生稳定重复规则后，
才重新比较 Lock4j，并重跑 Boot 4、Redisson 当前版本和故障恢复测试。

## 6. VXE Table

### 6.1 验证设计

隔离 Vue 应用同时安装：

- Vue 3.5.40；
- Vite 7.3.6；
- TypeScript 5.8.3；
- Ant Design Vue 4.2.6；
- VXE Table 4.20.9；
- VXE PC UI 4.16.23。

最终验证配置直接复用了 Alpha 的关键构建边界：

- UnoCSS 66.7.5 与 `presetWind4`，关闭第二套全局 reset；
- `unplugin-vue-components` 与 `AntDesignVueResolver`；
- Alpha 的 Vue vendor 拆包规则；
- `defineAsyncComponent` 异步加载试点页面；
- Ant Design Vue 生产组件自动导入；
- VXE Table 和 Column 在页面内局部导入；
- VXE 样式随异步页面拆包，不在 `main.ts` 全局注册完整 VXE UI。

试点复刻 Alpha 操作日志宽表：

- Ant Design Vue 提供搜索、按钮、徽标、分页、Popover 和 Checkbox；
- VXE 提供排序、固定列、可调列宽和宽表滚动；
- 9 个业务列可显示或隐藏；
- 列设置保存到具名 localStorage Key；
- 390 px 移动端不删除功能。

本次验证值仍是无 schema 包装的字符串数组。正式试点必须改为
`{ version: 1, columns: [...] }`，不能把当前临时格式当成跨版本契约。

### 6.2 自动化与构建

最终结果：

| 检查 | 结果 |
| --- | --- |
| 列设置默认值、保存、非法值处理 | 3 项测试通过 |
| Ant Design Vue 与 VXE 共存 | 1 项测试通过 |
| 组件生命周期恢复列设置 | 1 项测试通过 |
| `vue-tsc --noEmit` | 通过 |
| UnoCSS shortcut 实际生效 | 浏览器计算样式为 `display:flex; justify-content:space-between` |
| Vite 生产构建 | 通过，3468 modules |

首轮测试因 Node 的实验性全局 `localStorage` 不可用而失败，改为显式
`window.localStorage` 并在 jsdom 提供标准 Storage 测试替身后通过。

首次类型检查还暴露两个构建约束：

- 必须包含 Node 类型；
- 第三方 UI 类型需要沿用 Alpha 的 `skipLibCheck` 边界。

这些失败与修正说明正式试点必须复用 Alpha 的 tsconfig 和测试环境，不能复制一个简化
示例配置后直接判断兼容。

### 6.3 浏览器结果

真实浏览器验证：

| 视口 | 结果 |
| --- | --- |
| 1440 x 900 | 页面无横向溢出，宽表、固定用户列、排序入口和筛选区正常 |
| 390 x 844 | 页面本身无横向溢出；表格内部从 364 px 滚动到 1580 px |
| 列设置 | 隐藏 `Trace ID` 后立即消失，刷新后仍保持隐藏 |
| 隐藏后移动宽表 | 内部滚动宽度从 1580 px 降为 1290 px |
| 控制台 | 桌面和移动端均无 error 或 warning |

### 6.4 体积与维护成本

首轮全量全局注册得到 2670.12 KB JavaScript 和 567.23 KB CSS，只能作为错误接入方式的
成本上界。根据独立强审查意见改为 Alpha 风格的异步页面、局部组件和 UnoCSS 后，最终
产物为：

| 产物 | 原始大小 | gzip |
| --- | ---: | ---: |
| 应用入口 JavaScript | 97.42 KB | 34.20 KB |
| Vue vendor | 78.36 KB | 31.03 KB |
| VXE 操作日志异步 JavaScript | 822.99 KB | 250.42 KB |
| 应用入口 CSS | 5.30 KB | 1.98 KB |
| VXE 操作日志异步 CSS | 141.38 KB | 21.79 KB |

安装目录实际大小：

- `vxe-table`：约 15.2 MB；
- `vxe-pc-ui`：约 28.2 MB。

Alpha 当前完整 `dist` 约 2.8 MB，且有多个业务路由；隔离应用只有一个页面。两者不能
直接做总量差值。最终结果证明 VXE 可以不进入首屏入口，但单个页面仍增加约 250 KB gzip
JavaScript 和 22 KB gzip CSS，成本不低。正式试点仍须在完整 Alpha 中记录真实路由增量。

### 6.5 决策约束

结论为 `GO WITH CONSTRAINTS`，按计划归入 `GO`：

1. 第一期继续使用 Ant Design Vue Table。
2. 第二期只选择一个列多、固定列和列设置收益明确的复杂列表。
3. 不先建设 `AlphaGrid`，不批量改造列表，不引入 CRUD DSL。
4. VXE 必须路由级异步加载并局部引入 Table/Column，禁止在 `main.ts` 全局注册完整 UI。
5. 列偏好先使用带 schema 版本的本地存储；跨设备同步另立任务。
6. 试点必须补真实分页、服务端排序、权限列、空态、错误态和触屏横向滚动。
7. 若试点后的路由 gzip 增量、交互一致性或测试成本不合格，撤回并继续使用 Ant Table。

## 7. 错误与限制追溯

| 候选项 | 遇到的问题 | 处理 | 是否改变结论 |
| --- | --- | --- | --- |
| Spring Boot Admin | Boot 4 `EndpointRequest` 包路径变化 | 使用 Boot 4 新包 | 否，形成正式约束 |
| SnailJob | 只验证成功任务，且重试次数为 0 | 技术兼容记 `PASS`，阶段决策降为 `DEFER` | 是 |
| Lock4j | 现有 Redis 要求认证 | 不读取凭据，启动独立 Redis | 否 |
| VXE | pnpm 默认阻止依赖构建脚本 | 首轮临时放宽后，经审查改为只允许 `core-js`、`esbuild` | 否，正式仓必须逐包批准 |
| VXE | Node 全局 localStorage 与 jsdom 冲突 | 显式浏览器边界和测试 Storage | 否 |
| VXE | 类型检查缺 Node 类型且第三方声明冲突 | 对齐 Alpha tsconfig 边界 | 否 |
| VXE | 首轮未使用 UnoCSS 且全局注册双 UI 栈 | 补 UnoCSS、组件自动导入、异步页面和局部 VXE 导入 | 是，补证后保留受限 `GO` |
| 浏览器验证 | `networkidle` 在当前控制接口不支持 | 使用 DOM、指标和截图直接验证 | 否 |

### 7.1 轻量证据清单

以下 SHA-256 只覆盖各隔离工程的自建 `pom.xml`、前端 Manifest、配置和 `src`，不包含
第三方源码、`node_modules`、`target`、`dist` 或 116 MB JAR：

| 工程 | SHA-256 |
| --- | --- |
| SBA Admin | `b10df7793e5cba49abf8b924030f25b4cdf890e92f927a856fadd570eec32b22` |
| SBA Client | `a60d487d727bede2de721e5b49a7b6b803f8d2bc99781529ff710585b2d828ad` |
| Lock4j | `e95591c4bb06e25551f5843103811fdcc24ea6410e15929882ff3e0e9575d906` |
| SnailJob Client | `f3ba54f703e9e8a18b0ab57bf6e5c072b2d00546483dbf62caf96be7114e9e64` |
| VXE | `b076e0aa265ca1479a487f3c5fe0b528be6deff616be9fe73193abfc0801edbe` |

这些是实际执行时自建源码的清单哈希。可恢复源码另存为
[S0-04 Spike 源码归档](./evidence/s0-04-spike-sources-2026-07-29.tar.gz)，归档
SHA-256 为：

```text
30f0a2888718728506edbc6f8d7e51754f84a5eebcc440e3d10dcac4d441afee
```

归档包含 34 个自建文件。为避免把临时明文凭据带入仓库，归档前只将 Basic 密码和
SnailJob group token 替换为环境变量占位符；测试逻辑、Fixture、依赖和构建配置均保留。
清单、校验和恢复命令见 [证据说明](./evidence/README.md)。

哈希计算方式：

```bash
find <spike-dir> -type f \
  \( -path '*/src/*' -o -name 'pom.xml' -o -name 'package.json' \
  -o -name 'pnpm-lock.yaml' -o -name 'pnpm-workspace.yaml' \
  -o -name 'vite.config.ts' -o -name 'tsconfig.json' \
  -o -name 'uno.config.ts' -o -name 'index.html' \) \
  ! -path '*/target/*' ! -path '*/node_modules/*' ! -path '*/dist/*' \
  -print0 | sort -z | xargs -0 shasum -a 256 | shasum -a 256
```

完整复现入口：

| 候选项 | 构建或测试命令 | 运行验证摘要 |
| --- | --- | --- |
| SBA | Admin、Client 分别执行指定 Maven `clean package` | 双 JAR 启动后校验注册、401/200、端点白名单和密码掩码 |
| SnailJob | Server 执行 `-pl snail-job-server-starter -am clean package -DskipTests`；Client 执行 `clean package` | 独立 MySQL、Server、Client 启动后创建并触发任务，再查询四张表 |
| Lock4j | `mvn -f /tmp/alpha-vue-s0-04/lock4j/pom.xml clean test` | 3 项自动化测试 |
| VXE | `pnpm install --frozen-lockfile=false && pnpm test && pnpm build` | 5 项测试；1440 x 900、390 x 844 浏览器验证 |

SBA 运行验证使用临时 Basic 凭据；SnailJob 使用临时数据库和 group token。文档和归档
不固化这些临时明文值，复现时通过环境变量重新生成。测试场景、关键输出、失败原因和修正
均在各候选章节记录。证据归档不参与生产构建，防止被误当成正式模块。

## 8. 对后续阶段的影响

### 第一期

不新增本报告四项依赖。第一期仍聚焦：

- HikariCP；
- SpringDoc/Therapi；
- Redisson 核心迁移；
- 类型化配置；
- clientId 会话闭环；
- 日志、SQL 摘要和存储边界。

### 第二期

允许两个独立任务进入排期评审：

1. Spring Boot Admin 独立扩展；
2. VXE 单复杂列表试点。

两者都不是第二期必做项，必须再次证明业务收益。

### 第三期

SnailJob 当前为 `DEFER`。只有本地维护任务已经不能满足多实例协调、可靠重试或可视化
运维需求时才重新执行失败链路 Spike 和正式设计。

Lock4j 不进入任何已排期阶段，只保留重新评估触发条件。

## 9. 环境恢复要求

已完成：

- [x] 所有 Spring Boot Admin、SnailJob、Lock4j 和 Vite 临时进程已停止；
- [x] `alpha-s0-04-*` 临时 Docker 容器已删除；
- [x] 26180、26181、26300、26301、26306、26320、26379、26888、26889 端口已释放；
- [x] `/tmp/alpha-vue-s0-04` 及证据打包中间目录已删除；
- [x] 可恢复的脱敏源码归档已保存并校验 34 个文件；
- [x] Alpha Git 工作区只包含需求文档修改；
- [x] 生产代码、配置、依赖、数据库和 Redis 均未变化。

## 10. 最终结论

S0-04 技术验证完成：

- Spring Boot Admin：`GO WITH CONSTRAINTS`，仅第二期独立部署；
- SnailJob：Boot 4 成功链路 `PASS`，架构阶段决策 `DEFER`；
- Lock4j：`DEFER`；
- VXE Table：`GO WITH CONSTRAINTS`，仅第二期单页、异步、按需试点。

用户已于 2026-07-30 通过“下一步”确认本结论，S0-04 关闭。下一任务为 S0-05
兼容性决策评审；该确认不代表 G0 已通过，也不会自动开始第一期实施。
