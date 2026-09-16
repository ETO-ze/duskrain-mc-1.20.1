# DuskRain 衣冠阁：皮肤与外置登录

已部署于 `https://skin.duskrain.cn`，负责账号注册、游戏登录验证、皮肤和披风管理。

| 用途 | 地址 |
|---|---|
| 网站 | https://skin.duskrain.cn |
| 注册 | https://skin.duskrain.cn/web/registration |
| PCL2 / HMCL 外置认证 | https://skin.duskrain.cn/authlib-injector |
| 游戏服务器 | `nbc.rainplay.cn:42741` |

## 玩家登录

1. 在网站注册账号；账号使用英文字母、数字或下划线，密码至少 12 位。
2. 在 PCL2 选择 `DuskRain-1.20.1`，使用“DuskRain 衣冠阁”登录。HMCL 可手动添加上表的外置认证地址。
3. 填写皮肤站账号密码，启动游戏并连接服务器。
4. 在网站的角色管理中上传 PNG 皮肤、披风并选择经典或纤细模型。

本地安装位置为 `E:\.minecraft\versions\DuskRain-1.20.1`。已配置 Forge 47.4.10、独立 Java 17、4 GiB 内存、RTX 4060 光影档，以及独立版本文件夹。个人密码单独交付，不包含在这里和公开客户端包中。

原测试角色仍使用 `DuskRainDirector`，UUID 为 `6e93fc7d-8abf-3d21-a965-18b49e686b0b`。使用稳定的角色 UUID，保留原有游戏进度。网站暂不开放玩家自行改角色名，以免更改角色身份。

## 部署结构

- 应用：Drasl 4.0.1，官方源码标签 `v4.0.1`，提交 `823d8a21780d6b15f44aec3de68411a5c40e3fe2`。
- 容器：`duskrain-skin`，使用 `compose.yaml` 中固定摘要的官方镜像。
- 目录：`/srv/duskrain-skin`；数据库、签名私钥、皮肤位于 `data/`，配置位于 `config/config.toml`。
- 仅监听宿主机 `127.0.0.1:25585`，由 Nginx 提供 HTTPS。容器限制为 256 MiB 内存、0.5 CPU。
- Nginx 文件为 `/www/server/panel/vhost/nginx/duskrain-skin-domain.conf` 和 `duskrain-skin-ip.conf`。
- 另保留 `https://38.22.234.64` 的受信任 HTTPS 入口；启动器长期使用域名地址。
- 本机原有其他网站与服务保持独立。

### 初始化及更新

首次初始化先使用随附配置中的 `RequireInvite = true`。确认 `DefaultAdmins` 与原角色 UUID 后，运行 `bootstrap-admin.py`；它只在私人凭据文件不存在时创建初始管理员，并检查管理员身份与原角色 UUID。初始化完成后才将部署配置中的 `RequireInvite` 改为 `false` 并重启容器，开放玩家注册。当前部署已完成此步骤。

`assets/` 保留官方镜像的完整运行资源。更新原创页面时，把 `branding/*.tmpl` 复制到 `assets/view/`，把 `branding/duskrain.css` 和 `branding/logo.svg` 复制到 `assets/public/`，再重启应用。仅修改模板或 CSS，不替换官方前端脚本。保留本目录的许可与署名文件。

```sh
cd /srv/duskrain-skin
docker compose ps
docker compose logs --tail 60 skin
docker compose restart skin
```

管理员凭据保存于服务器 `/srv/duskrain-skin/private/admin-account.json`，权限为 600。它含管理 API 令牌，不能作为玩家说明书分发。登录网站可修改账号密码。

### HTTPS 续期

使用独立的 Certbot 5.8.0 环境和独立证书目录，不复用其他网站的 Certbot 配置。

- 程序：`/srv/duskrain-skin/certbot-venv/bin/certbot`
- 证书：`/srv/duskrain-skin/tls/live/`
- 定时器：`duskrain-skin-renew.timer`，每天两次检查，成功续期后校验并重载 Nginx。
- IP 证书有效期较短，必须保留定时器与 HTTP 80 的 ACME 验证路径。

```sh
systemctl status duskrain-skin-renew.timer
journalctl -u duskrain-skin-renew.service --since yesterday
/bin/sh /srv/duskrain-skin/renew-certificates.sh
```

证书申请使用 `webroot` 验证，挑战目录为 `/www/wwwroot/duskrain.cn`；Nginx 仅为 `/.well-known/acme-challenge/` 提供相应文件。

### 备份与恢复

`backup-site.py` 通过 SQLite 在线备份生成一致的数据库副本，并保留签名私钥、皮肤、配置、主题和证书。以 root 在服务器执行，备份位于 `private/backups/`，不要公开下载目录。

恢复时先停止 `skin` 容器，将备份解压到单独目录核查，再恢复 `data/` 和需要的配置；保留原 `key.pkcs8`，避免改变已签名资料的验证密钥。恢复后启动容器，检查 HTTPS、密码登录、角色 UUID 和游戏入服。不要在运行中的数据库上直接覆盖文件。

## 游戏服务器认证

服务器保持 `online-mode=true` 与 `enforce-secure-profile=true`。在 `/server/user_jvm_args.txt` 中加入：

```text
-javaagent:authlib-injector-1.2.8.jar=https://skin.duskrain.cn/authlib-injector
```

组件放在 `/server/authlib-injector-1.2.8.jar`，仍用 `sh start-duskrain.sh` 启动。此配置需要重启 Java 后生效，2026-09-16 已完成重启和真实入服验证。

## 本次验收

- 域名与 IP HTTPS 可访问，两张证书的模拟续期均通过，续期定时器启用。
- 域名下 19 项 API 检查通过，覆盖注册、皮肤/披风、错误密码、越权拒绝、会话票据和令牌撤销。
- 浏览器完成注册、角色管理和皮肤上传，检查桌面与窄屏布局；临时账号已删除。
- 原版 Minecraft 1.20.1 authlib + Java 17 实测登录、入服票据与聊天签名密钥。
- PCL2 已识别并选中 E 盘版本；额外验证 PCL2 使用的 `/authlib-injector/authserver/authenticate` 路径。
- 使用 E 盘实际客户端连接公网服务器成功，原 UUID、渡劫后期、合欢宗宗主、9,733,272 灵石与原数据一致。
- Minecraft 运行库 93 项与资源对象 3575 项验证完成；原有三个 Minecraft 整合包保留。
- 主站、个人页和简历页部署前后 SHA-256 一致。

真实入服采用客户端的测试入口完成；未代替用户在 PCL2 登录表单中保存密码。详细机器测试记录保存在本地 `docs/qa/`，不含公开交付的凭据。以上为功能检查，不是并发压力测试。
