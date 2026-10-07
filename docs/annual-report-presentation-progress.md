# 展示层实施进度台账

基线：`b6790aa9`（分支 `dev/play-event`）
实施包：`docs/annual-report-presentation-implementation.md`
设计文档：`docs/annual-report-presentation-design.md`

| Task | 状态 | 提交 | 构建 | 截图/验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 | ✅ | 8158f594 | 通过 | `docs/screenshots/t0.1-report-open.png`（冷启动与热启动两条路径都到达报告页）、`t0.1-before-warm.png`（证明热启动前不在该页） | 计划原方案不可行：`NavDeepLink` 构造函数为 internal，且 Activity 拿不到 composition 内的 NavController；改为 `PendingRoute` 通道，并补 `onNewIntent`（否则热启动不生效） |
| T0.2 | ✅ | a0ccbd7e | — | `tools/shots.ps1` 实际产出截图 | PowerShell 的 `>` 重定向按文本处理会损坏 PNG，改为 `screencap` 落盘后 `adb pull` |
| T0.3 | ✅ | 本次提交 | — | 本文件 | 台账建立 |

## 设备与环境

- 设备：vivo V2425A（Android 16，SDK 36），序列号 `10AEBU1SK3000LB`
- adb：`D:/Application2/Android/Sdk/platform-tools/adb.exe`
- 构建：`$env:JAVA_HOME="D:\Application2\jdk-17.0.3.1"; $env:ANDROID_HOME="D:\Application2\Android\Sdk"; .\gradlew.bat :app:assembleNormalDebug`
- 产物：`app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk`
- 出图验证用 GPU：NVIDIA RTX 4060 Ti 16GB（P3-7 资产管线可用 Flux.1-dev fp8）

## 注意事项（执行中积累）

1. **设备锁屏会导致 `adb install` 卡住**（vivo 需要亮屏解锁才给装）。用例：`adb shell input keyevent KEYCODE_WAKEUP` + `adb shell input swipe 540 2000 540 700 200` 解锁后再装。
2. **报告页数据已就绪**：2026 年真实数据存在（1531 次播放 / 90.6 小时 / 465 首歌 / 210 歌手 / 257 专辑 / 流派 Blues 347 次），截图验证有真实内容可看。
