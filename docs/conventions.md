# 编码规范

## 后端

- 单体按领域分包：通用契约放 `common`，技术适配放 `framework`，业务放 `modules/<domain>`。每个领域按职责继续拆为 `controller`、`service`、`mapper`、`entity`、`dto`、`vo`；配置放 `config`，基础设施实现放专属子包（如 `storage`、`aspect`）。禁止将 Controller、Service、Mapper 直接并列放在领域根包。
- Controller 只处理 HTTP、Jakarta Validation、权限和统一响应；事务与业务规则进入 Service；Mapper 只负责持久化。
- Controller 默认继承 `framework.web.BaseController` 复用统一响应、traceId、客户端 IP 和当前登录用户 id 等请求上下文能力；不得在各 Controller 重复手写响应包装。基类保持轻量，不承载通用 CRUD、业务权限前缀、实体入参/出参或持久化逻辑。
- 数据库实体以 `Sys` 开头并继承 `SystemEntity`；不得把密码字段返回为接口 DTO。
- Entity 只表达持久化结构，不直接作为 Controller 入参或响应；新增、更新分别使用不可变 DTO record，并用 Jakarta Validation 声明边界。
- 响应使用明确的 View record，不返回无关字段；DTO 与 View 放在所属领域，不建立跨领域的通用大对象。
- 权限码使用 `domain:resource:action`。前端隐藏不是安全边界，每个后端入口必须独立校验。
- 迁移只追加 `V<n>__description.sql`，不得修改已运行的迁移；种子数据仅包含最小系统数据。
- Mapper 接口不得使用 MyBatis SQL 注解（如 `@Select`、`@Update`）；所有自定义 SQL 必须放在 `src/main/resources/mapper/<domain>/*.xml`，并与 Mapper 全限定名一一对应。
- 实体名使用业务表前缀与单数名词（例如 `SysUser`、`SysFile`），关联实体使用双方名词（例如 `SysUserRole`）；请求使用 `Create`、`Update`、`Save` 等 DTO，响应使用 `*Vo`，不使用含混的 `Data`、`Info` 命名。
- 预期业务错误抛 `BusinessException`；公开消息来自固定枚举，不把异常细节返回客户端。
- Controller 不捕获并吞掉业务异常；统一异常处理器负责 HTTP 状态、错误消息和 traceId。存储、数据库等内部异常只记录安全上下文。
- 仅在有审计价值的变更入口使用 `@OperationLog`，不得记录请求体或敏感参数。
- 使用 SLF4J 参数化日志和 MDC traceId，禁止 `System.out`、直接打印异常凭据或 SQL 参数。
- SQL 监控只记录最近执行摘要和占位符 SQL，不记录真实参数值；采集控制只能作为运行时排查开关，不作为审计或持久化配置。Druid 监控须受配置、登录和网络边界控制，生产默认关闭。

## 前端

详细规则见 [Alpha Vue 前端开发规范](frontend-conventions.md)。本节保留跨前后端评审时必须快速核对的核心约束。

- 使用 TypeScript 严格模式；接口类型集中在 `service`，页面不直接调用 Axios。
- 禁止使用 `any` 绕过类型检查；确需处理未知值时使用 `unknown` 并在边界收窄。
- 认证状态只有一个来源；Token、profile、permissions 和 routes 必须整体写入或整体清空。
- 路由权限负责导航体验，`v-permission` 负责按钮展示；两者都不替代后端授权。
- Ant Design Vue 负责表格、表单、弹窗、上传等业务控件；Tailwind/CSS 仅负责布局、间距和响应式组合。
- 页面组件负责交互编排，不复制请求封装、会话恢复或权限判断；可复用状态进入 store，可复用 HTTP 契约进入 service。
- 页面保持“标题与主要操作、筛选、数据区、弹窗”结构。表格设置稳定列宽和横向滚动。
- 列表页提供明确刷新操作；新增、编辑、删除、上传成功后重新请求后端。失败时保留当前数据并给出可见反馈。
- 图片上传必须限制选择类型、显示加载和成功/失败反馈，并在获得可访问 URL 后提供缩略图或预览。
- 小于 768px 使用抽屉导航、筛选与表单单列；不得删除新增、编辑、删除、上传、分配等能力。
- 样式使用统一设计令牌，页面区块不嵌套装饰卡片；紧凑工具栏、表格和弹窗优先保证高频操作效率。
- 图标按钮使用现有图标库并提供 `aria-label`/`title`；禁止手绘重复 SVG。
- Ant Design Vue 的 locale 必须固定为中文；分页、空状态、确认与校验消息不得遗留英文。按钮使用组件内图标，并保持图标和文字垂直居中。
- API 错误由请求层处理会话失效，页面只处理本业务反馈；不得在控制台输出 Token 或敏感表单。

## 测试与交付

- 安全、权限、事务和跨层契约优先写失败测试；真实 HTTP 认证不能只依赖 MockMvc。
- 每个独立任务运行聚焦测试，阶段交付前运行相关后端全量测试、前端 typecheck/test/build 与 `git diff --check`；涉及真实联调、部署配置或存储切换时再运行 smoke test。
- 浏览器验收覆盖桌面、平板和手机，检查无重叠、无功能降级、可横向滚动和所有操作可达。
