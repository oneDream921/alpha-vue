# Alpha Vue 前端结构治理执行计划

> 状态：草案，待规范与计划确认后执行
>
> 依据：[Alpha Vue 前端开发规范](../../frontend-conventions.md)

## 1. 结论

当前前端分层方向正确，不需要整体重建目录。治理采用渐进方式：先拆分聚合 service，再拆分职责过多的页面，最后根据增长情况整理路由。保留现有技术栈、`layouts`、`stores`、权限机制和用户可见行为。

## 2. 当前基线

| 位置                         | 现状                                                   | 判断                                       |
| ---------------------------- | ------------------------------------------------------ | ------------------------------------------ |
| `src/service/system.ts`      | 203 行，集中用户、角色、菜单、部门、配置、字典六个领域 | 优先拆分，边界清晰且风险较低               |
| `src/views/system/users.vue` | 564 行，列表、编辑、角色分配、重置密码、响应式操作集中 | 优先拆分页面                               |
| `src/views/system/dicts.vue` | 534 行，同时管理字典类型和字典项两套状态及表单         | 优先拆分页面                               |
| `src/views/system/logs.vue`  | 494 行，登录日志与操作日志共存                         | 在确认交互保持不变后拆分                   |
| `src/views/system/menus.vue` | 370 行                                                 | 观察后按职责拆分，不单纯按行数处理         |
| `src/router/index.ts`        | 159 行，静态路由、业务白名单、动态注册集中但尚可读     | 延后处理，避免扩大首轮变更                 |
| `src/layouts/BaseLayout.vue` | 271 行，布局与导航集中                                 | 非本轮优先项，除非页面治理暴露稳定组件边界 |

## 3. 目标与非目标

### 目标

- 让 service 和页面按领域及交互职责演进，降低修改冲突。
- 保持稳定 import 入口和 API 契约，避免一次性修改所有调用方。
- 为分页、校验、权限和请求契约补充与风险匹配的测试。
- 每个阶段都可独立验证、独立提交、独立回退。

### 非目标

- 不切换 Element Plus，不引入第二套 UI 组件库。
- 不重写视觉设计，不改变接口和权限码。
- 不为了目录对称预建 `components`、`composables`、`utils` 空目录。
- 不在本轮建设通用 CRUD 页面生成器、万能表格或万能表单。
- 不同时重构后端或调整数据库契约。

## 4. 计划阶段

### 阶段 0：冻结行为基线

**目的**：确认开始结构调整前仓库处于可验证状态。

操作：

1. 记录 `git status --short`，区分已有用户修改。
2. 运行前端全量 typecheck、test、lint、format check 和 build。
3. 记录现有失败；若存在失败，先判断是否与结构治理相关，不在本任务中顺带修复无关问题。
4. 对用户、字典、日志页做桌面与手机截图，记录关键操作入口和表格行为。

验收：基线结果明确，后续每个失败都能判断是新增回归还是既有问题。

### 阶段 1：拆分系统领域 Service

**目的**：先建立低耦合、可稳定导入的领域边界。

目标结构：

```text
src/service/
├── system.ts                 # 迁移期兼容入口，只做 re-export
└── system/
    ├── shared.ts            # PageResponse、BaseEntity、resource 工厂
    ├── users.ts
    ├── roles.ts
    ├── menus.ts
    ├── depts.ts
    ├── configs.ts
    ├── dicts.ts
    └── index.ts
```

操作：

1. 将共享分页、基础实体和资源工厂移入 `shared.ts`，限制 `EntityName` 仍为受控联合类型。
2. 将每个领域的类型和 API 放入对应文件。
3. `system/index.ts` 统一导出领域模块。
4. 保留 `service/system.ts` 作为兼容 barrel，使现有 `@/service/system` 导入无需修改。
5. 新增请求契约测试，覆盖至少一个通用 CRUD 资源和字典自定义路径。

验证：

```bash
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test --run src/service
pnpm --dir alpha-web lint
git diff --check
```

停止点：确认导出名称、HTTP 方法、路径、参数与请求体没有变化后再进入页面拆分。

### 阶段 2：拆分用户管理页面

**目的**：把三个独立表单流程从路由页面中分离，同时保留页面编排权。

目标结构：

```text
src/views/system/users/
├── index.vue
├── components/
│   ├── UserFormModal.vue
│   ├── RoleAssignModal.vue
│   └── PasswordResetModal.vue
├── composables/
│   └── useUserPage.ts        # 仅承载列表请求、分页和选项加载
└── *.test.ts
```

操作：

