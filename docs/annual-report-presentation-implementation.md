# APlayer 年度报告 · 展示层实施包

> **本文档是唯一执行依据。** 设计动机与调研见 `docs/annual-report-presentation-design.md`（那份回答"为什么"，本文档回答"怎么做"，冲突时以本文档为准）。
> 目标：执行者**不需要再做任何设计决策**，按任务逐条落地即可。
> 基线提交：`b6790aa9`（分支 `dev/play-event`）。

---

## 0. 执行须知

### 0.1 纪律（违反即回滚）

1. **只推 `origin`（`GuiltySpark-343/APlayer`），任何情况都不推 `upstream`（`rRemix/APlayer`）。**
2. **不新增任何依赖**。本实施包用到的 `palette-ktx`、`glide`、`androidx.compose.foundation.pager`、`androidx.core`、`media3` 全部已在 `gradle/libs.versions.toml` 与 `app/build.gradle.kts` 中。
3. **不改动数据层已有接口**。`PlayEventDao` 现有查询、`PlayEventRepository` 现有方法、`AnnualReport` 现有字段的**签名与语义一律不动**，只允许新增。
4. **一次只做一个 Task**，做完即按验收命令验证并提交；验证不过不得进入下一个 Task。
5. **不得改动未在本 Task 列出的文件。**
6. 每个 Task 结束必须 `:app:assembleNormalDebug` 成功。

### 0.2 环境与固定命令

```powershell
# 构建（在 S:\Workplace\APlayer 下执行）
$env:JAVA_HOME="D:\Application2\jdk-17.0.3.1"; $env:ANDROID_HOME="D:\Application2\Android\Sdk"
.\gradlew.bat :app:assembleNormalDebug --console=plain

# 构建失败且报 Gradle 类加载器/configuration-cache 相关错误时（守护进程损坏，非代码问题）
.\gradlew.bat --stop
Remove-Item -Recurse -Force ".gradle\configuration-cache" -ErrorAction SilentlyContinue
# 然后重跑构建（可加 --no-daemon）

# 产物
app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk

# 设备（vivo V2425A，Android 16）
D:/Application2/Android/Sdk/platform-tools/adb.exe devices
D:/Application2/Android/Sdk/platform-tools/adb.exe install -r "app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk"
```

**装不上时**（`INSTALL_FAILED_ABORTED: User rejected permissions`）：手机解锁亮屏后重试；仍失败则
`adb push <apk> /sdcard/Download/` 后手动安装。

### 0.3 视觉验证协议（本实施包的核心验收手段）

Compose 页面无法在 PC 端渲染，因此统一用 **真机截图 + 人工看图** 验证：

```powershell
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
& $adb shell am start -a android.intent.action.VIEW -d "aplayer://annual_report"   # 依赖 T0.1
Start-Sleep -Seconds 3
& $adb exec-out screencap -p > "docs/screenshots/<task-id>-<name>.png"
```

取图后**必须真的打开图片看**，逐项核对：

- 有无文字溢出卡片/画布、有无与相邻元素重叠、有无被截断
- 段落间距是否均匀、层级是否清晰
- 空数据页是否整页不出现
- **极端数据**：在 `AnnualReportViewModel` 临时注入（或在数据库里塞）超长歌名、5 位数播放次数、`1234.5 小时`、超长歌手名，各页都要过一遍

截图统一存放 `docs/screenshots/`，随 Task 一起提交。

### 0.4 进度台账

每个 Task 完成后，在 `docs/annual-report-presentation-progress.md` 追加一行：

```
| Task | 状态 | 提交 | 构建 | 截图/验证 | 备注 |
| T0.1 | ✅ | f1234567 | 通过 | docs/screenshots/t0.1-report-open.png | deep link 生效 |
```

该文件在 T0.3 创建，之后每个 Task 追加。

---

## 1. 基线（执行前必须全部为真）

| # | 事实 | 校验方式 |
| --- | --- | --- |
| 1 | 分支 `dev/play-event`，HEAD = `b6790aa9` | `git log --oneline -1` |
| 2 | 工作区干净 | `git status --short` 只应输出空 |
| 3 | `composeBom = "2025.09.00"`、`kotlin = "2.1.21"` | `Select-String libs.versions.toml` |
| 4 | `implementation(libs.palette.ktx)` 已存在 | `Select-String app/build.gradle.kts -Pattern palette` |
| 5 | `HorizontalPager` 已被项目使用 | `PlayingPanel.kt:74`、`widget/app/ViewPager.kt:38` |
| 6 | `ColorUtil`（Java）已有 `getColor(Palette, int)` / `getSwatch(Palette)` / `shiftColor(int, float)` / `lightenColor` / `darkenColor` | `app/src/main/java/remix/myplayer/util/ColorUtil.java:122/143/96/88/59` |
| 7 | `AppDatabase.VERSION = 10`，迁移在 `DbMigrations` 内以 `val migrationXtoY = object : Migration(X, Y)` 声明，并在 `buildDatabase()` 里 `.addMigrations(...)` | `AppDatabase.kt:69/89-96` |
| 8 | 报告路由 `RouteAnnualReport = "annual_report"`，注册于 `AppNav.kt:262`；`normalAnimatedScreen(route, arguments, deepLinks, content)` 已支持 `deepLinks` 参数（`AppNav.kt:286-302`） | 读文件 |
| 9 | manifest 已有 `aplayer` scheme 的 intent-filter（`AndroidManifest.xml:98`，host=`playing_screen`） | 读文件 |
| 10 | `AnnualReport` 字段固定（见 §2.1），`TopPlayItem.audioId: Long?` 可取到 MediaStore id | `AnnualReport.kt`、`PlayEventDao.kt:263-272` |
| 11 | `TextPrimary/TextSecondary` 签名见 `ui/widget/common/Text.kt`（`maxLine` 默认 1、`overflow` 默认 Ellipsis、`fontWeight` 可传） | 读文件 |

### 1.1 已修复项（不要重复修）

`b6790aa9` 已修：B1 时长单位 i18n、B2 海报指标值溢出、B3 海报段落贴死、B4 中文避头点、B5 榜单副标题冲出画布、B6 热力图与页脚间距。

---

## 2. 交付物与数据契约

### 2.1 可用数据（只读，不要改）

`AnnualReport`（`data/model/report/AnnualReport.kt`）：

```
year: Int
plays: Int                   listenScore: Double        completedPlays: Int
listenMs: Long               listenedDays: Int          distinctSongs/Artists/Albums: Int
firstListenedSongs: Int      addedSongs: Int            skippedPlays: Int
firstPlay/lastPlay: SongMoment?(title, artist, album, at)
topSongs: List<TopPlayItem>  topArtists: List<TopArtistItem>  topAlbums: List<TopAlbumItem>
monthDistribution: List<MonthCount(month, plays, listenedMs)>
hourDistribution: List<HourCount(hour, plays, listenedMs)>
sourceBreakdown: List<SourceCount(source, plays, listenedMs)>
weekdayDistribution: List<WeekdayCount(weekday, plays, listenedMs)>
dailyDistribution: List<DayCount(month, day, plays, listenedMs)>
loopTop: List<LoopItem(canonicalId, audioId, title, artist, album, loops, listenedMs)>
genreBreakdown: List<GenreCount(genre, plays, listenedMs)>
lateNightTopSongs: List<TopPlayItem>
```

`TopPlayItem(canonicalId, audioId: Long?, title, artist, album, listenedMs, playScore, plays)`
`TopArtistItem/TopAlbumItem(name, listenedMs, plays, songs)`

**注意**：`TopAlbumItem` **没有 albumId**。需要专辑维度封面时，一律走 `TopPlayItem.audioId → MediaStore 查 albumId`（见 T4.3），不要试图给 `TopAlbumItem` 加字段。

`ReportUiState`（`AnnualReportViewModel.kt:330-339`）：`years/year/report/previousReport/posterBitmap/sharePosterIntent/loading/exportIntent/exportRequestId`

### 2.2 交付物总表

| 类型 | 路径 | 来自 |
| --- | --- | --- |
| 设计令牌 | `ui/theme/report/ReportTokens.kt` | T1.1 |
| 卡片/骨架 | `ui/component/report/StoryCard.kt`、`StoryPage.kt`、`StoryPager.kt`、`HeroNumber.kt`、`Caption.kt` | T1.3 / T2.1 / T2.2 |
| 图表 | `ui/component/report/chart/*.kt`（7 个） | T3.1–T3.6 |
| 叙事页面 | `ui/screen/report/ReportStoryScreen.kt`、`pages/*.kt` | T2.3–T2.4、T3.7、T6.3–T6.4、T4.4 |
| 数据新增 | `data/db/room/entity/AlbumColor.kt`、`ReportOverride.kt`、对应 DAO、`DbMigrations.migration10to11/11to12` | T4.1、T6.2 |
| 封面取色 | `repo/AlbumColorRepository.kt` | T4.3 |
| 资产管线 | `docs/report-asset-spec.md`、`tools/report_assets/generate.md` | T7.1–T7.3 |
| 截图 | `docs/screenshots/*.png` | 各 Task |
| 台账 | `docs/annual-report-presentation-progress.md` | T0.3 |

---

## 3. 任务包

### 阶段 P3-0：验证基建

#### T0.1 报告页 deep link（让截图可脚本化）

**为什么**：后续每个 UI Task 都要截图核对，不能依赖人工点进设置。

