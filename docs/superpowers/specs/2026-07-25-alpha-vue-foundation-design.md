# Alpha Vue 第一阶段设计

## 目标

Alpha Vue 是面向后续业务项目复用的全栈管理框架。第一阶段提供一个容易理解、规范且可运行的单体基础：Vue 管理端、Java 后端、账号登录、RBAC、文件上传与审计日志。

本阶段优先开发效率、清晰边界与安全默认值；不追求一次覆盖所有企业功能。

## 技术基线

| 范围 | 选择 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios |
| UI 与样式 | Ant Design Vue、Tailwind CSS v4、CSS Variables、少量 SCSS |
| 后端 | Java 21、Spring Boot 4、MyBatis-Plus、Sa-Token |
| 基础设施 | MySQL、Redis、Flyway、MinIO（可选） |
| 日志 | SLF4J + Logback |

Ant Design Vue 负责表格、表单、树、弹窗和上传等业务组件；Tailwind 只负责页面布局、间距、响应式与自定义业务区块，不覆盖组件库内部样式。

## 架构与目录

采用轻量单体，而非一开始拆分 Maven 多模块。后端以领域模块分包，未来确有需要时再提取模块。

```text
alpha-vue/
├── alpha-web/
│   └── src/
│       ├── api/          # 接口客户端
│       ├── components/   # 通用业务组件
│       ├── layouts/      # 响应式主布局
│       ├── locales/      # 仅预留，中文为默认语言
│       ├── router/       # 路由、守卫、动态菜单
│       ├── stores/       # 用户、权限、主题状态
│       ├── styles/       # Tailwind、设计令牌、全局样式
│       └── views/        # 页面视图
├── alpha-server/
│   └── src/main/
│       ├── java/io/github/onedream921/alphavue/
│       │   ├── common/    # 响应、异常、分页、通用工具
│       │   ├── framework/ # 安全、日志、配置、MyBatis-Plus
│       │   └── modules/   # auth、system、file
│       └── resources/db/migration/
├── deploy/                # Docker Compose 与环境示例
└── docs/
```

本地开发前后端独立启动。后续可选择将前端构建产物托管给后端，但不作为第一期交付目标。

## 第一阶段功能

### 页面

- 登录页
- 首页（欢迎信息与快捷入口）
- 个人中心（头像、昵称、密码修改）
- 用户、角色、菜单、部门管理
- 文件管理
- 操作日志与登录日志
- 403 与 404 页面

### RBAC

用户可拥有多个角色；角色关联菜单与按钮权限；用户关联一个部门。后端是唯一安全边界，前端仅用于展示菜单与隐藏无权限按钮。

权限编码采用 `domain:resource:action`，例如 `system:user:list`、`system:user:create`、`file:upload`。内置 `SUPER_ADMIN` 角色拥有全部权限且不可删除。

### 登录

第一期使用账号密码、可配置图形验证码与 Sa-Token 的 Redis 会话 Token。Token 通过 `Authorization: Bearer <token>` 传递，不使用 JWT。

- 密码使用 BCrypt；初始密码首次登录时要求修改。
- 开发环境验证码默认关闭，生产环境默认开启。
- 按账号与 IP 限制失败次数：默认连续 5 次失败锁定 15 分钟，可配置。
- 默认会话有效期 8 小时，30 分钟无操作失效；记住我可延长至 7 天。
- 支持退出、管理员踢下线、禁用用户后即时失效。
- 短信、OAuth、单点登录仅预留 `LoginStrategy` 扩展点，不在本阶段实现。

登录成功后，前端获取个人资料、权限与动态菜单，再进入首页。

### 文件

文件模块通过 `StorageProvider` 隔离存储实现，第一期提供 `LocalStorageProvider` 和 `MinioStorageProvider`。

- 系统配置控制允许类型与大小。
- 上传前校验扩展名、MIME 类型、大小和文件名。
- MySQL 仅保存文件元数据和访问地址，不保存二进制。
- 文件管理支持上传、列表、预览链接和删除；删除按“先删对象，再软删记录”执行。

### 数据库

Flyway 管理全部版本化迁移。初始数据只包含必要表、最小管理员账号、角色、权限与菜单；严禁提交真实密钥、Token、历史日志或个人数据。

初始表：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`、`sys_dept`、`sys_config`、`sys_file`、`sys_login_log`、`sys_oper_log`。

## API 与错误处理

接口统一以 `/api` 为前缀，并按领域组织：

```text
/auth/login       /auth/logout       /auth/profile       /auth/routes
/system/users     /system/roles      /system/menus        /system/depts
/system/configs   /files/upload      /files
/logs/operations  /logs/logins
```

统一响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {},
  "traceId": "..."
}
```

分页参数统一为 `page`、`size`，响应统一为 `records`、`total`、`page`、`size`。后端全局异常处理不暴露堆栈；前端统一处理 401、403、网络错误与表单校验错误。

## 日志与审计

采用“开发可观测、生产克制审计”的策略：

- 应用日志：控制台与按日期滚动文件；生产默认保留 30 天。
- 操作审计：`@OperationLog` + AOP，异步写入 `sys_oper_log`；默认保留 180 天。
- 登录审计：记录成功、失败、锁定、退出和踢下线，异步写入 `sys_login_log`。
- 开发环境打印 SQL 耗时和慢 SQL；生产只保留慢 SQL 摘要，不提供 SQL 日志页面。
- 每个请求生成并返回 `traceId`。
- 密码、Token、Cookie、验证码、`secret`、`key`、`password` 字段和上传正文一律脱敏且不落库。

## 响应式规则

管理端保持一套完整功能，不做移动端阉割版：

- 宽度不小于 1024px：侧栏可固定或折叠。
- 768px 至 1023px：默认收起侧栏，按需展开。
- 小于 768px：侧栏变抽屉；筛选条件折行；表格可横向滚动；所有操作保留。

## 明确不在第一期实现的功能

- XXL-JOB 集成与任何定时任务
- 代码生成
- 监控大屏
- 多租户
- 完整国际化
- 第三方登录、短信登录、单点登录
- WebSocket
- 监控平台或完整 API 请求日志页面

这些能力仅保留清晰的未来接入位置，不预先编写实现。

## 验收标准

1. 空环境可通过 Docker Compose 启动 MySQL、Redis、MinIO，并通过 Flyway 初始化。
2. 登录、验证码开关、退出、禁用和踢下线流程可验证。
3. 普通角色无法访问或操作未授权菜单、按钮和接口。
4. 本地与 MinIO 文件上传均可用，类型和大小限制有效。
5. 操作与登录审计可查询，敏感字段不进入日志。
6. 桌面、平板、手机尺寸下均可完成上述功能。
7. 前端通过类型检查与构建，后端通过测试与构建。
