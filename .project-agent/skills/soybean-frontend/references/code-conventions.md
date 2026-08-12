# Soybean 代码规范

## 事实源优先级

发生冲突时按以下顺序处理：

1. `alpha-web-soybean` 的实际依赖、类型、生成文件和邻近实现。
2. Alpha Vue 的 API、权限、安全和交付约束。
3. Soybean Admin Ant Design Vue 参考项目与 Soybean 官方规范。

上游参考用于保持框架习惯，不能覆盖本项目已经确认的后端 API 与权限语义。参考目录不可用时，以当前项目代码和类型为准，不猜测上游实现。

## 命名与目录

- 文件和目录使用 kebab-case；Vue 组件名、类型、接口和类使用 PascalCase；变量和函数使用 camelCase；稳定常量使用 UPPER_SNAKE_CASE。
- Alpha Vue 在 Soybean 组件之上的业务扩展使用 `Alpha` 前缀，避免与自动导入组件重名，例如 `AlphaTableCard`、`AlphaTableColumnSetting`。
- 请求函数以 `fetch` 开头并表达资源和动作，例如 `fetchGetRedisInfo`、`fetchDeleteFile`。
- 路由一级名称使用 kebab-case，多级名称使用下划线分隔层级，例如 `monitor_redis`。
- 页面入口放在 `src/views/<domain>/index.vue`；仅供当前页面使用的组件放在页面的 `modules/`；跨页面稳定复用后再上移到 `src/components/`。
- 使用 `@/` 引用 `src` 内跨目录模块；只对同目录或紧邻模块使用相对路径，避免深层 `../../../`。

## Vue SFC

- 使用 `<script setup lang="ts">`、Composition API 和 `defineOptions`；可复用组件声明稳定组件名。
- 导入按 Vue/Router/Pinia/VueUse、UI/第三方、项目别名、相对路径分组；类型使用 `import type`。
- Props 和 Emits 先声明类型，再调用 `defineProps`、`withDefaults`、`defineEmits`；不要用 `any` 绕过接口或组件类型。
- 把可命名的初始化流程收敛到 `init`/`load`；watch 只处理真正的响应式副作用，避免与初始化重复请求。
- 卸载时清理定时器、事件监听、object URL、AbortController、ResizeObserver 和 ECharts 实例。
- 路由页面保持单一可过渡根节点，避免路由动画因多个根元素失效。

## TypeScript 与服务层

- 为 API 请求、响应和页面模型提供明确类型；优先复用 `src/typings` 与服务域类型，不在页面重复声明漂移的接口。
- 用联合类型、类型守卫和泛型表达状态，不用非空断言掩盖未初始化数据。
- 页面只调用 `src/service/api` 暴露的函数；响应适配、认证头、业务响应码和统一错误处理留在 service/request 层。
- setup store 不调用 Pinia 的 `$reset()`；需要重置时实现显式、可测试的清理方法。
- 浏览器可见文案保持同一页面语言一致。框架、路由和已有多语言域沿用 i18n key；不要为了单个改动启动无边界的全站国际化迁移。

## 主题与样式

- 优先使用 `src/theme/vars.ts` 暴露的 Soybean token、UnoCSS 主题类和 Ant Design Vue ConfigProvider token。
- `--alpha-*` 只作为 Alpha Vue 业务页面兼容 token；新增 token 应从 Soybean 主题变量派生，或同时提供 `.dark` 下的有效值。
- 禁止在页面散落品牌主色、纯白背景和固定浅色文字；确需固定颜色时说明其视觉语义并验证暗色模式。
- Ant Design Vue 负责业务控件行为，UnoCSS/CSS 负责布局、间距和响应式组合；不覆盖组件内部结构来模拟另一套组件库。
- 样式类名使用 kebab-case；优先 flex/grid/gap，避免负 margin、绝对定位和魔法数字修正通用对齐。

## 代码检查

- `package.json` 的 `lint` 脚本包含 `--fix`；只做无写入检查时使用 `pnpm exec eslint .`。
- 使用 `pnpm typecheck`、相关 Vitest 用例和 `pnpm build:test` 验证类型、行为与构建。
- 自动化检查不能替代浏览器验收；可见问题必须验证页面、交互、控制台和目标视口。