> **⚠️ 执行修订（2026-10-07，已完成，提交 `8158f594`）**
> 原方案（`deepLinks = listOf(NavDeepLink(...))`）**在本项目里不可行**，两点原因：
> 1. `androidx.navigation.NavDeepLink` 的 `constructor(uriPattern, action, mimeType)` 在这个版本里是 **internal**，直接 `NavDeepLink(uri)` 编译不过；
> 2. `LocalNavController` 是 composition 内 `rememberNavController()` 提供的（`ComposeActivity:140`），Activity 的 `handleIntent()` **拿不到** NavController，而项目现有的 `playingScreenDeepLink` 也只是被 `Notify`/`ComposeActivity` 当普通 Intent data 用，并未走 navigation 的 deep link。
>
> 实际采用的方案（已验证冷启动 + 热启动两条路径）：

**改 1**：新建 `ui/nav/PendingRoute.kt`（Activity → Compose 的一次性路由通道）

```kotlin
package remix.myplayer.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 从 Activity 的 intent 到 Compose 导航的一次性请求通道。
 *
 * 为什么不用 NavDeepLink：本项目的 LocalNavController 是 composition 内
 * `rememberNavController()` 提供的，Activity 的 handleIntent() 拿不到它；
 * 用 StateFlow 暂存请求再由 AppNav 消费，可避免依赖 onResume 与首帧的先后顺序。
 */
object PendingRoute {

  val route = MutableStateFlow<String?>(null)

  fun request(route: String) {
    this.route.value = route
  }

  fun consume() {
    route.value = null
  }
}
```

**改 2**：`ui/nav/AppNav.kt`

- 在 `playingScreenDeepLink` 附近新增常量（**只用于 host 匹配，不再传给 navigation**）：
  ```kotlin
  /** 年度听歌报告（附录页）。用于真机截图验证脚本直接打开该页。 */
  val annualReportDeepLink = "aplayer://annual_report".toUri()
  ```
- **`RouteAnnualReport` 的注册保持原样** `normalAnimatedScreen(RouteAnnualReport) { AnnualReportScreen() }`（不要传 `deepLinks`）。
- 在 `AppNav()` 顶部加消费者：
  ```kotlin
  // 消费 Activity intent 登记的一次性路由请求（见 PendingRoute）
  val nav = LocalNavController.current
  val pendingRoute by PendingRoute.route.collectAsStateWithLifecycle()
  LaunchedEffect(pendingRoute) {
    pendingRoute?.let {
      PendingRoute.consume()
      nav.navigate(it)
    }
  }
  ```
- 补 import：`androidx.compose.runtime.getValue`、`androidx.lifecycle.compose.collectAsStateWithLifecycle`。

**改 3**：`ui/activity/ComposeActivity.kt`

- `handleIntent()` 的 `when (it.scheme)` 改为 `when { }`，**host 判断必须放在最前**，否则 `aplayer://annual_report` 会被当成播放页、而落到 `else` 分支还会被当歌曲 URI 去播放：
  ```kotlin
  when {
    // 报告页：只登记路由请求，由 AppNav 消费（NavController 在 composition 内）
    it.host == annualReportDeepLink.host -> PendingRoute.request(RouteAnnualReport)

    it.scheme == playingScreenDeepLink.scheme -> { /* 原逻辑不变 */ }

    else -> { /* 原逻辑不变 */ }
  }
  ```
- **必须新增 `onNewIntent`**：App 已在前台时系统走 `onNewIntent`，不会触发 `onResume`，热启动下 deep link 会失效：
  ```kotlin
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent()
  }
  ```
- 补 import：`PendingRoute`、`RouteAnnualReport`、`annualReportDeepLink`。

**改 4**：`app/src/main/AndroidManifest.xml`

在 ComposeActivity 的 `aplayer` intent-filter 内（`playing_screen` 那行之后）加：

```xml
        <data android:scheme="aplayer" android:host="annual_report" />
```

**验收（两条路径都要过）**

```powershell
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
$pkg = "remix.myplayer.debug"

# A. 冷启动：杀掉进程后直接 deep link
& $adb shell am force-stop $pkg
& $adb shell am start -a android.intent.action.VIEW -d "aplayer://annual_report"
& $adb shell screencap -p /sdcard/b.png; & $adb pull /sdcard/b.png docs/screenshots/t0.1-report-open.png

# B. 热启动：先回到首页（抽屉态），再 deep link，验证 onNewIntent
& $adb shell am start -n "$pkg/remix.myplayer.ui.activity.ComposeActivity"
& $adb shell screencap -p /sdcard/a.png; & $adb pull /sdcard/a.png docs/screenshots/t0.1-before-warm.png
& $adb shell am start -a android.intent.action.VIEW -d "aplayer://annual_report"
& $adb shell screencap -p /sdcard/b.png; & $adb pull /sdcard/b.png docs/screenshots/t0.1-report-open.png
```

两条路径截图都必须停在「年度听歌报告」页（`t0.1-before-warm.png` 用于证明热启动前**不在**该页）。

**提交**：`feat(report): open annual report via aplayer deep link`（已完成：`8158f594`）

---

#### T0.2 截图脚本（可选但推荐）

新建 `tools/shots.ps1`：

```powershell
param([Parameter(Mandatory=$true)][string]$Name, [string]$Uri = "aplayer://annual_report")
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
$dir = Join-Path $PSScriptRoot "..\docs\screenshots"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
& $adb shell am start -a android.intent.action.VIEW -d $Uri | Out-Null
Start-Sleep -Seconds 3
& $adb exec-out screencap -p > (Join-Path $dir "$Name.png")
Write-Host "saved $dir\$Name.png"
```

**验收**：`pwsh tools/shots.ps1 -Name t0.2-smoke` 能产出图片文件。

**提交**：`chore(report): add screenshot helper script`

---

#### T0.3 进度台账

新建 `docs/annual-report-presentation-progress.md`，内容：

```markdown
# 展示层实施进度台账

基线：`b6790aa9`（分支 `dev/play-event`）
实施包：`docs/annual-report-presentation-implementation.md`

| Task | 状态 | 提交 | 构建 | 截图/验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
```

**验收**：文件存在且被提交。

**提交**：`docs(report): add presentation layer progress ledger`

---

### 阶段 P3-1：基建（消 D1 / D5）

#### T1.1 设计令牌 `ReportTokens`

**新建** `app/src/main/java/remix/myplayer/ui/theme/report/ReportTokens.kt`：

```kotlin
package remix.myplayer.ui.theme.report

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils

/**
 * 报告展示层的唯一视觉来源：页面与海报共用同一份。
 * 默认值等于海报原本硬编码的配色，保证替换后无视觉回归。
 */
data class ReportTokens(
  val accent: Color,
  val accentSoft: Color,
  val bgTop: Color,
  val bgBottom: Color,
  val cardBg: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textFooter: Color,
  val pagePadding: Dp,
  val cardPadding: Dp,
  val cardGap: Dp,
  val cardRadius: Dp,
  val sectionGap: Dp,
  val titleSize: TextUnit,
  val bodySize: TextUnit,
  val captionSize: TextUnit,
  val heroSize: TextUnit,
  val chartHeight: Dp
)

object ReportTokenDefaults {

  val Dark = ReportTokens(
    accent = Color(0xFF8C9BFF),
    accentSoft = Color(0xFF5C63A8),
    bgTop = Color(0xFF141628),
    bgBottom = Color(0xFF2A2450),
    cardBg = Color(0x22FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB9BCDA),
    textFooter = Color(0xFF7A7E9E),
    pagePadding = 20.dp,
    cardPadding = 16.dp,
    cardGap = 10.dp,
    cardRadius = 16.dp,
    sectionGap = 10.dp,
    titleSize = 16.sp,
    bodySize = 14.sp,
    captionSize = 12.sp,
    heroSize = 56.sp,
    chartHeight = 120.dp
  )

  /**
   * 由专辑封面主色派生整套配色（S2 音乐颜色）。
   * 规则固定：保留色相，饱和度/明度夹到可读区间；背景取同色相深色。
   */
  fun fromAccent(seedArgb: Int): ReportTokens {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedArgb, hsl)
    val hue = hsl[0]
    fun at(hueOffset: Float, s: Float, l: Float): Color =
      Color(ColorUtils.HSLToColor(floatArrayOf((hue + hueOffset + 360f) % 360f, s, l)))
    return Dark.copy(
      accent = at(0f, 0.62f, 0.68f),
      accentSoft = at(0f, 0.40f, 0.42f),
      bgTop = at(0f, 0.28f, 0.09f),
      bgBottom = at(18f, 0.36f, 0.17f)
    )
  }
}

val LocalReportTokens = staticCompositionLocalOf { ReportTokenDefaults.Dark }
```

**验收**：`:app:assembleNormalDebug` 通过（本 Task 只新增文件，不接线）。

**提交**：`feat(report): add shared design tokens for report presentation`

---

#### T1.2 海报接入令牌（消 D5 的一半）

**改** `ui/screen/report/ReportPoster.kt`：

1. 删除第 58-64 行的 7 个 `COLOR_*` 常量与 `private fun COLOR_SUB()`。
2. `render` 签名加默认参数：
   ```kotlin
   fun render(
     context: Context,
     report: AnnualReport,
     story: ReportStoryResult,
     tokens: ReportTokens = ReportTokenDefaults.Dark
   ): Bitmap
   ```
3. `paint()` 从「接受 hex 字符串」改为「接受 `Color`」：
   ```kotlin
   private fun paint(color: Color, size: Float, bold: Boolean = false): Paint =
     Paint(Paint.ANTI_ALIAS_FLAG).apply {
       this.color = color.toArgb()
       textSize = size
       typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
     }
   ```
   补 import：`androidx.compose.ui.graphics.Color`、`androidx.compose.ui.graphics.toArgb`。
