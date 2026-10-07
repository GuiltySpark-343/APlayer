# 展示层实施进度台账

基线：`b6790aa9`（分支 `dev/play-event`）
实施包：`docs/annual-report-presentation-implementation.md`
设计文档：`docs/annual-report-presentation-design.md`

| Task | 状态 | 提交 | 构建 | 截图/验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 | ✅ | 8158f594 | 通过 | `docs/screenshots/t0.1-report-open.png`（冷启动与热启动两条路径都到达报告页）、`t0.1-before-warm.png`（证明热启动前不在该页） | 计划原方案不可行：`NavDeepLink` 构造函数为 internal，且 Activity 拿不到 composition 内的 NavController；改为 `PendingRoute` 通道，并补 `onNewIntent`（否则热启动不生效） |
| T0.2 | ✅ | a0ccbd7e | — | `tools/shots.ps1` 实际产出截图 | PowerShell 的 `>` 重定向按文本处理会损坏 PNG，改为 `screencap` 落盘后 `adb pull`；注意本机 shell 是 Windows PowerShell 5.1，**没有 `pwsh` 命令**，直接 `& <path>\shots.ps1` 调用 |
| T0.3 | ✅ | f4a13ac3 | — | 本文件 | 台账建立 |
| T1.1 | ✅ | 448d92be | 通过 | — | 新增 `ui/theme/report/ReportTokens.kt`（`ReportTokens` + `ReportTokenDefaults` + `LocalReportTokens`），默认值等于海报原配色 |
| T1.2 | ✅ | 4ec9b944 | 通过 | 待截图（设备断开，延后与 T1.3 一起补） | 海报改为消费 tokens；因文件已 import `android.graphics.Color`，Compose 的 `Color` 用别名 `ComposeColor` 导入，`paint()` 改为吃 `Color` |
| T1.3 | ✅ | 76d9fad8 | 通过 | 待截图 | `StoryCard` 真卡片；`AnnualReportScreen` 拆为 `AnnualReportScreen()`（提供 tokens）+ `AnnualReportContent()`，避免整块 Scaffold 重新缩进 |
| T1.4 | ⬜ 待做 | — | — | — | 字号收敛到 token；等设备回来与截图一起做，便于对比视觉差异 |
| T2.1 | ✅ | 本次提交 | 通过 | 待接线后截图 | `StoryPage` / `StoryPager`（背景只在 Pager 画） |
| T2.2 | ✅ | 本次提交 | 通过 | 待接线后截图 | `HeroNumber`（Animatable 800ms）/ `Caption` |
| T2.3 | ✅ | 本次提交 | 通过 | 构建即验证 | 7 个字符串 key 已加（en + zh-rCN），加前已查重 |
| T2.4 | ✅ | 本次提交 | 通过 | 待截图 | `PageCover`（S0）/ `PageOverview`（S1）+ `poster_hours_suffix`、`story_overview_caption` 两个字符串。**执行中修掉计划里的一个编译错误**：`joinToString` 的 lambda 不是 inline，不能调 `stringResource`，改为先 `forEach` 解析 |
| T2.5 | ✅ | 本次提交 | 通过 | 待截图 | `StoryPages`（页面注册表 + `visible()`）、`renderStoryPage`、`PagePlaceholder`、`ReportStoryScreen`、路由 `annual_report_story` + deep link + manifest host + 设置入口改指向叙事页。`keywords`/`share` 暂走占位页，由 T2.6 替换 |

## 阻塞

- **USB 设备断开**（Windows 只枚举到通用 USB 复合设备，无 ADB 接口），`adb devices` 为空。
  受影响：T1.2 / T1.3 的截图核对，以及后续所有需要真机截图的任务。
  恢复方式：重新插好数据线并确认手机已授权 USB 调试；恢复后先跑 `adb devices` 确认，再补截图。

## 设备与环境

- 设备：vivo V2425A（Android 16，SDK 36），序列号 `10AEBU1SK3000LB`
- adb：`D:/Application2/Android/Sdk/platform-tools/adb.exe`
- 构建：`$env:JAVA_HOME="D:\Application2\jdk-17.0.3.1"; $env:ANDROID_HOME="D:\Application2\Android\Sdk"; .\gradlew.bat :app:assembleNormalDebug`
- 产物：`app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk`
- 出图验证用 GPU：NVIDIA RTX 4060 Ti 16GB（P3-7 资产管线可用 Flux.1-dev fp8）

## 注意事项（执行中积累）

1. **设备锁屏会导致 `adb install` 卡住**（vivo 需要亮屏解锁才给装）。用例：`adb shell input keyevent KEYCODE_WAKEUP` + `adb shell input swipe 540 2000 540 700 200` 解锁后再装。
2. **本机没有 `pwsh` 命令**（shell 是 Windows PowerShell 5.1），脚本用 `& <path>` 调用。
3. **报告页数据已就绪**：2026 年真实数据存在（1531 次播放 / 90.6 小时 / 465 首歌 / 210 歌手 / 257 专辑 / 流派 Blues 347 次），截图验证有真实内容可看。
4. **`AnnualReportScreen.kt` 的 `SectionCard` 已删除**，全部改为 `StoryCard`；注意该文件里 `Column`/`fillMaxWidth` 等 import 可能已不再使用（不影响构建，但后续清理时留意）。
