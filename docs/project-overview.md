# APlayer 项目介绍

## 1. 项目定位

APlayer 是一款 Android 本地音乐播放器，主要面向本地音乐库，也支持 WebDAV、SMB 等远程文件来源。项目使用 Kotlin 编写，界面以 Jetpack Compose 为主，播放引擎使用 AndroidX Media3/ExoPlayer，并通过 FFmpeg 扩展音频解码能力。

## 2. 技术栈

- Kotlin 2.1.21
- Java/Kotlin JVM target 17
- Android Gradle Plugin 8.8.0
- Gradle Wrapper 8.11.1
- compileSdk / targetSdk 35，minSdk 21
- Jetpack Compose、Material 3、Navigation Compose
- AndroidX Media3/ExoPlayer
- Room 数据库、Hilt 依赖注入、KSP
- CMake/C++ 原生代码

## 3. 模块结构

### `app`

主应用模块，包含播放服务、音乐扫描、歌曲/专辑/歌手模型、歌词、播放列表、设置、界面和数据库。

### `feature_smb`

动态功能模块，负责 SMB 网络共享访问，使用 `smbj` 库。

### `baselineprofile`

用于启动性能测试和 Baseline Profile 生成，不是用户功能模块。

### `third-party/taglib`

通过 Git 子模块引入的 TagLib 模块，用于音频标签读取和写入。完整构建前必须初始化该子模块。

## 4. 发行变体

项目有三个 distribution flavor：

- `normal`：普通发行版，包含 Bugly 和应用内更新
- `foss`：FOSS 发行版，关闭商业更新能力
- `google`：Google Play 发行版，包含 Google Play Billing 和动态功能交付

每个 flavor 都有 `debug` 和 `release` 两种构建类型。

## 5. 当前数据库与播放历史

项目使用 Room，数据库类为 `AppDatabase`。现有 `History` 表只保存歌曲级累计信息：

- `audio_id`：歌曲 ID
- `play_count`：累计播放次数
- `last_play`：最近播放时间

播放服务位于 `app/src/main/java/remix/myplayer/service/MusicService.kt`，底层播放实现位于 `service/playback/ExoPlayback.kt`。

现有 `History` 可以支持最近播放和简单排行，但没有保存每一次播放事件，因此无法准确生成按年份、月份、时段统计的年度报告。

年度统计功能应采用独立的跨平台播放事件协议。Android 端使用 Room 保存事件，电脑端通过 JSON/JSONL 导入或同步；协议中的 `eventId`、`deviceId`、`schemaVersion`、UTC 时间和 `canonicalId` 不依赖 Android 专有实现。

## 6. 本地构建

Debug 构建命令：

```powershell
.\gradlew.bat :app:assembleNormalDebug
```

正式构建还需要 JDK 17、Android SDK 35、NDK `25.2.9519653`、CMake 和正式签名配置。仓库当前的 `third-party/taglib` 为空时，应先执行：

```powershell
git submodule update --init --recursive
```