4. 全文替换（**逐处对应，不遗漏**）：

   | 原 | 新 |
   | --- | --- |
   | `paint(COLOR_TEXT, 132f, true)` | `paint(tokens.textPrimary, 132f, true)` |
   | `paint(COLOR_SUB(), 42f)` | `paint(tokens.textSecondary, 42f)` |
   | `paint(COLOR_ACCENT, 60f, true)` | `paint(tokens.accent, 60f, true)` |
   | `paint("#E6E7F2", 34f)` | `paint(tokens.textPrimary, 34f)` |
   | `paint(COLOR_TEXT_SUB, 26f)` | `paint(tokens.textSecondary, 26f)` |
   | `paint(COLOR_ACCENT, 36f, true)` / `34f` | `paint(tokens.accent, 36f, true)` / `34f` |
   | `paint(COLOR_TEXT, 36f)` / `28f` | `paint(tokens.textPrimary, 36f)` / `paint(tokens.textSecondary, 28f)` |
   | `paint(COLOR_TEXT_SUB, 28f)` | `paint(tokens.textSecondary, 28f)` |
   | `paint(COLOR_FOOTER, 26f)` | `paint(tokens.textFooter, 26f)` |
   | `Color.parseColor(COLOR_CARD)` | `tokens.cardBg.toArgb()` |
   | `Color.parseColor(COLOR_BG_TOP)` / `COLOR_BG_BOTTOM` | `tokens.bgTop.toArgb()` / `tokens.bgBottom.toArgb()` |
   | `Color.parseColor(COLOR_ACCENT)`（热力图） | `tokens.accent.toArgb()` |

5. `drawBackground` / `drawMetrics` / `drawList` / `drawHeatmap` 的签名都要加 `tokens: ReportTokens` 参数（`drawMetrics` 已有 `context`，追加在末尾）。

**验收**

1. 构建通过。
2. 用 `.poster_mock.py` 重出预览不适用于本步（PIL 脚本是复现，不读 Kotlin）；改为**看图对比**：安装后进入报告页 → 点「分享卡片」→ 截图，与 `docs/samples/poster-preview.png` 的版式一致（配色与排布无回归即通过）。
3. 记录：`docs/screenshots/t1.2-poster-after-tokens.png`。

**提交**：`refactor(report): drive poster colors from shared tokens`

---

#### T1.3 真卡片 `StoryCard`（消 D1）

**新建** `app/src/main/java/remix/myplayer/ui/component/report/StoryCard.kt`：

```kotlin
package remix.myplayer.ui.component.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/**
 * 报告页的真实卡片：圆角 + 半透明背景 + 内边距。
 * 注意 Modifier 顺序：padding(外边距) → clip → background → padding(内边距)。
 */
@Composable
fun StoryCard(
  title: String? = null,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = tokens.pagePadding, vertical = tokens.cardGap / 2)
      .clip(RoundedCornerShape(tokens.cardRadius))
      .background(tokens.cardBg)
      .padding(tokens.cardPadding)
  ) {
    if (title != null) {
      TextPrimary(text = title, fontSize = tokens.titleSize, color = tokens.accent)
      Spacer(Modifier.height(8.dp))
    }
    content()
  }
}
```

补 import：`androidx.compose.ui.unit.dp`。

**改** `ui/screen/report/AnnualReportScreen.kt`：

1. 删除文件末尾的 `SectionCard`（原 648-660 行）。
2. 全文把 `SectionCard(` 替换为 `StoryCard(`，并把原来 `SectionCard(title = ...)` 的写法保留不变（签名兼容）。
3. 加上 `StoryCard` 的 import；删掉因此不再使用的 import（若有）。
4. **接线令牌**：在 `AnnualReportScreen()` 的 `Scaffold` 外层包一层，让附录页也用同一套 token：

   ```kotlin
   CompositionLocalProvider(LocalReportTokens provides ReportTokenDefaults.Dark) {
     Scaffold(...) { ... }
   }
   ```

   补 import：`androidx.compose.runtime.CompositionLocalProvider`、`remix.myplayer.ui.theme.report.LocalReportTokens`、`remix.myplayer.ui.theme.report.ReportTokenDefaults`。

5. 章节之间的 `Spacer(Modifier.height(8.dp))` 统一改成 `Spacer(Modifier.height(tokens.sectionGap))`；
   在 `AnnualReportScreen()` 顶部取 `val tokens = LocalReportTokens.current`。
   （卡片自带 `cardGap/2` 的上下外边距，`sectionGap` 只用于卡片之外的间隙，避免双重间距过大。）

**验收**

1. 构建通过；deep link 进页面截图 `docs/screenshots/t1.3-cards.png`。
2. 人工看图确认：每个 section 都有可见的圆角半透明卡片；卡片之间间距均匀；无文字压边。
3. 与 T0.1 的基线截图对比，确认只是"加卡片"，没有错位。

**提交**：`feat(report): render report sections as real cards`

---

#### T1.4 字号与间距阶梯收口

**改** `ui/screen/report/AnnualReportScreen.kt`：把散落的 `fontSize = 16.sp / 14.sp / 13.sp / 12.sp` 归到 token：

- 小节标题 → `tokens.titleSize`
- 正文/榜单标题 → `tokens.bodySize`
- 说明/次要 → `tokens.captionSize`

**允许保留例外**：`YearSelector` 的年号（16.sp）、`HeatmapCard` 里 7px 色块与月份小字（9.sp）、`HourCard` 的刻度小字（9.sp）——这些是图表内部尺寸，不属于阶梯。

**验收**：构建通过 + 截图 `docs/screenshots/t1.4-type-scale.png`，确认视觉层级仍然清晰（标题 > 正文 > 说明）。

**提交**：`refactor(report): consolidate type scale onto tokens`

---
### 阶段 P3-2：叙事骨架

> 本阶段产物是**一条新的横向分页主线**，老页面（仪表盘）原样保留、降级为附录。

#### T2.1 `StoryPage` / `StoryPager`

**新建** `ui/component/report/StoryPage.kt`：

```kotlin
package remix.myplayer.ui.component.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens

/**
 * 一页的内边距与居中：背景由 StoryPager 统一绘制，这里不画背景，避免叠加。
 */
@Composable
fun StoryPage(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = tokens.pagePadding, vertical = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content
  )
}
```

**新建** `ui/component/report/StoryPager.kt`：

```kotlin
package remix.myplayer.ui.component.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens

/** 报告主线：横向分页 + 底部进度点。背景渐变在此统一绘制。 */
@Composable
fun StoryPager(
  pageCount: Int,
  modifier: Modifier = Modifier,
  state: PagerState = rememberPagerState(pageCount = { pageCount }),
  content: @Composable (Int) -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(tokens.bgTop, tokens.bgBottom)))
  ) {
    HorizontalPager(state = state, modifier = Modifier.weight(1f)) { page -> content(page) }
    StoryProgress(current = state.currentPage, count = pageCount)
  }
}

@Composable
private fun StoryProgress(current: Int, count: Int) {
  val tokens = LocalReportTokens.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 20.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    repeat(count) { index ->
      Box(
        modifier = Modifier
          .padding(horizontal = 3.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(if (index == current) tokens.accent else tokens.textSecondary.copy(alpha = 0.3f))
      )
    }
  }
}
```

**验收**：构建通过（本 Task 只新增组件，尚未接线）。

**提交**：`feat(report): add story page and pager components`

---

#### T2.2 `HeroNumber` / `Caption`

**新建** `ui/component/report/HeroNumber.kt`：

```kotlin
package remix.myplayer.ui.component.report

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/**
 * 会滚动到目标值的大数字。动画时长固定 800ms，重复进入同一页会重播。
 * [format] 负责把当前动画值转成字符串，避免在组件内猜测业务格式。
 */
@Composable
fun HeroNumber(
  target: Float,
  format: (Float) -> String,
  modifier: Modifier = Modifier,
  suffix: String = "",
  durationMs: Int = 800
) {
  val tokens = LocalReportTokens.current
  val anim = remember { Animatable(0f) }
  LaunchedEffect(target) {
    anim.snapTo(0f)
    anim.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing))
  }
  TextPrimary(
    text = format(anim.value) + suffix,
    modifier = modifier,
    fontSize = tokens.heroSize,
    fontWeight = FontWeight.Bold,
    color = tokens.accent
  )
}
```

**新建** `ui/component/report/Caption.kt`：

```kotlin
package remix.myplayer.ui.component.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** 一句情绪文案。默认最多 3 行、居中、次要色。 */
@Composable
fun Caption(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = LocalReportTokens.current.textSecondary,
  fontSize: TextUnit = LocalReportTokens.current.bodySize,
  maxLine: Int = 3
) {
  TextPrimary(
    text = text,
    modifier = modifier,
    fontSize = fontSize,
    color = color,
    maxLine = maxLine,
    textAlign = TextAlign.Center
  )
}
```

**验收**：构建通过。

**提交**：`feat(report): add hero number and caption components`

---

#### T2.3 新增字符串资源

**改** `app/src/main/res/values/strings.xml` 与 `values-zh-rCN/strings.xml`。