1. 将路由入口从 `users.vue` 移至 `users/index.vue`，同步白名单 import，不改 path、name、componentId 或 permission。
2. 三个弹窗分别通过 typed props/emits 接收数据和回传提交事件，不直接读取路由或全局 DOM。
3. 页面保留删除、踢下线、权限控制和成功反馈等流程编排。
4. 将窗口尺寸监听封装并确保卸载清理；只有确认跨页面复用后才提升为全局 composable。
5. 增加关键表单校验、提交/取消、权限操作入口和分页变化测试。

验证：运行用户页聚焦测试、typecheck、lint，并在 1440px、768px、375px 三种宽度验收所有操作可达。

停止点：用户页行为与基线一致、没有重复错误提示、没有残留事件监听。

### 阶段 3：拆分字典与日志页面

**目的**：按页面内部已有的业务子域拆分，而不是抽取通用 CRUD。

字典页建议组件：

- `DictTypeTable.vue`
- `DictItemTable.vue`
- `DictTypeFormModal.vue`
- `DictItemFormModal.vue`

保留并迁移现有 `dicts.pagination.ts`、`dicts.validation.ts` 及测试到字典页面目录。类型切换必须继续重置字典项页码并避免旧请求覆盖新选择。

日志页建议组件：

- `LoginLogTable.vue`
- `OperationLogTable.vue`
- `OperationLogDetailModal.vue`（仅在现有详情确实构成独立区块时创建）

操作：

1. 先拆字典页并完成验证，再单独拆日志页，避免一次提交覆盖两个高状态页面。
2. 父页面管理当前 tab/字典类型等跨区块状态；子组件管理自身展示细节。
3. 不创建共享 `ProTable` 或 `useCrud`，除非两个页面拆完后出现相同且稳定的接口。

验证：分别运行字典与日志聚焦测试、typecheck、lint；桌面和手机检查双栏/切换、分页、详情及操作菜单。

### 阶段 4：评估剩余页面

**目的**：基于实际复杂度决定是否继续，不把拆分本身当作目标。

评估顺序：`menus.vue`、`monitor/redis.vue`、`roles.vue`、`depts.vue`、`BaseLayout.vue`。每个文件只有满足规范中的拆分条件时才进入调整，并为每个页面单独提交。

候选边界：

- 菜单：树表格与菜单表单。
- Redis：指标概览、查询结果、值预览。
- 角色：角色表单与菜单分配。
- 布局：桌面导航与移动抽屉导航，但共享菜单模型和权限判断不得复制。

### 阶段 5：按增长情况整理路由

**目的**：当路由继续增长或多人频繁修改时，再分离定义与注册机制。

建议结构：

```text
src/router/
├── index.ts                  # 创建 router 和挂载 guard
├── guard.ts
├── staticRoutes.ts
├── managementRoutes.ts      # 受控组件白名单
└── managedRouteRegistry.ts  # 动态注册与清理
```

必须保持：

- `managementRoutesFor` 的后端菜单类型、组件 ID 和权限三重匹配。
- 会话切换前清除动态路由。
- 路由 name、path、permission 和懒加载行为不变。
- 现有 `routes.test.ts` 扩展后继续覆盖未知组件拒绝、权限不匹配和路由清理。

当前 `router/index.ts` 尚未形成迫切风险，因此本阶段可以在复审时决定不执行。

## 5. 提交与回退策略

- 每个阶段至少一个独立提交；字典和日志必须分开提交。
- 纯文件移动与行为修改尽量分开，方便审查真实逻辑差异。
- 不使用全仓库批量格式化，不触碰无关后端和部署文件。
- 阶段验收失败时，只修复当前阶段引入的问题；必要时回退该阶段提交，不影响此前已验收阶段。
- 删除兼容 `service/system.ts` 入口属于后续破坏性清理，必须单独提案，不包含在本计划中。

## 6. 最终验收

完成获批阶段后运行：

```bash
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
git diff --check
```

浏览器验收覆盖 1440px、768px、375px：

- 登录与退出、动态菜单、403/404 正常。
- 用户、角色、菜单、部门、配置、字典、日志、文件和 Redis 页面可进入。
- 新增、编辑、删除、分配、重置密码、上传等原有能力均可达。
- loading、空状态、错误状态和成功反馈明确，无重复提示。
- 表格可横向滚动，操作列、弹窗和文字无重叠。

## 7. 执行前确认项

开始修改源码前，需要确认：

1. [前端开发规范](../../frontend-conventions.md) 作为后续前端任务的约束基线。
2. 阶段 1 至阶段 3 纳入本轮执行；阶段 4、阶段 5 默认复审后再决定。
3. service 使用兼容 barrel，当前页面导入路径保持不变。
4. 页面拆分不包含视觉改版和后端接口调整。
5. 项目 Skill 的安装位置与触发范围。
