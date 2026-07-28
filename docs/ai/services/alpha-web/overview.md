# alpha-web · 实现概览

## 职责

- Vue 3 管理端，负责认证后的页面、交互、导航体验和前后端 HTTP 契约调用。
- 前端权限用于路由和操作可见性；后端授权仍是唯一安全边界。

## 结构与入口

- `service/` 承载类型化 HTTP 契约；页面和组件不得直接调用 Axios。认证、文件、日志、监控和系统管理按领域隔离。
- `views/` 负责路由页面与交互编排；只在存在清晰职责或稳定复用时提取页面局部组件、组合式函数。
- `stores/` 持有跨页面状态。认证状态的 token、profile、permissions 和动态路由必须整体写入或整体清空。
- `router/` 使用受控组件白名单与后端菜单、权限码共同注册动态业务路由；未知组件标识拒绝注册，会话切换或退出时清除动态路由。
- 管理端采用 Ant Design Vue 业务控件，UnoCSS/CSS 负责布局、间距和响应式组合；UnoCSS reset 保持关闭，避免覆盖 Ant Design Vue 的组件基线。保留桌面、平板和手机上的完整操作能力。

## 结构演进

- 以领域与交互职责决定 service、页面或路由的拆分，不以文件行数或目录对称为目标。
- 结构迁移需要保持 API 契约、权限码、路由路径和用户可见行为；必要时保留类型化兼容导出，待独立评审后再移除。
- 路由页保留流程编排权；不预先创建通用 CRUD 抽象、空目录或第二套组件库。

## 相关文档

- 跨端协议：[`../../contracts/frontend-backend-common.md`](../../contracts/frontend-backend-common.md)
- 详细开发规范：`docs/frontend-conventions.md`