**本 Task 先加 §7 表中标为 T2.3 的 7 个 key**（`story_start_hint`、`story_all_data`、`story_year_best`、`story_swap`、`story_genre_evolution`、`story_color`、`story_color_caption`）；
其余 key 在各自 Task 里从 **§7 附录：新增字符串总表** 取用——**§7 是唯一清单，本处不再重复列出文案**（避免两处不一致）。

**注意**：`share`、`close`、`annual_report`、`stat_*`、`kw_*`、`poster_*` 均已存在，**不要重复定义**（曾因重复 `share` 导致 `mergeNormalDebugResources` 失败）。加之前按 §7 顶部给的单行命令查重。

**验收**：`:app:mergeNormalDebugResources` 通过（构建即可覆盖）。

**提交**：`feat(report): add story flow strings`

---

#### T2.4 S0 / S1 两页

**新建** `ui/screen/report/pages/PageCover.kt`：

```kotlin
package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.screen.report.ReportStoryResult
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** S0 开场：年份 + 主题词 + 开始提示。 */
@Composable
fun PageCover(report: AnnualReport, story: ReportStoryResult) {
  val tokens = LocalReportTokens.current
  StoryPage {
    TextPrimary(
      text = report.year.toString(),
      fontSize = tokens.heroSize * 2f,
      fontWeight = FontWeight.Bold,
      color = tokens.textPrimary
    )
    Spacer(Modifier.height(48.dp))
    TextPrimary(
      text = keywordLabels.joinToString(" · "),
      fontSize = tokens.titleSize * 1.5f,
      fontWeight = FontWeight.Bold,
      color = tokens.accent
    )
    Spacer(Modifier.height(24.dp))
    Caption(stringResource(R.string.story_start_hint), color = tokens.textFooter)
  }
}
```

> `TextUnit` 的乘法只使用 `* Float` 形式（`* 2f`），不要写 `* 2`，避免依赖不确定的重载。

> **⚠️ `stringResource` 不能在 `joinToString` 里调用**：`joinToString` 的 transform 参数**不是 inline lambda**，
> 在其中调用 `@Composable` 会编译报错 `@Composable invocations can only happen from the context of a @Composable function`。
> 正确写法是先用 `forEach`（inline，允许）解析成字符串：
>
> ```kotlin
> val keywordLabels = ArrayList<String>(story.keywords.size)
> story.keywords.forEach { keywordLabels.add(stringResource(it.titleRes)) }
> ```
>
> 然后 `keywordLabels.joinToString(" · ")`。此坑已在执行中实际踩到并修正（见自检 5）。

**新建** `ui/screen/report/pages/PageOverview.kt`：

```kotlin
package remix.myplayer.ui.screen.report.pages

/** S1 总览：一年听了多久。 */
@Composable
fun PageOverview(report: AnnualReport, context: Context) {
  val tokens = LocalReportTokens.current
  StoryPage {
    HeroNumber(
      target = report.listenMs / 3_600_000f,
      format = { "%.1f".format(it) },
      suffix = " " + context.getString(R.string.poster_hours_suffix)
    )
    Spacer(Modifier.height(16.dp))
    Caption(
      context.getString(
        R.string.story_overview_caption,
        report.plays, report.distinctSongs, report.listenedDays
      )
    )
  }
}
```

配套新增两个字符串（加入 T2.3 的同一批）：

| key | en | zh-rCN |
| --- | --- | --- |
| `poster_hours_suffix` | h | 小时 |
| `story_overview_caption` | %1$d plays · %2$d songs · %3$d days | %1$d 次播放 · %2$d 首歌 · %3$d 天 |

> `poster_hours` 是带格式化的 `%1$.1f 小时`，不能直接取"单位"，因此单独加 `poster_hours_suffix`。

**验收**：构建通过（页面此时尚未接进导航）。

**提交**：`feat(report): add cover and overview story pages`

---

#### T2.5 叙事页容器 + 导航接线 + 附录降级

**新建** `ui/screen/report/ReportStoryScreen.kt`：

```kotlin
package remix.myplayer.ui.screen.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import remix.myplayer.ui.component.report.StoryPager
import remix.myplayer.ui.screen.report.pages.PageCover
import remix.myplayer.ui.screen.report.pages.PageOverview
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.theme.report.ReportTokenDefaults
import remix.myplayer.ui.theme.report.ReportTokens
import remix.myplayer.viewmodel.annualReportViewModel

/**
 * 报告主线（叙事流）。页面集合是动态的：数据不足的页不进入列表。
 */
@Composable
fun ReportStoryScreen(nav: NavController) {
  val viewModel = annualReportViewModel
  val state by viewModel.state.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    if (state.report == null) viewModel.load()
  }

  val report = state.report
  if (report == null) {
    Box(Modifier.fillMaxSize())   // 加载中：留白，不显示"暂无数据"（老页面负责该提示）
    return
  }

  val story = ReportStory.analyze(report)
  val tokens: ReportTokens = ReportTokenDefaults.Dark   // T4.4 换为按专辑主色派生
  val pages = StoryPages.visible(report)

  CompositionLocalProvider(LocalReportTokens provides tokens) {
    Box(Modifier.fillMaxSize()) {
      StoryPager(pageCount = pages.size) { page ->
        StoryPages.render(page, report, story, context, nav, viewModel)
      }
    }
  }
}
```

**新建** `ui/screen/report/StoryPages.kt`（页面注册表 + 充分性判断，**这是唯一的"页面集合"定义处**）：

```kotlin
package remix.myplayer.ui.screen.report

/**
 * 页面注册表。新增页面只改这里。
 * 顺序即叙事顺序；show 为 false 的页整页不出现（学网易云"数据不足则不展示"）。
 * 注意：不要放"永不展示"的占位条目，未实现的页等到对应 Task 再加。
 */
object StoryPages {

  data class Entry(val id: String, val show: (AnnualReport) -> Boolean)

  val entries: List<Entry> = listOf(
    Entry("cover", { true }),
    Entry("overview", { it.plays > 0 }),
    Entry("highlights", { it.topSongs.isNotEmpty() || it.loopTop.isNotEmpty() }),
    Entry("peak_hour", { it.hourDistribution.isNotEmpty() }),
    Entry("night", { lateNightRatio(it) >= 0.02 }),
    Entry("calendar", { it.dailyDistribution.isNotEmpty() }),
    Entry("loop_king", { it.loopTop.isNotEmpty() }),
    Entry("repeat_explore", { it.plays > 0 }),
    Entry("sources", { it.sourceBreakdown.isNotEmpty() }),
    Entry("keywords", { true }),
    Entry("share", { true })
  )

  /** 当前数据下实际展示的页面（顺序即索引顺序）。渲染与计数都必须走这里，避免两处条件不一致。 */
  fun visible(report: AnnualReport): List<Entry> = entries.filter { it.show(report) }

  private fun lateNightRatio(report: AnnualReport): Double {
    val plays = report.plays
    if (plays <= 0) return 0.0
    return report.hourDistribution.filter { it.hour in 0..5 }.sumOf { it.plays }.toDouble() / plays
  }
}
```

**同时新建** `ui/screen/report/pages/PagePlaceholder.kt`（未实现页的临时占位，避免引用未定义符号）：

```kotlin
package remix.myplayer.ui.screen.report.pages

import androidx.compose.runtime.Composable
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage

/** 尚未实现的页面：只显示页面 id，便于截图确认页序。 */
@Composable
fun PagePlaceholder(id: String) {
  StoryPage { Caption(id) }
}
```

`StoryPages.render(page, report, story, context, nav)` 用 `visible(report).getOrNull(index)?.id` 分发到各页面组件（本阶段先实现 `cover` / `overview` / `keywords` / `share`，其余 id 走 `PagePlaceholder`，由后续 Task 逐个替换）：

```kotlin
/** 页面 index → 组件。索引必须基于 visible(report)，与 StoryPager 的 pageCount 同源。 */
@Composable
fun render(
  index: Int,
  report: AnnualReport,
  story: ReportStoryResult,
  context: Context,
  nav: NavController,
  viewModel: AnnualReportViewModel
) {
  when (visible(report).getOrNull(index)?.id) {
    "cover" -> PageCover(report, story)
    "overview" -> PageOverview(report, context)
    "keywords" -> PageKeywords(report, story)
    "share" -> PageShare(report, nav, viewModel)
    else -> PagePlaceholder(visible(report).getOrNull(index)?.id ?: "")
  }
}
```

`ReportStoryScreen` 内相应改为：

```kotlin
val pages = StoryPages.visible(report)
StoryPager(pageCount = pages.size) { page ->
  StoryPages.render(page, report, story, context, nav, viewModel)
}
```

**导航改动** `ui/nav/AppNav.kt`：

1. 新增 `const val RouteAnnualReportStory = "annual_report_story"`。
2. 新增 `val annualReportStoryDeepLink = "aplayer://annual_report_story".toUri()`。
3. 注册：
   ```kotlin
   normalAnimatedScreen(
     RouteAnnualReportStory,
     deepLinks = listOf(NavDeepLink(annualReportStoryDeepLink))
   ) {
     ReportStoryScreen(LocalNavController.current)
   }
   ```
4. manifest 的 `aplayer` intent-filter 里加 `<data android:scheme="aplayer" android:host="annual_report_story" />`。

**入口改动** `ui/screen/setting/SettingScreen.kt:78-80`：把 `nav.navigate(RouteAnnualReport)` 改为 `nav.navigate(RouteAnnualReportStory)`。

