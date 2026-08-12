# alpha-web-soybean · 实现概览

## 职责

- 基于 SoybeanJS 的新 Vue 3 管理端，与原 `alpha-web` 分别作为共享 `alpha-server` 的独立客户端。
- 负责新管理端的页面、交互、路由、权限展示和前端状态；后端授权与业务契约仍由 `alpha-server` 决定。
- 具体编码、响应式和浏览器验收规则由项目 `soybean-frontend` Skill 承载，本文件只提供稳定实现导航。

## 结构与入口

- `src/views`：业务页面与页面编排。
- `src/components`：跨页面复用组件；`src/layouts`：应用布局。
- `src/service/api`：领域 API；`src/service/request`：统一请求和错误处理。
- `src/router`：Vue Router、Elegant Router 生成结果与路由守卫。
- `src/store`：认证、动态路由、标签页等跨页面状态。
- `src/main.ts`：应用、Store 和 Router 的装配入口。

前后端共用字段、响应与权限码语义见 [`../../contracts/frontend-backend-common.md`](../../contracts/frontend-backend-common.md)。

## 排查入口

- 页面数据或操作异常：先核对 `src/views` 调用的 `src/service/api` 领域函数，再检查统一请求层的响应适配。
- 登录、权限或菜单异常：先检查认证 Store、路由 Store、`src/router/guard` 和后端返回的菜单与权限。
- 页面路由或组件解析异常：核对 Elegant Router 生成文件、组件命名约定和视图文件是否一致。
- 页面空白或布局异常：从 `src/main.ts`、布局、目标页面和浏览器控制台依次定位；不要用原 `alpha-web` 的结构替代新前端事实。
