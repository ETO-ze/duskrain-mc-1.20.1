# 源码开发与运行

## 环境

Minecraft1.20.1、Forge47.4.10、JDK17，Gradle版本由 `mod/gradle/wrapper/gradle-wrapper.properties` 锁定。设置 `JAVA_HOME` 指向JDK17，首次构建需网络。Gradle负责下载Forge、GeckoLib与FerriteCore，无需把上游JAR提交进Git。

在仓库根目录：

```powershell
.\mod\gradlew.bat -p .\mod build
.\mod\gradlew.bat -p .\mod runGameTestServer
```

Linux/macOS可以用 `sh mod/gradlew -p mod build`。本轮实际验收平台是Windows，其他平台尚未逐项验证。

## 开发客户端

```powershell
$env:DUSKRAIN_CLIENT_DIR='run-dev'
$env:DUSKRAIN_USERNAME='DuskRainDev'
.\mod\gradlew.bat -p .\mod runClient
```

可选 `$env:DUSKRAIN_VISUALS='true'` 开启构建中声明的客户端视觉依赖；光影和资源包需从发行包或相应官方源取得。不要把客户端渲染模组安装到正式服务端。

`DUSKRAIN_DIRECTOR` 默认关闭。设为true会开启本项目的本机自动验收接口、自动进入测试世界并接收命令文件，只用于隔离的开发实例。

## 文件边界

- `mod/src/main/java/cn/duskrain`：服务端玩法、客户端菜单、网络协议及测试代码。
- `mod/src/main/resources`：维度、任务/指南数据、方块与人物资源。
- `assets/jade-city`：原创材质包源文件；第三方元数据保留作者、版本和哈希。
- `tools/generate_*.py`：原创资源生成过程，具体依赖查看各脚本导入。
- `tools/gradle.ps1`：当前制作机的独立 `.deps/java` 与 `.deps/gradle` 封装；普通新克隆请直接使用Gradle Wrapper。
- `tools/qa`、`start-v14-*.ps1`：含本机路径、历史测试角色和存档约定的验收工具。先阅读并改为自己的隔离实例路径再运行，不能直接针对线上世界操作。
- `tools/package-v14.py`：制作机上的版本迁移与完整发行打包脚本，依赖先前发行包和经过备份的体验世界；普通源码编译不依赖它。

## 运行与升级

新启动器实例和独立服务端安装优先参考发行包README。模组版本2.1.0-preview，协议7，客户端与服务端同步升级。正式开服前备份世界和配置；不要用体验包里的测试角色账本覆盖自己的生产账本。

本轮48项必要GameTests、两次成品服务端启动与三档短时模拟负载已通过，完整真人试玩矩阵仍有未完成项。仓库中的精选数据在 `docs/github/validation.json`；发行包保留 `docs/qa` 原始记录。