**附录降级**：`AnnualReportScreen` 顶部 `CommonAppBar` 保持不变；**保留其全部 16 个 section 与 `ActionRow`（导出/导入/清空/生成歌单/分享），不要加任何"查看全部数据"入口**——它自己就是"全部数据"页。「查看全部数据」按钮只放在叙事页的 S12（见 T2.6），点击后 `nav.navigate(RouteAnnualReport)`。

**验收**

1. 构建通过。
2. `& $adb shell am start -a android.intent.action.VIEW -d "aplayer://annual_report_story"` → 截图 `docs/screenshots/t2.5-story-cover.png`；左右滑动确认能翻到 S1、S11、S12。
3. 从设置页进入也走新页面（截图 `docs/screenshots/t2.5-from-settings.png`）。

**提交**：`feat(report): add story flow screen with pager navigation`

---

#### T2.6 S11 关键词页 / S12 分享页

**新建** `ui/screen/report/pages/PageKeywords.kt`：关键词标签组 + 三行概要 + `Caption`。用 `FlowRow`（`androidx.compose.foundation.layout.FlowRow`，`@OptIn(ExperimentalLayoutApi::class)`）渲染 `story.keywords`，每个标签：圆角胶囊 + `cardBg` 背景 + `accent` 文字。

**新建** `ui/screen/report/pages/PageShare.kt`：

```kotlin
/**
 * S12 分享页：进入时用 ViewModel 生成海报，展示缩略图 + 分享按钮 + 查看全部数据。
 */
@Composable
fun PageShare(report: AnnualReport, nav: NavController, viewModel: AnnualReportViewModel)
```

行为规定：

1. `LaunchedEffect(report.year) { viewModel.generatePoster() }` 生成海报；
2. `viewModel.state.collectAsState().value.posterBitmap` 为 `null` 时显示 `Caption("生成中…")`（新增字符串 `story_poster_loading`：en `Rendering…` / zh `生成中…`）；
3. 非空时 `Image(bitmap.asImageBitmap(), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(360.dp))`；
4. 两个按钮：`stringResource(R.string.share)` → `viewModel.sharePoster()`；`stringResource(R.string.story_all_data)` → `nav.navigate(RouteAnnualReport)`；
5. `viewModel.consumeSharePosterIntent()` 与老页面一致，放在分享完成后由 `LaunchedEffect` 处理（照抄 `AnnualReportScreen` 里现有的用法）。

配套字符串（并入 T2.3 批次）：`story_poster_loading`。

**验收**：截图 `docs/screenshots/t2.6-keywords.png`、`docs/screenshots/t2.6-share.png`；确认海报缩略图正常、分享能拉起系统面板。

**提交**：`feat(report): add keywords and share story pages`

---

### 阶段 P3-3：图表层

> 所有图表统一用 `Canvas` + `rememberTextMeasurer()`/`drawText` 绘制（Compose BOM 2025.09 支持），**不引入任何图表库**。
> 统一约定：
> - 空数据（列表为空）时组件 `return`，不画任何东西；
> - 颜色全部来自 `LocalReportTokens.current`；
> - 数值标注字号 = `tokens.captionSize`，颜色 = `tokens.textSecondary`；
> - 每个图表带**进入动画**：`animateFloatAsState(targetValue = 1f, tween(600))` 作为生长进度 `p ∈ [0,1]`，所有几何量乘以 `p`。

#### T3.1 `ChartFrame`（图表外壳）

**新建** `ui/component/report/chart/ChartFrame.kt`：

```kotlin
@Composable
fun ChartFrame(
  title: String,
  modifier: Modifier = Modifier,
  height: Dp = LocalReportTokens.current.chartHeight,
  legend: (@Composable () -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
)
```

结构：`Column { 标题(accent, titleSize) ; Spacer(8.dp) ; Box(Modifier.fillMaxWidth().height(height)) { content() } ; legend?.invoke() }`。

**验收**：构建通过。

**提交**：`feat(report): add chart frame`

---

#### T3.2 `PolarClockChart`（S5 最爱听歌时段）

**新建** `ui/component/report/chart/PolarClockChart.kt`：

```kotlin
@Composable
fun PolarClockChart(
  hours: List<HourCount>,
  modifier: Modifier = Modifier,
  height: Dp = 200.dp
)
```

绘制规则（`Canvas` 内）：

1. `counts = IntArray(24)`，由 `hours` 填充；`max = counts.max()`；`max <= 0` 直接 return；
2. 圆心 = 画布中心，`innerR = min(w,h)/2 * 0.35f`，`outerR = min(w,h)/2 * 0.95f`；
3. 每小时的扇区：`startAngle = hour * 15f - 90f`，`sweep = 13.5f`（留 1.5° 缝隙）；半径 `r = innerR + (outerR - innerR) * (counts[hour]/max) * p`；
4. 画法：`drawArc(color = accent.copy(alpha = 0.35f + 0.65f * ratio), startAngle, sweep, useCenter = false, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round), topLeft = Offset(cx - r, cy - r), size = Size(r*2, r*2))`；
5. 峰值小时用 `accent` 实色描边，并在圆心用 `drawText` 写 `"%d:00"`（`peakHour`），副行写 `stat_hours` 文案；
6. 四个刻度（0/6/12/18）在 `outerR + 14.dp` 处用 `drawText` 标注。

**验收**：截图（S5 页）确认扇区方向正确（0 点在正上方、顺时针）、峰值最长的扇区与数据库 `hourDistribution` 的峰值一致。

**提交**：`feat(report): add polar clock chart`

---

#### T3.3 `CalendarHeatmap`（S7 年度年历）

**新建** `ui/component/report/chart/CalendarHeatmap.kt`：

```kotlin
@Composable
fun CalendarHeatmap(
  days: List<DayCount>,
  modifier: Modifier = Modifier,
  monthLabelWidth: Dp = 28.dp
)
```

绘制规则：

1. 网格 12 行（月）× 31 列（日）；`days` 为空 return；
2. cell 尺寸由可用宽度反推：`cell = (size.width - monthLabelWidth.toPx() - 30 * gap) / 31`，`gap = 2.dp.toPx()`；**不允许写死 cell 尺寸**（这是 B5 的教训）；
3. 色阶 5 档：`ratio = ms / maxMs`，alpha = `0.08f`（无数据）/ `0.25f / 0.45f / 0.7f / 1.0f`（按 ratio 分档），颜色 = `accent`；
4. 月份标签：1..12 在左侧用 `drawText` 写 `"%02d"`，颜色 `textSecondary`，字号 `captionSize`；
5. 底部 legend：`Row { Caption("少") ; 5 个色块 ; Caption("多") }`（复用已有文案或新增 `chart_legend_less/more`：zh `少`/`多`，en `Less`/`More`）；
6. 生长动画：按行（月）依次显示，第 m 行的 `p` 用 `((progress * 12f) - m).coerceIn(0f, 1f)`。

**验收**：截图确认 12×31 网格完整、右侧不超出画布、月份标签对齐；把某一格数据改成极大值确认色阶封顶。

**提交**：`feat(report): add calendar heatmap chart`

---

#### T3.4 `TrendLineChart`（S3 曲线）

```kotlin
@Composable
fun TrendLineChart(
  months: List<MonthCount>,
  modifier: Modifier = Modifier,
  height: Dp = 140.dp
)
```

规则：X 轴 1..12 等分；Y 由 `listenedMs` 归一化；画 4 条水平网格线（`textSecondary.copy(alpha=0.15f)`，`Stroke(1.dp)`）；折线用 `Path` + `Stroke(2.dp, cap=Round)`；折线下方用同色 `alpha=0.18f` 的 `Path` 填充到底边；仅在最大值点与两端标数值；X 轴刻度 1/4/7/10（`textSecondary`, `captionSize`）；生长动画：`PathMeasure` 截取前 `p` 比例。

**验收**：截图确认折线与 `monthDistribution` 峰值一致、两端不越界。

**提交**：`feat(report): add trend line chart`

---

#### T3.5 `DonutChart`（S10 来源占比）

```kotlin
@Composable
fun DonutChart(
  slices: List<Triple<String, Long, Int>>,   // (label, value, colorArgb)
  modifier: Modifier = Modifier,
  size: Dp = 160.dp
)
```

规则：由 `slices.value` 求总和，逐段 `drawArc(useCenter = false, style = Stroke(18.dp.toPx()))`，起始角 `-90f`；每段之间留 1° 缝隙；圆心用 `drawText` 写总占比或 top1 标签；图例在右侧 `Column`，每行 = 8dp 色点 + 标签 + 百分比；生长动画按 `p` 累加 sweep。

颜色分配：`colors = listOf(tokens.accent, tokens.accentSoft, tokens.textSecondary)` 循环取用（来源上限 3 个）。

**验收**：截图确认弧段总和为整圆（无缝隙累积误差）、图例与 `sourceBreakdown` 一致。

**提交**：`feat(report): add donut chart`

---

#### T3.6 `DualBarCompare` / `PaletteStrip` / `GenreBands`

```kotlin
@Composable
fun DualBarCompare(
  leftLabel: String, leftValue: Float,
  rightLabel: String, rightValue: Float,
  leftText: String, rightText: String,
  modifier: Modifier = Modifier
)

@Composable
fun PaletteStrip(colors: List<Int>, modifier: Modifier = Modifier, height: Dp = 72.dp)

@Composable
fun GenreBands(
  stages: List<GenreStage>,   // data class GenreStage(val label: String, val genres: List<GenreCount>)
  modifier: Modifier = Modifier
)
```

