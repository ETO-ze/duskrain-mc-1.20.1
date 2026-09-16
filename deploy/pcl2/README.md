# PCL2 独立版本设置示例

`Setup.ini` 适用于新建的 DuskRain 版本，复制到版本目录的 `PCL/Setup.ini`。它启用独立实例、4 GiB 内存、皮肤站外置登录，并自动连接现有服务器。

`VersionArgumentJavaV2:2` 表示使用版本文件夹内的 Java。请先把独立 Java 17 放入版本文件夹的 `java17/` 中；若使用自己安装的 Java，在启动器版本设置中明确选择 Java 17。新版 PCL2 的 `PCL/config.json` 可使用：

```json
{"InstanceMigratedJava":true}
```

该文件仅预置启动设置，不包含游戏本体、Forge 运行库、模组、Java 程序或账户密码。先按项目发行说明安装游戏和客户端文件；不要把此设置覆盖到其他整合包。

用户本地 `E:\.minecraft\versions\DuskRain-1.20.1` 已完成对应安装和真实入服检查。已有发行 ZIP 仍需按 [外置登录说明](../../docs/账号与外置登录.md) 配置认证。
