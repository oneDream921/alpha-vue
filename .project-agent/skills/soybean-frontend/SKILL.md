---
name: soybean-frontend
description: Apply and review the Alpha Vue SoybeanJS frontend standard for Vue 3, strict TypeScript, Ant Design Vue, Elegant Router, Pinia, UnoCSS, theme tokens, shared components, responsive admin pages, ECharts, permissions, and browser-based visual verification. Use for any implementation, refactor, code review, debugging, visual polish, route integration, component extraction, or pre-delivery validation under alpha-web-soybean.
---

# Soybean 前端规范

这是 Alpha Vue 新前端 `alpha-web-soybean` 的项目级 SoybeanJS 开发与验收规范。它约束页面结构、组件选择、路由适配、设计变量、响应式行为和真实浏览器验证；后端 API 契约保持不变。

## 工作流程

1. 先检查仓库状态、目标页面、邻近组件和真实运行地址；以 `alpha-web-soybean` 当前代码为事实源，不把旧 `alpha-web` 的实现直接当作新前端规范。
2. 页面结构、组件复用、路由、图表或响应式任务读取 `references/component-patterns.md`；命名、Vue/TypeScript 写法、主题或文案任务读取 `references/code-conventions.md`；工具链、生成文件或运行时任务读取 `references/tooling-and-generated-files.md`；路由、权限或测试任务读取 `references/route-permission-test-matrix.md`。只读取与任务相关的参考。
3. 保持业务 API、请求路径、响应码和后端权限语义不变。页面不得直接调用 Axios；请求统一放在 `src/service/api` 和 `src/service/request`。
4. 优先复用 Soybean/Ant Design Vue 组件和本项目共享组件；只有在行为确实不同或跨页面复用成立时才新增组件。
5. 实现 loading、empty、error、permission、success、confirmation 和 cleanup 状态，不用静态占位内容冒充接口结果。
6. 可见 UI 或交互变更必须使用内置浏览器验证：先看页面身份和 DOM，再截图，检查控制台，执行至少一个真实交互；有响应式影响时复查桌面、平板与移动端。
7. 新增、移动或删除 `src/views` 页面后运行 `pnpm gen-route`，检查 Elegant Router 生成文件和类型变化；不得手改生成文件来绕过路由生成流程。
8. 修改本 Skill 时使用 `afk-maintenance`，根据 Manifest 的 npm 或本地源码模式选择同一来源的 dry-run 和正式同步入口；不要直接修改 `.agents`、`.claude`、`.cursor` 或 Manifest 投影。

## 必须遵守的边界

- 技术栈固定为 Vue 3 Composition API、严格 TypeScript、Ant Design Vue、Pinia、Axios 封装、Vite、UnoCSS 和 ECharts（按需引入）。不引入 Element Plus、Naive UI 或第二套业务组件库。
- `src/views` 负责页面编排，`src/components` 只放有明确复用价值的组件，`src/service/api` 负责领域 API，`src/store` 负责真正跨页面状态。
- 使用后端菜单和权限作为可访问性来源；前端 `v-permission` 只改善体验，不能替代后端授权。
- 动态路由适配必须保留 `layout.base$view.*`、`view.*` 的组件命名约定。隐藏但由顶栏进入的认证页面（例如 `/user-center`）必须注册为隐藏路由，不能因不在后端菜单而变成 404。
- 使用 `@ant-design/icons-vue` 或项目已有 `SvgIcon`；按钮和图标必须有可见的对齐、可访问名称和可点击区域。
- 页面样式优先使用 Soybean 主题 token、UnoCSS 和页面局部样式。现有 `--alpha-*` 是业务兼容层；新增或调整 token 必须兼容亮色/暗色主题，禁止写死仅适配亮色的背景或文字色，也禁止引入第二套 reset。
- ECharts 组件只在容器存在且有尺寸时初始化；不能用 `import.meta.env.MODE === 'test'` 阻止真实浏览器渲染。jsdom 下用尺寸保护自然跳过初始化。
- 文件和目录默认使用 kebab-case；Vue 组件文件可使用 PascalCase，但文件名、组件名和自动导入名称必须保持一致；普通 TS 文件不新增 camelCase/PascalCase 例外。
- 请求函数统一以 `fetch` 开头；API、路由和权限测试不得用脱离真实契约的 mock 名称或静态演示接口。
- `strict`、`strictNullChecks` 和 `import type` 规则必须保持；未知异常使用 `unknown` 并在使用前完成类型收窄，避免用 `any` 或非空断言隐藏状态问题。
- 当前项目的 `pnpm lint` 会执行自动修复；只读检查使用 `pnpm exec eslint .`。Node/pnpm 版本以 `package.json` 的 engines 和锁文件为准，变更运行时基线时同步更新文档与验证入口。

## 验收门槛

页面交付前至少检查：

- 首屏不是空白，标题、导航、页面主要内容和品牌文案正确。
- 桌面 1280/1440、平板 768/991 宽度下无重叠、裁切或巨型未约束图片；移动端 375/390 宽度下操作仍可达；涉及颜色、背景或图表时同时检查亮色和暗色主题。
- 表格在窄屏可横向滚动，筛选栏和操作按钮能换行；弹窗、抽屉和下拉菜单不超出视口。
- 按钮、图标、文字和 loading 状态垂直对齐；图标按钮有 aria-label 或 tooltip。
- 图表有有效数据时真实生成 canvas/SVG；无数据时展示明确 empty 状态；图表组件卸载时释放 observer 和实例。
- 浏览器控制台无本次改动产生的错误或未解释警告；真实交互能产生预期状态变化。
- service 变更已验证 HTTP 方法、路径、参数、请求体和响应适配；路由或权限变更已验证白名单、动态注册、未知组件拒绝和会话清理；store 变更已验证会话整体写入、失效清理和持久化异常。

## 检查命令

在 `alpha-web-soybean` 目录执行与改动相关的检查：

```bash
pnpm exec eslint .
pnpm typecheck
pnpm exec vitest run --reporter=dot --pool=forks --poolOptions.forks.singleFork=true
pnpm build:test
git diff --check
```

页面或路由文件变更时，先运行 `pnpm gen-route`，再运行相关 Vitest、`pnpm typecheck` 和 `pnpm build:test`。不要把构建通过当作可见 UI 通过；涉及页面时必须补充浏览器截图、DOM/URL 状态和控制台证据。
