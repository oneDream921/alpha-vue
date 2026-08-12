# 工具链与生成文件规则

## 运行时与包管理

- 使用 pnpm，依赖安装、脚本执行和锁文件更新均通过 pnpm 完成。
- Node 与 pnpm 版本以 `alpha-web-soybean/package.json` 的 `engines` 和仓库锁文件为准；不要仅依据上游 Soybean 文档猜测本项目运行时。
- 运行时基线发生变化时，同步检查 README、CI、启动脚本和本地验证命令。
- 不把 Registry token、登录信息或真实环境变量写入源码、日志和提交内容。

## 代码质量命令

- `pnpm lint` 允许自动修复，适用于用户明确允许修改格式的场景。
- 只读检查使用 `pnpm exec eslint .`。
- 阶段验证至少包括 `pnpm typecheck`、相关 Vitest 和 `pnpm build:test`；最终检查补充 `git diff --check`。
- 项目已配置 Soybean ESLint、simple-git-hooks 和 lint-staged。Agent 可以检查配置和报告结果，但不代用户执行暂存、提交、推送或其它 Git 写操作。

## Elegant Router 生成文件

- 新增、移动、重命名或删除 `src/views` 页面后运行 `pnpm gen-route`。
- 生成结果包括 `src/router/elegant/imports.ts`、`src/router/elegant/routes.ts`、相关路由类型声明和其它由脚本产生的文件；以脚本输出为准。
- 不手改生成文件来注册不存在的组件、绕过组件白名单或掩盖文件路径错误。
- 生成后检查路由名称、组件名称、隐藏路由和类型 diff；确认旧页面删除后没有残留导入。
- 生成文件的变更必须与页面变更放在同一逻辑交付范围内。

## 依赖与脚本变更

- 新依赖必须说明现有依赖无法满足的原因、体积影响和维护成本。
- 修改 hooks、lint-staged、生成脚本或 engines 后，重新执行对应的只读检查，并说明是否需要用户在本地重新安装或执行 `pnpm prepare`。
