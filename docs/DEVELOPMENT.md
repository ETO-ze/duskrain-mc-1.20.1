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

新启动器实例和独立服务端安装优先参考发行包README。模组版本2.2.0-preview，协议8，客户端与服务端同步升级。正式开服前备份世界和配置；不要用体验包里的测试角色账本覆盖自己的生产账本。

当前构建65项必要GameTests通过；历史性能样本和本次客户端交付检查分开记录。最新边界见 `docs/releases/2.2.0-preview.md`，原始本机记录位于忽略的 `docs/qa`；不随客户端包公开。

## 轻量客户端打包

需要 Python 3（打包器使用标准库）、已构建自研 JAR 和仓库锁文件；输出文件存在时拒绝覆盖。

```powershell
python tools/package-mrpack.py --output dist/release-r3/DuskRain-2.2.0-PCL2-Visual-r3.mrpack
python tools/package-v22.py --output dist/release-r3/DuskRain-2.2.0-client-Visual-r3
python -m unittest discover -s tools/qa -p test_*.py
```

`client_visual_bundle.py` 为两种包统一选择默认模组与光影，并从自研模组的 `visual-profiles.json` 生成首次启动参数。`modrinth-lock.json` 的光影 `default=false` 是底层通用模组栈选择标记；客户端发行选择器明确纳入此光影，服务端选择器继续排除它。`.mrpack` 的光影条目必须写入 `shaderpacks/`，不能写入 `mods/`。发行检查读取最终 ZIP/导入包并比对 JAR 内画质定义，覆盖此前漏装回归。

第三方文件通过固定官方 URL 与哈希获取。不要将安装完成后的暂存目录重新压包，否则会混入第三方二进制和本机运行数据；上传打包器最初生成的只读归档。
