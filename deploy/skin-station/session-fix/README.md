# PCL 旧会话刷新兼容修正

基于 Drasl 4.0.1、提交 `823d8a21780d6b15f44aec3de68411a5c40e3fe2`。

## 原因

当前配置 `TokenStaleSec = 86400`，令牌一天后需要刷新，但未到 30 天失效期。
上游 `/validate` 使用 `StalePolicyAllow`，`/session/minecraft/join` 使用 `StalePolicyDeny`。
PCL 在 validate 成功后直接复用令牌，导致启动器登录成功、游戏内却显示无效会话。

## 修正

`validate-stale.patch` 只将 `BindAuthValidate` 改为 `StalePolicyDeny`。刷新接口仍允许 stale token，保持原有客户端令牌校验、签名、版本检查、失效期和 UUID。

`duskrain_session_test.go` 验证旧令牌被拒绝、刷新成功、新令牌通过、旧版本仍被拒绝。Dockerfile 使用原运行镜像，只替换修正后的应用二进制。

构建上下文包含固定提交的 `source/`、应用该 patch 后的 auth.go，以及新增测试。执行 `docker build -t duskrain-drasl:4.0.1-stale-validate .`；构建内先运行鉴权测试，通过后编译静态二进制。

部署前备份 compose、配置、数据库、纹理和签名密钥。只更新 `skin` 服务镜像，不修改其他站点与容器。回退恢复备份的 compose 并重新创建 skin；本补丁没有数据库迁移。

## 验收状态

已通过 `TestAuth`、`TestDuskRainStaleValidateRefresh`，构建日志 `docs/qa/skin-session-build.log`。
公网新会话 validate=204、join=204、hasJoined=200 且 UUID 一致；真实 PCL 于 2026-09-25 02:51 入服，03:06 正常退出。
旧 PCL 会话先校验失败，刷新也返回 403，启动器随后自动重新认证成功；不能把这次实机过程写成 refresh 请求成功。
部署备份：`/srv/duskrain-skin/session-fix-20260925/before/`，含 compose、配置和停服后数据快照。
当前自定义镜像 `duskrain-drasl:4.0.1-stale-validate`，不修改数据库结构。

准备上下文：`python prepare-context.py <新的空目录>`；然后在输出目录构建 Dockerfile。上游源码和 GPLv3 许可保留。

参考：[Drasl 鉴权实现](https://github.com/unmojang/drasl/blob/823d8a21780d6b15f44aec3de68411a5c40e3fe2/auth.go)、[入服实现](https://github.com/unmojang/drasl/blob/823d8a21780d6b15f44aec3de68411a5c40e3fe2/session.go)。
