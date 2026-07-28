# alpha-server · 实现概览

## 职责

- Spring Boot 单体服务，提供认证、系统管理、文件、日志和监控等业务 API。
- 以领域模块组织代码；未来是否拆分为多模块取决于真实的边界和演进需求。

## 结构与入口

- `common/` 承载通用响应、异常、分页与工具；`framework/` 承载安全、Web、日志和技术适配；`modules/<domain>/` 承载业务。
- 每个领域按 `controller`、`service`、`mapper`、`entity`、`dto`、`vo` 等职责分层。Controller 处理 HTTP、校验、权限和统一响应；Service 负责事务与业务规则；Mapper 只负责持久化。
- Entity 仅表达持久化结构，不能直接作为 HTTP 入参或响应；新增、更新使用不可变 DTO，响应使用 VO。
- Flyway 管理版本化迁移，只能追加新版本，不能修改已运行的历史迁移。

## 安全与可观测性

- 预期业务错误通过统一异常处理转为公开错误消息，不向客户端暴露堆栈或内部细节。
- 有审计价值的写操作记录操作元数据，不记录请求体、密钥、令牌或其他敏感字段。
- 每个请求关联 traceId；日志使用参数化输出并遵守敏感信息脱敏规则。

## 相关文档

- 跨端协议：[`../../contracts/frontend-backend-common.md`](../../contracts/frontend-backend-common.md)
- 编码约定：`docs/conventions.md`
