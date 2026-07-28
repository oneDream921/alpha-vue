# 访问控制与 RBAC

## 术语

| 术语 | 含义 |
|------|------|
| 用户 | 可登录的管理端主体，可关联多个角色并归属一个部门 |
| 角色 | 菜单和按钮权限的集合 |
| 权限码 | `domain:resource:action` 形式的授权标识 |
| `SUPER_ADMIN` | 唯一全权限绕过角色，不可删除 |

## 流程与规则

- 第一阶段使用账号密码与 Redis 会话 Token；客户端经 `Authorization: Bearer <token>` 传递认证信息。
- 密码仅以 BCrypt 哈希保存。首次登录修改初始密码；改密后当前会话失效。
- 登录成功后，客户端取得个人资料、权限和动态菜单再进入系统。退出或切换会话时，认证状态及动态路由必须一并清除。
- 后端是授权的最终安全边界。前端路由和按钮隐藏仅改善使用体验，不能替代接口权限检查。
- 登录失败按账号与 IP 限制；验证码、会话和登录失败窗口属于敏感安全数据。

## 字段语义

| 字段/状态 | 含义 |
|-----------|------|
| `Authorization` | Bearer 认证头 |
| `permission` | 用户被授予的权限码 |
| `MENU` | 可注册为业务路由的菜单类型 |

## 相关文档

- 前后端契约：[`../contracts/frontend-backend-common.md`](../contracts/frontend-backend-common.md)
- 安全说明：`docs/security.md`
