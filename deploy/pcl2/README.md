# PCL2 独立版本设置示例

`Setup.ini` 适用于新建的 DuskRain 版本，复制到版本目录的 `PCL/Setup.ini`。它启用独立实例、4 GiB 内存和皮肤站外置登录。启动后显示 DuskRain 游戏首页，点击“启程”连接服务器。

`VersionArgumentJavaV2:1` 与 `VersionArgumentJavaRange:[17.0,18.0)` 限定启动器使用 Java 17。导入包不捆绑 Java；如果启动器未找到合适版本，先安装 Java 17。不要强制“仅使用版本内 Java”，因为轻量包没有内置运行环境。新版 PCL2 的 `PCL/config.json` 可使用：

```json
{"InstanceMigratedJava":true}
```

该文件仅预置启动设置，不包含游戏本体、Forge 运行库、模组、Java 程序或账户密码。先按项目发行说明安装游戏和客户端文件；不要把此设置覆盖到其他整合包。

将发行 `.mrpack` 拖入 PCL2 窗口，使用新的版本名称安装。官方源下载所需模组，完成后选择 DuskRain 衣冠阁登录。账户和密码由玩家自行注册，不包含在包内。新包预置左上角地图和 DuskRain 游戏首页；旧实例的地图位置需要在地图选项中改为左上角，不能仅靠覆盖默认配置修复。登录与入服检查以当前发行的测试记录为准。