- `DualBarCompare`：两条水平条，左条从左边生长、右条从右边生长，长度按 `value / max(left,right)`，颜色 `textSecondary` / `accent`；两端各写 `leftText` / `rightText`；用于 S9 与多年对比。
- `PaletteStrip`：`colors` 逐个画等宽圆角矩形（`Row` + `weight(1f)` + `RoundedCornerShape(8.dp)`），色块依次填充（生长动画按 `p * colors.size` 决定每个色块是否已显示与透明度）；下方 `Row` 写 `"#RRGGBB"` 或色名（本阶段只写 hex，字号 `captionSize`）。
- `GenreBands`：按 `stages` 顺序横排分段色带，每段宽度按该阶段 `plays` 总和占比；段内再按各 genre 的 `plays` 细分颜色（同一段内用 `accent` 的不同 alpha 区分），段下方写阶段标签。

**验收**：三者的截图分别确认无溢出、无零长度条、色块数量正确。

**提交**：`feat(report): add compare, palette and genre band charts`

---

#### T3.7 图表页接线（S5 / S7 / S9 / S10）

**新建** `ui/screen/report/pages/PagePeakHour.kt`、`PageCalendar.kt`、`PageRepeatExplore.kt`、`PageSources.kt`，各自 `StoryPage { ChartFrame(...) { <图表> } }`。

**改** `StoryPages.kt`：把对应 id 从 `PagePlaceholder` 换成真实页面；`entries` 的 `show` 条件维持不变。

**验收**：四页截图（`docs/screenshots/t3.7-*.png`），逐页核对数据与数据库一致。

**提交**：`feat(report): wire chart pages into story flow`

---

### 阶段 P3-4：音乐颜色

#### T4.1 新增 `album_colors` 表 + 迁移

**新建** `data/db/room/entity/AlbumColor.kt`：

```kotlin
package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 专辑封面主色缓存：albumId(MediaStore) → ARGB。 */
@Entity(tableName = "album_colors")
data class AlbumColor(
  @PrimaryKey val albumId: Long,
  val color: Int,
  val updatedAt: Long
)
```

**新建** `data/db/room/dao/AlbumColorDao.kt`：

```kotlin
package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import remix.myplayer.data.db.room.entity.AlbumColor

@Dao
interface AlbumColorDao {

  @Query("SELECT * FROM album_colors WHERE albumId IN (:ids)")
  suspend fun byIds(ids: List<Long>): List<AlbumColor>

  @Query("SELECT * FROM album_colors ORDER BY updatedAt DESC LIMIT :limit")
  suspend fun recent(limit: Int): List<AlbumColor>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(rows: List<AlbumColor>)

  @Query("DELETE FROM album_colors")
  suspend fun clear()
}
```

**改** `data/db/DbMigrations.kt` 末尾追加：

```kotlin
  val migration10to11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `album_colors` (`albumId` INTEGER NOT NULL, `color` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
      )
    }
  }
```

**改** `data/db/room/AppDatabase.kt`：

1. `const val VERSION = 11`；
2. `entities` 数组加 `AlbumColor::class`；
3. 加 `abstract fun albumColorDao(): AlbumColorDao`；
4. `buildDatabase()` 的链式调用加 `.addMigrations(migration10to11)`；
5. 补 import（`AlbumColor`、`AlbumColorDao`、`migration10to11`）。

**验收**：构建通过；安装后**从旧版本升级**（覆盖安装）不崩溃，且 `adb shell run-as remix.myplayer.debug sqlite3 ...` 或重新进入报告页能正常跑（Room 会在升级时执行迁移；若 `IllegalStateException: Migration didn't properly handle` 说明 SQL 与实体不一致）。

**提交**：`feat(report): add album color cache table`

---

#### T4.2 封面取色仓库

**新建** `repo/AlbumColorRepository.kt`：

```kotlin
package remix.myplayer.repo

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.AlbumColorDao
import remix.myplayer.data.db.room.entity.AlbumColor
import remix.myplayer.util.ColorUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumColorRepository @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val dao: AlbumColorDao
) {

  /** albumId → ARGB。命中缓存则直接返回，未命中才读封面取色。 */
  suspend fun colorsForAudioIds(audioIds: List<Long>, fallbackArgb: Int, limit: Int = 100): Map<Long, Int> =
    withContext(Dispatchers.IO) {
      val albumIds = resolveAlbumIds(audioIds).distinct().take(limit)
      if (albumIds.isEmpty()) return@withContext emptyMap()

      val cached = dao.byIds(albumIds).associate { it.albumId to it.color }
      val missing = albumIds.filterNot { cached.containsKey(it) }

      val fresh = ArrayList<AlbumColor>(missing.size)
      val result = HashMap<Long, Int>(cached)
      for (albumId in missing) {
        val color = extractColor(albumId) ?: fallbackArgb
        fresh.add(AlbumColor(albumId, color, System.currentTimeMillis()))
        result[albumId] = color
      }
      if (fresh.isNotEmpty()) dao.upsert(fresh)
      result
    }

  private fun resolveAlbumIds(audioIds: List<Long>): List<Long> {
    if (audioIds.isEmpty()) return emptyList()
    val out = ArrayList<Long>(audioIds.size)
    val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
    for (audioId in audioIds) {
      runCatching {
        context.contentResolver.query(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          projection,
          "_id = ?",
          arrayOf(audioId.toString()),
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) out.add(cursor.getLong(0))
        }
      }
    }
    return out
  }

  private fun extractColor(albumId: Long): Int? = runCatching {
    val uri: Uri = ContentUris.withAppendedId(
      Uri.parse("content://media/external/audio/albumart/"), albumId
    )
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
      ?: return@runCatching null
    // 只取缩略图：先读边界，再按 160px 目标降采样，避免整张大图进内存
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 160) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return@runCatching null
    val palette = Palette.from(bitmap).generate()
    bitmap.recycle()
    ColorUtil.getColor(palette, 0)
  }.getOrNull()?.takeIf { it != 0 }
}
```

**注意**：`ColorUtil.getColor(Palette, int)` 已存在（`ColorUtil.java:122`），直接用，**不要**自己写"取哪个 swatch"的逻辑。`fallback` 用 `ReportTokenDefaults.Dark.accent.toArgb()`。

**验收**：临时在 `AnnualReportViewModel.load()` 里对 `report.topSongs.mapNotNull { it.audioId }` 调一次 `colorsForAudioIds(...)`，用 `Timber.d` 打出 map 大小与几个颜色值；截图或看日志确认非空且颜色不是全同一个值（全同一个值说明封面读取失败退化成 fallback）。

**提交**：`feat(report): add album cover color extraction`

---

#### T4.3 S2 音乐颜色页

**新建** `ui/screen/report/pages/PageColors.kt`：

```kotlin
@Composable
fun PageColors(context: Context, colors: List<Int>)
```

结构：`StoryPage { Caption(stringResource(R.string.story_color), 用 titleSize/accent) ; PaletteStrip(colors) ; Caption(stringResource(R.string.story_color_caption)) }`。

**改** `ReportStoryScreen`：从 ViewModel 暴露 `colors: List<Int>`（在 `ReportUiState` 里加 `val paletteColors: List<Int> = emptyList()`，由 `load()` 在拿到 report 后填充：取 `report.topSongs.mapNotNull { it.audioId }` → `colorsForAudioIds` → 按 `topSongs` 顺序映射回颜色列表）。`StoryPages.entries` 中新增 `Entry("colors", { paletteColors.isNotEmpty() })`（放在 `overview` 之后）。

**注意**：`ReportUiState` 加字段属于**新增**，允许；不要改已有字段。

**验收**：截图 `docs/screenshots/t4.3-colors.png`，确认色块数量 = 封面可用数量、颜色互不相同。

**提交**：`feat(report): add music colors page`

---

#### T4.4 令牌由专辑主色驱动

**改** `ReportStoryScreen`：把

```kotlin
val tokens: ReportTokens = ReportTokenDefaults.Dark
```

改为

```kotlin
val tokens: ReportTokens = state.paletteColors.firstOrNull()
  ?.let { ReportTokenDefaults.fromAccent(it) }
  ?: ReportTokenDefaults.Dark
```

**改** `AnnualReportViewModel.generatePoster()`：渲染海报时传入同一套 tokens（`ReportPoster.render(context, report, story, tokens)`），保证页面与海报同源。

**验收**：截图 `docs/screenshots/t4.4-themed.png`，确认整页配色跟随主色变化；再换一个主色（不同专辑封面的数据）确认变化明显且文字仍可读（对比度）。

**提交**：`feat(report): theme report with user's album colors`

---

### 阶段 P3-5：动效

#### T5.1 翻页视差

`StoryPager` 内对每页加轻微视差：`Modifier.graphicsLayer { translationX = (page - state.currentPage + state.currentPageOffsetFraction) * -40f }`，幅度固定 40f，不做非线性。

**验收**：录屏或连续截图确认翻页有位移且文案不糊。

**提交**：`feat(report): add page parallax`

#### T5.2 逐段淡入

先在 `ui/component/report/StoryPage.kt` 内定义入口索引（**必须显式定义，不要留给执行者设计**）：

```kotlin
/** 页内元素的入场顺序索引，默认 0（第一个元素）。 */
val LocalStaggerIndex = staticCompositionLocalOf { 0 }

/** 便捷包装：把 index 传下去并在渲染后 +1。 */
@Composable
fun Staggered(index: Int, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalStaggerIndex provides index) { content() }
}
```

`StoryPage` 的每个直接子元素用 `Staggered(n) { ... }` 包裹（n 从 0 起）；页内用 `LaunchedEffect(index) { delay(80L * index) ; visible = true }` 触发，`alpha` 用 `animateFloatAsState(if (visible) 1f else 0f, tween(400))`。

