# Soybean 组件与页面模式

## 目录与职责

| 目标 | 位置 | 约束 |
| --- | --- | --- |
| 路由页面 | `src/views/<domain>/index.vue` | 编排请求、状态、页面结构和交互 |
| 页面局部组件 | `src/views/<domain>/modules/` | 只服务当前页面，跟随页面内聚维护 |
| 共享业务组件 | `src/components/` | 跨页面复用且边界稳定后再上移 |
| 领域 API | `src/service/api/<domain>.ts` | 类型、路径、请求参数和响应适配 |
| 请求基础设施 | `src/service/request/` | Bearer、响应码 200、401 清理和统一错误处理 |
| Pinia 状态 | `src/store/` | 仅保存跨页面或会话级状态；setup store 不调用 `$reset()` |
| 静态组件导入 | `src/router/elegant/imports.ts` | 新页面名必须与 Elegant Router 生成的组件名一致 |

## 页面骨架

业务管理页默认使用：

```vue
<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>页面标题</h1>
        <p>一句话说明页面作用</p>
      </div>
      <ASpace wrap>
        <AButton @click="load">刷新</AButton>
        <AButton type="primary" @click="openCreate">新建</AButton>
      </ASpace>
    </div>
    <div class="query-bar">
      <AInputSearch v-model:value="keyword" allow-clear />
      <AButton @click="resetQuery">重置</AButton>
    </div>
    <AlphaTableCard :loading="loading">
      <ATable :scroll="{ x: 'max-content' }" />
    </AlphaTableCard>
  </section>
</template>
```

`--alpha-canvas`、`--alpha-surface`、`--alpha-border-soft`、`--alpha-text-primary`、`--alpha-text-secondary`、`--alpha-muted`、`--alpha-primary`、`--alpha-radius` 和 `--alpha-shadow` 是当前业务兼容 token。页面不得使用未定义的 `--alpha-*` 变量；新增或修改这些 token 时必须与 `src/theme/vars.ts` 的 Soybean 主题变量协调并验证暗色模式。

## 组件选择

- 业务控件使用 `AButton`、`AForm`、`AInput`、`ASelect`、`ATable`、`ATabs`、`AModal`、`ADrawer`、`ATree`、`AUpload` 等 Ant Design Vue 组件。
- 图标优先使用 `@ant-design/icons-vue`；顶栏的小型图标按钮复用 `ButtonIcon`，必须提供 tooltip 或 aria-label。
- 新业务页面的列表容器复用项目级 `AlphaTableCard`；项目级紧凑操作列可复用 `TableActionMenu`；需要持久化列显隐、固定和对齐配置时复用项目级 `AlphaTableColumnSetting`。
- Soybean 原生管理页已有 `TableHeaderOperation` 和自动导入的 `TableColumnSetting` 时保持原有体系。项目扩展组件使用 `Alpha` 前缀，禁止创建与 Soybean 自动导入组件同名的业务组件。
- `AlphaTableCard` 内部必须保留 `overflow-x: auto`，表格传入 `scroll: { x: 'max-content' }`，确保移动端可查看完整列。
- 统一按钮使用 `inline-flex`、`align-items: center` 和稳定的图标文字间距；不要使用负 margin 修正图标位置。

## 路由与权限

后端动态菜单进入 `fetchGetUserRoutes()` 后，由 `src/service/api/route.ts` 转成 Elegant Route：

- 有父目录的页面使用 `view.<route-name>`。
- 顶层页面使用 `layout.base$view.<route-name>`。
- 个人中心、设置等由顶栏直接进入但不应出现在菜单的页面，使用 `hideInMenu: true` 的认证路由。
- 页面按钮使用 `v-permission="'domain:action'"`，但后端仍必须再次校验权限。
- 新增文件路由后运行 `pnpm gen-route`，检查 `src/router/elegant/imports.ts`、`routes.ts` 和路由类型的生成差异；不要手写生成文件来绕过 Elegant Router。

## ECharts 模式

按需引入图表模块并在组件内 `echarts.use([...])`。初始化规则：

1. `nextTick()` 后确认 ref 存在且 `clientWidth/clientHeight > 0`。
2. 有 `ResizeObserver` 时观察容器并在尺寸变化时 `chart.resize()`。
3. 数据变化只调用 `setOption(option, true)`，不要重复 `echarts.init()`。
4. `onBeforeUnmount` 断开 observer 并 `dispose()`。
5. 不用 test mode 判断阻止真实浏览器图表；测试环境因容器无尺寸自然不初始化。
6. 有数据必须检查 canvas/SVG 数量和截图；无数据提供明确的可读空状态。

## 响应式检查

- `max-width: 767px`：页面 padding 收紧，标题/操作换列，筛选项占满宽度。
- `max-width: 991px`：双栏工作区变单栏，卡片网格减少列数。
- `max-width: 1100px`：首页快捷入口由四列降为两列。
- 移动端不隐藏关键操作；可以收纳导航或折叠次要列，但不能让用户无法完成业务操作。
