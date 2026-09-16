# DuskRain 2.1.0 体验版 · 烟雨仙途

群号 **205255670**。Minecraft Java **1.20.1**、Forge **47.4.10**、Java **17**；DuskRain 通信协议 **7**，两端须一起更新。

新增铭灵附魔、登神三阶段、御剑、名下头衔、中文宗门与合欢宗府邸，以及27种商品箱子商铺。法衣共84件、法器21件，服务人物11位。新手指南更新为58页，原书会原位更新，`/dr guide` 可补领。

本包为体验版。通过项与尚未完成的实机验收分别记录在包内 `docs/V14-验收记录.md`，模拟角色不能代替真人网络容量测试。

## 客户端安装

1. 在正常启动器创建 Minecraft 1.20.1 / Forge 47.4.10 实例，使用 Java 17。
2. 复制 `client/mods`、`client/resourcepacks`、`client/shaderpacks`。更新旧实例前备份，并将旧的 `duskrain-*.jar` 移到实例之外，只保留本版一个 DuskRain JAR。
3. 新实例可使用 `client/config` 与 `options.txt`。已有实例使用 `client/apply-profile.ps1`，或游戏内 G → 显示设置选择2060、4060、5070档。这些名称是配置目标；并非三种显卡都做了实测。
4. 可选玩家立体皮肤：运行 `client/install-skinlayers.ps1 -ModsDirectory "实例完整路径\mods"`，从官方源下载并校验。ZIP不附3D Skin Layers JAR，原角色皮肤保留。

按键：G 菜单，Z/X/C 技能，R 起剑或缓降，空格上升，Shift 下降，F9 侧栏和技能栏轻量显示，F8 行旅HUD，Tab 道友信息。御剑中禁止攻击、施法和开容器；收剑落地后恢复正常操作。

## 服务端安装

包内提供独立 Java17、原创材质包及预生成世界。先运行根目录 `setup-server.ps1`，脚本从官方源下载并验证 Minecraft/Forge 依赖，然后运行 `server/start-server.bat`。ZIP不附游戏本体和下载依赖。

默认本机验证地址 `127.0.0.1:25566`，保留正版验证。资源下载地址默认为本机；对外开服需要修改 `server/resourcepack-host.json` 的公网地址，以及 `server.properties` 的监听设置。`online-mode=true` 不应因本地测试改动。

此包世界保留原玩家UUID进度、生存世界、住宅和商铺账本；主城增加铭灵轩，原宗门编号1改名合欢宗并建设府邸。额外测试角色、压测场地和QA购买没有合并进交付账本。**不要用随包世界覆盖已有正式服世界。** 旧服升级应备份后复制模组和配置，府邸创建仅针对原测试宗门；完成后的成员装修不会自动覆盖。

## 到店功能与指令

| 入口 | 功能 |
|---|---|
| 铭灵轩 · 云篆 / `/dr enchant` | 背包装备选择、指定词条、费用预览与确认；必须靠近柜台 |
| 问道书院 / `/dr cultivate` | 拜师、突破与登神资格检查 |
| 试炼入口 / `/dr trial` | 原三试炼及登神材料挑战 |
| 云渡使 / `/dr flight` | 御剑说明、10/60分钟体验；租用资格按实际飞行计时 |
| `/dr title` | 查看自己的称谓并切换名下头衔显示 |
| 宗务堂 / `/dr guild` | 创建、邀请、管理、洞天；中文名称2–16字 |
| `/dr guild rename 合欢宗` | 宗主改名，保留编号、成员和洞天 |
| 烟雨小集货柜 / `/dr stalls` | 每铺27种商品，实物上架、补货、改价、部分取回和购买 |

完整规则、资源消耗和限制见游戏内说明书与 `docs/中文指令手册.md`。配置位于 `server/config/duskrain*.json`，调整前备份，管理员使用 `/dr admin reload` 检查载入。

## 文件与署名

`source` 是源码与原创模型贴图，`server/world` 是世界，`docs/qa` 是测试原始记录，`checksums.json` 是文件SHA-256清单。

光影使用 Complementary Development / EminGTR 的 Complementary Reimagined r5.3（原包未修改，附原许可），渲染优化使用 Embeddium、Oculus；动画使用 GeckoLib，内存优化使用 FerriteCore。详见 `docs/素材与版本署名.md` 与 `third-party`。NPC不依赖3D Skin Layers实现自定义模型。