**验收**：截图或录屏确认元素依次出现、单页总时长 ≤1.5s。

**提交**：`feat(report): add staggered entrance animation`

#### T5.3 可跳过

`StoryPage` 与所有动画在 `LocalReduceMotion` 为 true 时立即到终态。新增 `staticCompositionLocalOf { false }`，在系统「移除动画」（`Settings.Global.ANIMATOR_DURATION_SCALE == 0`）时置为 true。

**验收**：把开发者选项的动画缩放设为「关闭」，进入报告页确认无动画且内容完整。

**提交**：`feat(report): respect system animation scale`

---

### 阶段 P3-6：数据补充页

#### T6.1 曲风季度序列查询

**改** `data/db/room/dao/PlayEventDao.kt`（**只新增**）：

```kotlin
  @Query(
    """
    SELECT ((month - 1) / 3 + 1) AS quarter, genreSnapshot AS genre,
           COUNT(*) AS plays, COALESCE(SUM(listenedMs), 0) AS listenedMs
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
      AND genreSnapshot IS NOT NULL AND genreSnapshot != ''
    GROUP BY quarter, genreSnapshot
    ORDER BY quarter ASC, plays DESC
    """
  )
  suspend fun genreByQuarter(year: Int): List<GenreQuarterCount>
```

新增投影：

```kotlin
data class GenreQuarterCount(
  val quarter: Int,
  val genre: String,
  val plays: Int,
  val listenedMs: Long
)
```

`PlayEventRepository` 新增 `suspend fun genreByQuarter(year: Int): List<GenreQuarterCount>`（实现直接转调 DAO）；`AnnualReport` 新增字段 `val genreByQuarter: List<GenreQuarterCount> = emptyList()`（**带默认值**，避免破坏其它构造点）。

**验收**：临时 `Timber.d` 打印结果，确认 quarter ∈ 1..4 且每个季度有数据。

**提交**：`feat(report): add quarterly genre breakdown query`

#### T6.2 年度之最覆盖表

**新建** entity `ReportOverride`：

```kotlin
@Entity(tableName = "report_overrides", primaryKeys = ["year", "slot"])
data class ReportOverride(
  val year: Int,
  val slot: String,          // "artist" | "album" | "song"
  val canonicalId: String,
  val audioId: Long?,
  val title: String,
  val artist: String,
  val updatedAt: Long
)
```

DAO `ReportOverrideDao`：`byYear(year)`、`upsert(row)`、`delete(year, slot)`、`clear()`。

迁移 `migration11to12`：

```kotlin
db.execSQL("CREATE TABLE IF NOT EXISTS `report_overrides` (`year` INTEGER NOT NULL, `slot` TEXT NOT NULL, `canonicalId` TEXT NOT NULL, `audioId` INTEGER, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`year`, `slot`))")
```

`AppDatabase`：`VERSION = 12`、entities 加 `ReportOverride::class`、加 `reportOverrideDao()`、加 `.addMigrations(migration11to12)`。

**验收**：构建通过 + 覆盖安装不崩溃。

**提交**：`feat(report): add yearly best override table`

#### T6.3 S3 曲风进化史页 / T6.4 S4 年度之最页

- S3：`PageGenreEvolution(genreByQuarter)` → `GenreBands`，4 个阶段标签用 `Q1..Q4`（新增字符串 `chart_quarter`：en `Q%1$d` / zh `第%1$d季度`）。
- S4：`PageYearBest(report, overrides, onSwap)` → 三张 `DualBarCompare` 风格的大卡（歌手/专辑/单曲），每张带 `story_swap` 按钮：点击后把当前 Top 列表里下一个候选项写入 `report_overrides` 并刷新（候选来自 `topArtists`/`topAlbums`/`topSongs` 的前 5）。
- `StoryPages.entries` 中把 `"highlights"` 拆分/替换为 `"year_best"`、并加入 `"genre_evolution"`。

**验收**：截图两页；点击「换一个」后确认卡片内容更换、退出再进入仍然记住（覆盖表生效）。

**提交**：`feat(report): add genre evolution and yearly best pages`

---

### 阶段 P3-7：资产管线（**条件执行**）

> **前置判定**：仅当确认要走"具象场景插画"（网易云那种缆车/秋千/博物馆质感）时才启动本阶段。
> 若程序化绘制（渐变 + 几何 + 视差）已够用，**跳过 P3-7**，直接进 P3-8。

#### T7.1 资产规格文档

**新建** `docs/report-asset-spec.md`，必须写死：

| 项 | 规定 |
| --- | --- |
| 画布 | 1080×1620（与海报一致），@1x，无内嵌文字 |
| 安全区 | 上 0-380px、下 1240-1620px 禁止放主体，留给文字槽 |
| 色彩 | **只出灰度/单色**，禁止彩色（运行时按 `tokens` 染色） |
| 格式 | WebP，质量 80，单张 ≤300 KB，总量 ≤4 MB |
| 命名 | `report_bg_<slot>_<variant>.webp`，slot ∈ {cover, overview, media, chart, rank, night} |
| 目录 | `app/src/main/res/drawable-nodpi/` |
| 数量 | 4–6 张，参数化（旋转/缩放/裁切/翻转/噪点/染色）覆盖全部页面 |

#### T7.2 scrim 算法

**新建** `ui/component/report/Scrim.kt`：

```kotlin
/**
 * 采样底图区域（8×8 网格）平均亮度 L∈[0,1]，返回 scrim alpha = lerp(0.05f, 0.45f, L)。
 * 亮度方差大于 0.15 时 alpha 再 +0.1，避免明暗跳变导致文字读不清。
 */
fun scrimAlphaFor(bitmap: ImageBitmap, rect: Rect): Float
```

规则固定，不允许执行时调参；如需调整，改这里并更新本表。

**验收**：对一张高亮底图，确认白字仍可读；对一张暗底图，确认 scrim 不显著。

**提交**：`feat(report): add automatic scrim for background assets`

#### T7.3 出图脚本与纪律

**新建** `tools/report_assets/generate.md`（操作手册，非代码）：

- 模型：ComfyUI + `Flux.1-dev fp8`（16 GB 显存可跑），分辨率 1024×1536 + hi-res fix；
- 固定 `seed` 族（同一批图用同 seed 偏移 0..5）、固定 style prompt；
- `negative`: `text, letters, numbers, watermark, logo, signature, ui, frame, border`；
- `positive` 必须包含留白要求（`large empty space at top and bottom`）；
- 产出 → 人工挑图 → 转 WebP q80 → 按 T7.1 命名落 `drawable-nodpi/`；
- **禁止运行时生成**。

**验收**：文档存在且清单齐全；执行者能从零按它出图。

**提交**：`docs(report): add asset generation guide`

#### T7.4 染色与接入

**新建** `ui/component/report/ThemedBackground.kt`：

```kotlin
@Composable
fun ThemedBackground(@DrawableRes resId: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)
```

实现：`Image(bitmap, colorFilter = ColorFilter.tint(tokens.accentSoft, BlendMode.Color))` 灰度底图按 `tokens.accentSoft` 着色，叠一层 `tokens.bgTop.copy(alpha = 0.55f)` 压暗，再叠 `Scrim`，最后放 `content`。

**验收**：截图确认不同主色下底图跟着变色、文字可读。

**提交**：`feat(report): theme background assets with tokens`

---

### 阶段 P3-8：附录页整理

#### T8.1 附录页与叙事页的一致性

**改** `ui/screen/report/AnnualReportScreen.kt`：

1. 顶部加副标题 `TextSecondary(text = stringResource(R.string.story_all_data))`，明确它是"全部数据"；
2. 保留全部 16 个 section 与 `ActionRow`（导出/导入/清空/生成歌单/分享）；
3. 卡片统一为 `StoryCard`（T1.3 已完成），间距统一 token；
4. `YearSelector` 保持在页面顶部。

**验收**：截图 `docs/screenshots/t8.1-appendix.png`，确认全部数据可读、操作按钮齐全。

**提交**：`refactor(report): finalize appendix screen`

---

## 4. 任务依赖图

```
P3-0 (T0.1→T0.2→T0.3)          验证基建，必须先做
   ↓
P3-1 (T1.1→T1.2, T1.3→T1.4)    基建
   ↓
P3-2 (T2.1→T2.2→T2.3→T2.4→T2.5→T2.6)   叙事骨架（T2.5 依赖 T0.1）
   ↓
P3-3 (T3.1→T3.2..T3.6→T3.7)    图表层（依赖 P3-2）
P3-4 (T4.1→T4.2→T4.3→T4.4)     音乐颜色（T4.1/T4.2 不依赖 P3-2，可并行）
   ↓
P3-5 (T5.1→T5.2→T5.3)          动效
P3-6 (T6.1→T6.2→T6.3/T6.4)     数据补充页
P3-7 (T7.1→T7.2→T7.3→T7.4)     资产管线（条件执行）
   ↓
P3-8 (T8.1)                    附录页整理
```

**可并行**：`P3-3` 与 `P3-4`；`T6.1` 与 `T6.2`。

---

## 5. 风险与回滚

