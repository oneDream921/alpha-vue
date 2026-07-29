# Spike 证据

本目录只保存可恢复的轻量验证源码，不保存第三方仓库、依赖目录、构建产物、数据库或日志。

## S0-04

`s0-04-spike-sources-2026-07-29.tar.gz` 包含 34 个自建文件：

- Spring Boot Admin Server 与 Client 最小工程；
- Lock4j 最小应用与测试；
- SnailJob Boot 4 Client、执行器与触发入口；
- VXE/UnoCSS 隔离页面、配置与测试。

归档 SHA-256：

```text
30f0a2888718728506edbc6f8d7e51754f84a5eebcc440e3d10dcac4d441afee
```

解压与校验：

```bash
shasum -a 256 s0-04-spike-sources-2026-07-29.tar.gz
mkdir s0-04-spike-sources
tar -xzf s0-04-spike-sources-2026-07-29.tar.gz -C s0-04-spike-sources
```

归档前已将临时 Basic 密码和 SnailJob group token 改为必须由环境变量提供的占位符。
归档不是生产模块，不参与 Alpha 构建，也不代表候选依赖已进入正式基线。