| 风险 | 触发症状 | 对策 |
| --- | --- | --- |
| Room 迁移与实体不一致 | 启动即崩 `Migration didn't properly handle` | 严格照抄本文档的 `CREATE TABLE` SQL；改完先覆盖安装验证升级路径 |
| 新增字符串 key 与已有重复 | `mergeNormalDebugResources: Found item String/xxx more than one time` | 加之前先 `Select-String` 全文查重（`share` 已踩过） |
| 图表手写坐标溢出 | 元素被裁切/越界 | 一律由 `size.width/height` 反推尺寸，禁止写死（B5 的教训）；每页必须截图核对 |
| `HorizontalPager` 内页面预加载导致动画互相干扰 | 相邻页动画同时播 | 动画只在 `state.currentPage == page` 时启动 |
| 主色过亮/过暗导致文字不可读 | 白字压亮底 | `fromAccent` 已夹明度区间；若仍不可读，改 `fromAccent` 的 s/l 参数并更新文档 |
| 极端数据溢出 | 长歌名/大数字越界 | 每个 Task 的验收都包含"极端数据过一遍"；文字槽必须有 `maxLine` + `overflow` |

**回滚**：每个 Task 独立提交，出问题 `git revert <commit>` 即可；Room 版本号回滚需同时回滚 `VERSION` 并卸装重装（迁移不可逆）。

---

## 6. 自检记录

（本实施包按用户要求经过多轮自检，每次自检结论与修正记录如下。）

### 自检 1 — 代码事实一致性

| 检查项 | 结论 |
| --- | --- |
| `ReportTokens` 命名是否与 `object` 冲突 | **发现冲突**：data class 与 object 同名，已改 object 为 `ReportTokenDefaults`（见 T1.1） |
| `StoryCard` 的 Modifier 顺序 | 已明确 `padding(外边距) → clip → background → padding(内边距)`，否则背景会溢出圆角 |
| `StoryPage` 是否重复画背景 | **发现重复**：`StoryPager` 已画渐变，已规定 `StoryPage` 不画背景 |
| `T2.4` 代码片段中的占位写法 | **发现不可编译的伪代码** `Modifier_height_48()`，已就地给出正确写法与 import 清单 |
| `AnnualReport` 能否直接拿到 albumId | **发现不可能**：`TopAlbumItem` 无 albumId，已规定统一走 `TopPlayItem.audioId → MediaStore` |
| 是否重复造色板逻辑 | **发现已有** `ColorUtil.getColor(Palette, int)`（Java），已改为直接复用 |
| `poster_hours` 能否当"单位"用 | **发现不能**：它是 `%1$.1f 小时` 带格式化的整串，已新增 `poster_hours_suffix` |
| `HorizontalPager` 是否可用 | 已核实项目在用（`PlayingPanel.kt:74`），无需新增依赖 |
| `palette-ktx` 是否可用 | 已核实 `app/build.gradle.kts` 已 `implementation(libs.palette.ktx)` |
| `.gitignore` 的 `*.txt` 是否会吞掉调研正文 | **发现会吞**：已加 `!docs/research/raw/*.txt` 白名单 |
| 截图验证是否依赖人工点击 | **发现依赖**：已增加 T0.1 deep link 使其可脚本化 |

### 自检 2 — 可执行性

| 检查项 | 结论 |
| --- | --- |
| 每个 Task 是否有"验收命令 + 判据" | 已补齐，且判据为可观测结果（能截图、能看日志、能编译） |
| 是否仍存在"执行时需自行设计"的点 | 图表绘制规则、颜色派生公式、scrim 公式、迁移 SQL、字符串文案均已写死 |
| 任务依赖是否闭环 | 已补 §4 依赖图；`P3-7` 明确为条件执行 |
| 是否存在跨 Task 的隐式约定 | 已显式化：`StoryPages.entries` 是唯一页面集合定义处；图表空数据一律 return |
| 回滚方式是否可逆 | 已说明 Room 版本回滚需卸装重装 |

### 自检 3 — 第二轮修正（本轮实际改动）

第二轮按"逐条对着真实代码核"的方式复核，发现并修掉以下问题：

| # | 发现的问题 | 处理 |
| --- | --- | --- |
| 1 | T2.4 的 `PageCover` 代码块里留了不可编译的伪代码 `Modifier_height_48()` | 已替换为完整可编译代码 + import 清单 |
| 2 | `TextUnit * 2` 依赖不确定的重载 | 已统一改为 `* 2f` 并加注 |
| 3 | T2.5「附录降级」自相矛盾：先要求加"查看全部数据"按钮，又说不加 | 已删除该代码块，明确**附录页不加任何入口按钮**，该按钮只在 S12 |
| 4 | `StoryPages` 里放了一条永不展示的 `Entry("compare", { false })` | 已删除；规定"不要放永不展示的条目" |
| 5 | 页面索引在渲染与计数两处各自 `filter`，存在不一致风险 | 已收敛为唯一入口 `StoryPages.visible(report)`，`count` 改为 `pages.size` |
| 6 | 渲染分发引用了未定义的 `PagePlaceholder` | 已补 `PagePlaceholder.kt` 的完整代码与创建指令 |
| 7 | T5.2 提到的 `LocalStaggerIndex` 从未定义（属于"留给执行者设计"） | 已给出 `LocalStaggerIndex` 与 `Staggered` 的完整声明 |
| 8 | §2.2 交付物总表写 `tools/report_assets/*.py`，T7.3 实际产出 `generate.md` | 已统一为 `tools/report_assets/generate.md` |
| 9 | 新增字符串散落在 T2.3/T2.4/T2.6/T3.3/T6.3 五处，执行时容易漏 | 已补 §7 字符串总表，作为唯一清单 |

### 自检 3.1 — 第三轮（修正后复扫）

| # | 发现的问题 | 处理 |
| --- | --- | --- |
| 10 | T2.3 内联字符串表与 §7「唯一清单」内容重复，两处容易走偏 | T2.3 改为只落地本 Task 的 7 个 key，其余一律指向 §7 |
| — | 全文复扫失效引用（`Modifier_height_48` / `PlaceholderPage` / `StoryPages.count` / `SectionButton`） | **通过**：仅出现在自检日志里，正文已无 |
| — | 任务编号连续性 | **通过**：34 个小节覆盖 35 个 Task（T6.3/T6.4 合并同一小节），无缺号无重号 |
| — | 每个 Task 是否都有：前置 / 改动文件 / 代码或规则 / 验收 / 提交信息 | **通过** |
| — | 是否还有"留给执行者设计"的开口 | **通过**：字符串、颜色公式、scrim 公式、迁移 SQL、图表几何规则、页面充分性条件均已写死 |

### 自检 5 — 执行中发现并回写的计划缺陷

计划不是一次写对的。以下缺陷**编译/运行时才暴露**，已回写进对应章节，避免后续执行者重踩：

| # | 位置 | 缺陷 | 修正 |
| --- | --- | --- | --- |
| 1 | T0.1 | `NavDeepLink(uri)` 构造函数在该 navigation 版本为 **internal**；且 `LocalNavController` 在 composition 内创建，Activity 的 `handleIntent()` **拿不到** NavController，热启动还不触发 `onResume` | 改为 `PendingRoute` 状态通道 + `onNewIntent` 显式分发（提交 `8158f594`） |
| 2 | T2.4 | `story.keywords.joinToString { stringResource(...) }` —— `joinToString` 的 transform **不是 inline lambda**，编译报 `@Composable invocations can only happen from the context of a @Composable function` | 先 `forEach`（inline）解析 `stringResource` 成字符串列表，再 `joinToString` |
| 3 | 执行环境 | 本机 shell 是 Windows PowerShell 5.1，**没有 `pwsh` 命令**；截图脚本要用 `& <path>\shots.ps1` 调用 | 已记入台账"注意事项" |

### 自检 4 — 待执行中回填

- T3.x 各图表的截图结论
- T4.2 取色的实测命中率（缓存命中 / 回退 fallback 比例）
- 极端数据清单的执行结果
- Room 两次迁移（10→11、11→12）在真机覆盖安装下的升级结果

---

## 7. 附录：新增字符串总表（唯一清单）

**加之前先查重**：`Select-String -Path app/src/main/res/values*/strings.xml -Pattern 'name="<key>"'`。
以下 key 均确认**当前不存在**；`share`、`close`、`annual_report`、`stat_*`、`kw_*`、`poster_hours`、`poster_minutes`、`poster_metric_*`、`poster_top_*`、`poster_subtitle`、`poster_footer`、`story_template` 一律**复用已有**，不要重复定义。

| key | en (`values/`) | zh-rCN (`values-zh-rCN/`) | 引入 Task |
| --- | --- | --- | --- |
| `story_start_hint` | Swipe to begin | 滑动开始回顾 | T2.3 |
| `story_all_data` | View all data | 查看全部数据 | T2.3 |
| `story_year_best` | Best of the year | 年度之最 | T2.3 |
| `story_swap` | Swap | 换一个 | T2.3 |
| `story_genre_evolution` | Genre evolution | 曲风进化史 | T2.3 |
| `story_color` | Music colors | 音乐颜色 | T2.3 |
| `story_color_caption` | Your year, in the colors of the albums you played most | 你这一年，藏在你听得最多的专辑封面颜色里 | T2.3 |
| `poster_hours_suffix` | h | 小时 | T2.4 |
| `story_overview_caption` | %1$d plays · %2$d songs · %3$d days | %1$d 次播放 · %2$d 首歌 · %3$d 天 | T2.4 |
| `story_poster_loading` | Rendering… | 生成中… | T2.6 |
| `chart_legend_less` | Less | 少 | T3.3 |
| `chart_legend_more` | More | 多 | T3.3 |
| `chart_quarter` | Q%1$d | 第%1$d季度 | T6.3 |


