# 展示层实施进度台账

基线：`b6790aa9`（分支 `dev/play-event`）
实施包：`docs/annual-report-presentation-implementation.md`
设计文档：`docs/annual-report-presentation-design.md`

图例：✅ 完成且验证 ｜ ⚠️ 代码完成、**截图待补** ｜ ⬜ 未开始

| Task | 状态 | 提交 | 构建 | 截图/验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 | ✅ | 8158f594 | 通过 | `docs/screenshots/t0.1-report-open.png`（冷启动+热启动两路径）、`t0.1-before-warm.png` | 计划原方案不可行（见"执行修正"1），改 `PendingRoute` 通道 + `onNewIntent` |
| T0.2 | ✅ | a0ccbd7e | — | `tools/shots.ps1` 实产出图 | `screencap` 落盘再 `pull`；本机无 `pwsh`，用 `& <path>` 调用 |
| T0.3 | ✅ | f4a13ac3 | — | 本文件 | 台账建立 |
| T1.1 | ✅ | 448d92be | 通过 | — | `ReportTokens` / `ReportTokenDefaults` / `LocalReportTokens` |
| T1.2 | ⚠️ | 4ec9b944 | 通过 | **待截图** | 海报改为消费 tokens；`android.graphics.Color` 冲突用别名 `ComposeColor` |
| T1.3 | ⚠️ | 76d9fad8 | 通过 | **待截图** | `StoryCard` 真卡片；`AnnualReportScreen` 拆为外壳（提供 tokens）+ 内容，避免整块重新缩进 |
| T1.4 | ⬜ 待做 | — | — | — | 字号收敛到 token；与截图批次一起做便于对比 |
| T2.1 | ⚠️ | c54d7ff2 | 通过 | **待截图** | `StoryPage` / `StoryPager`（背景只在 Pager 画） |
| T2.2 | ⚠️ | 1b3f5d18 | 通过 | **待截图** | `HeroNumber`（Animatable 800ms）/ `Caption` |
| T2.3 | ✅ | 350d534b | 通过 | 构建即验证 | 7 个字符串 key（en + zh-rCN） |
| T2.4 | ⚠️ | abc41df7 | 通过 | **待截图** | S0/S1 两页 + 2 个字符串。修掉计划里的编译错误（见"执行修正"2） |
| T2.5 | ⚠️ | fdaa1d3e | 通过 | **待截图** | `StoryPages` + `renderStoryPage` + `PagePlaceholder` + `ReportStoryScreen` + 路由/deep link/manifest/设置入口 |
| T2.6 | ⚠️ | 331ea648 | 通过 | **待截图** | S11 关键词页（复用 `story_template` 拼句）、S12 分享页 + `story_poster_loading`；P3-2 收尾 |
| T4.1 | ✅ | f25022b4 | 通过 | **静态验证通过**：Room 导出的 `11.json` 中 `album_colors` 的 `createSql` 与迁移 SQL **逐字节一致**，且只新增该表、其它表无漂移 | 不需设备即可确认迁移正确性；运行时升级路径仍待真机验证 |
| T4.2 | ✅ | d81a0481 | 通过 | 编译产物确认：`AlbumColorRepository.class`、`AlbumColorDao_Impl.class` 均生成 | 复用 `ColorUtil.getColor(Palette, int)`，未自造选色逻辑 |
| T7.1 | ✅ | 320ee0dc | — | 文档交付 | `docs/report-asset-spec.md`：画布/安全区/灰度/命名/体积/接入/验收清单 |
| T7.3 | ✅ | 02e521f1 | — | 文档交付 | `tools/report_assets/generate.md`：**从零搭建**（本机无 ComfyUI/无 torch）+ 锁定出图参数 + 后处理 + 禁止事项 |
| T7.2 | ✅ | 本次提交 | 通过 | **数值验证通过**：修正后的 scrim 反解公式在 19 档底图亮度下白字对比度恒为 **4.50:1（PASS）**；原插值公式 15/19 档不达标（已废弃并回写规格） | `Scrim.kt`：`alpha = 1 - 0.1833/mean`，`mean ≤ 0.1833` 时取 0.05；方差 > 0.15 再 +0.05。**端到端效果仍待有底图后实机确认** |
| T1.4 | ⚠️ | 本次提交 | 通过 | **待截图** | 附录页字号全部收敛到 token；剩余字面值正好只有文档标注的例外（图表内部 8/9/10.sp、指标值 20.sp） |
| T8.1 | ⚠️ | 本次提交 | 通过 | **待截图** | 附录页顶部加"查看全部数据"标识，与叙事流区分；`YearSelector`、16 个 section、`ActionRow` 全部保留 |
| T7.4 | ⚠️ | abfb018e | 通过 | **待截图**；APK 内已确认打包 6 张底图 | `ThemedBackground`：灰度底图按 `accentSoft` 染色 + 压暗 0.55 + 上下文字区 scrim（走已验证的反解公式）；scrim 采样用 4 倍降采样解码，避免 15MB 整图常驻内存；`StoryPages` 按 6 个槽位映射 13 页 |
| 资产 | ✅ | 8fd5e0a5 | 通过 | **数值验证**：6 张底图全部满足资产规格（文字安全区均值 ≤0.20、主体中位明度 0.31–0.46、峰值 ≤0.92、最低 ≥0.055、单张 14.2–22.8KB）；合计 **100.1KB** 已确认打进 APK | `tools/report_assets/generate_procedural.py`（设计文档"方案 A"）。**修掉生成器自身的两个缺陷**：①约束顺序反了（先压安全区又整体缩放，把安全区顶回 0.20 以上）；②包络斜坡每像素≈1/255 导致明显色带，改为在包络**之后**加约 1 LSB 抖动。扩散模型出图与这些文件**同名**，可直接覆盖、代码零改动 |
| T6.1 | ✅ | 42c0c9e3 | 通过 | **双重验证**：①Room 编译期校验 `@Query` 通过；②用 Room 导出的 schema 在本机 sqlite3 真跑该 SQL，结果与期望**逐项一致**（Q1 Pop2/Rock1、Q2 Pop3、Q3 Jazz1、Q4 Pop1），且 `song_added`、NULL 流派、空流派、跨年记录全部被正确排除（计入总数 8） | `genreByQuarter` + `GenreQuarterCount` + 仓库方法 + `AnnualReport.genreByQuarter`（带默认值） |
| T6.2 | ✅ | 82da1434 | 通过 | **静态验证通过**：`12.json` 的 `createSql` 与迁移 SQL 逐字节一致，复合主键 `year,slot` 正确，仅新增 `report_overrides` | 实体 + DAO + `migration11to12` + `VERSION = 12` |
| T3.1 | ⚠️ | 88c17a4e | 通过 | **待截图** | `ChartFrame`：标题 + 固定高度画布 + 可选图例；横向内边距交给 StoryPage，避免双重留白 |
| T3.2 | ⚠️ | ff8238ba | 通过 | **待截图**；几何已自检（0 点起始角 `-90°`、每小时 15°、缝隙 13.5°） | `PolarClockChart`：半径随播放量生长，圆心标峰值小时，0/6/12/18 刻度 |
| T3.3 | ⚠️ | b60121b4 | 通过 | **待截图**；尺寸已数值自检：1080px 屏（density 2.75）下可用宽 970px、月标签 77px、gap 5.5px → `cell ≈ 23.5px`、`gridHeight ≈ 342px`（画布高 522px，**放得下**） | `CalendarHeatmap`：cell 由可用宽度反推（**不写死**，海报踩过此坑）、5 档色阶、逐月生长、月份标签 + legend；新增 `chart_legend_less/more` |
| T4.3 | ⚠️ | 本次提交 | 通过 | **配色可读性已数值验证**：复现 `fromAccent` 的 HSL 推导，对 24 个色相算 WCAG 对比度——白字/bgTop 最低 16.99、白字/bgBottom 最低 11.56、次要文字/bgTop 最低 9.12、**accent/bgTop 最低 5.13（hue240 蓝）**，全部 ≥ AA 4.5。**待截图** | `AlbumColorRepository.colorsInOrder`（按听歌顺序）、`ReportUiState.paletteColors`、`PaletteStrip`、`PageColors`、`StoryPages` 新增 `colors` 页 |
| T4.4 | ⚠️ | 本次提交 | 通过 | 同上（配色已数值验证）；**待截图** | 新增统一入口 `reportTokensFor(paletteColors)`，页面与海报共用；`generatePoster` 改为传同一套令牌 |
| — | ✅ | 332395d4 | 通过 | — | 补充 `DatabaseModule` 的 `AlbumColorDao` / `ReportOverrideDao` 提供（Hilt 缺 provider 会编译失败，属 T4.3/T6.2 的必要收尾） |
| T3.4 | ⚠️ | 本次提交 | 通过 | **待截图** | `TrendLineChart`：4 条网格线、Path + 渐变填充（alpha 0.18）、PathMeasure 生长、峰值点标注、X 轴 1/4/7/10 |
| T3.5 | ⚠️ | 本次提交 | 通过 | **待截图** | `DonutChart`：弧段留 1° 缝隙、圆心写最大项占比、右侧图例；弧形按 progress 生长 |
| T3.7 | ⚠️ | 51cff35b | 通过 | **待截图** | 四个图表页接线（S5 时段 / S7 年历 / S9 探索重复 / S10 来源）；把 `sourceLabel` 抽成 `SourceLabels.kt` 供附录页与叙事页共用，避免两处文案走偏 |
| T5.1 | ⚠️ | 本次提交 | 通过 | **待截图** | 翻页视差：`translationX = -offset * 40f`（`offset = (page - currentPage) + currentPageOffsetFraction`） |
| T5.2 | ⚠️ | 本次提交 | 通过 | **待截图** | `Motion.kt` 提供 `FadeInStaggered(index)`（80ms 间隔 + 400ms 淡入），已在 S0/S1/S2/S11 四个多元素页面应用 |
| T5.3 | ⚠️ | 本次提交 | 通过 | 逻辑可静态确认；**待截图** | 新增 `LocalReduceMotion`，`ReportStoryScreen` 读 `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`；所有图表动画改走 `reportTween()`、`HeroNumber` 与视差也在关动画时直接到终态 |
| T6.3 | ⚠️ | 054e0792 | 通过 | **待截图** | `PageGenreEvolution`：按季度聚合成 4 个 `GenreStage`，交给 `GenreBands`；新增 `chart_quarter` 文案 |
| T6.4 | ⚠️ | 本次提交 | 通过 | **待截图** | `PageYearBest`（歌手/专辑/单曲三卡 + 换一个）；ViewModel 增加 `bestOverrides`、`swapBest(slot)`（在前 5 候选里循环）并落 `report_overrides`；`StoryPages` 顺序重排为 S0–S12 |
| S6/S8 | ⚠️ | 本次提交 | 通过 | **待截图** | 补齐实施包 S 列表里未单列的两页：`PageNight`（0–6 点占比 + 深夜最常听 Top3）、`PageLoopKing`（循环次数大数字 + 曲名）。**至此 S0–S12 十三页全部有实现，流程内不再出现占位页** |

## 阻塞

- **USB 设备断开**（`adb devices` 为空；Windows 只枚举到通用 USB 复合设备，无 ADB 接口）。
  受影响：**全部 26 个 ⚠️ 任务的截图核对**——代码已 100% 完成，但"编译通过"不等于"看着对"。
  恢复方式：重插数据线并确认已授权 USB 调试；`adb devices` 能看到设备后，按下面的批次计划一次核完。

**代码侧已无待办**：36 个任务全部落地（T0.1–T8.1 + S6/S8 补页），`P3-7` 的资产管线也已产出可用底图。剩下的只有"看图验证"这一件事。

## 截图批次计划（设备恢复后按此顺序执行）

1. `adb install -r app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk`
2. T1.3/T1.4 附录页：`& tools\shots.ps1 -Name t1.3-cards -Uri "aplayer://annual_report"`
3. T2.4/T2.5 叙事流：`-Uri "aplayer://annual_report_story"`，逐页滑动截图（S0/S1/S11/S12 至少各一张）
4. T1.2 海报：附录页点「分享卡片」→ 截图对比 `docs/samples/poster-preview.png`
5. 极端数据复核：超长歌名、5 位数播放次数、`1234.5 小时`、超长歌手名
6. 每张图人工看过再回填本台账

## 设备与环境

- 设备：vivo V2425A（Android 16，SDK 36），序列号 `10AEBU1SK3000LB`
- adb：`D:/Application2/Android/Sdk/platform-tools/adb.exe`
- 构建：`$env:JAVA_HOME="D:\Application2\jdk-17.0.3.1"; $env:ANDROID_HOME="D:\Application2\Android\Sdk"; .\gradlew.bat :app:assembleNormalDebug`
- 产物：`app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk`
- 出图 GPU：NVIDIA RTX 4060 Ti 16GB，驱动 610.88；**ComfyUI 未安装**，python 3.11.5（anaconda），无 torch

## 执行修正（计划文档已同步回写）

1. **T0.1 deep link 方案不可行**：`NavDeepLink(uri)` 构造函数在该 navigation 版本是 `internal`；`LocalNavController` 在 composition 内创建，Activity 拿不到；且热启动不触发 `onResume`。改为 `PendingRoute` 状态通道 + `onNewIntent`。
2. **T2.4 示例代码编译不过**：`joinToString { stringResource(...) }`——`joinToString` 的 transform 不是 inline lambda，不能调 `@Composable`。改为先 `forEach`（inline）解析字符串。

## 注意事项（执行中积累）

1. **设备锁屏会导致 `adb install` 卡住**（vivo 需亮屏解锁）：先 `adb shell input keyevent KEYCODE_WAKEUP`，必要时 `adb shell input swipe 540 2000 540 700 200`。
2. **本机没有 `pwsh` 命令**（shell 是 Windows PowerShell 5.1）：脚本用 `& <path>\xxx.ps1` 调用。
3. **不要用 `Select-Object -First N` 接 gradle 的输出**：会提前掐断管道把 gradle 杀掉，表现为莫名其妙的 exit 1。改为先 `$out = ... 2>&1` 再过滤。
4. **报告页数据已就绪**：2026 年真实数据（1531 次播放 / 90.6 小时 / 465 首歌 / 210 歌手 / 257 专辑 / Blues 347 次），截图有真实内容可看。
5. `/app/schemas` 虽被 gitignore，但**是验证 Room 迁移的最好工具**：`app/schemas/remix.myplayer.data.db.room.AppDatabase/<version>.json` 里的 `createSql` 就是 Room 期望的建表语句。

## ����ʱ����������ƣ������豸��

�� `ui/**/report/**` ȫ��ɨ�� `!!`��`.first()`��`.last()`��`.reduce()`��`.maxOf()` �����г�����

- **�޷ǿն���**��`!!` 0 ������
- `.first()/.last()` �� 5 ����ȫ���� `isEmpty()` �緵��֮��`PageYearBest` 2 �������� `(1..12)` �������챣֤��`TrendLineChart` 3 ������
- ���� 20 �ദ����ĸȫ���� `coerceAtLeast(1)` / `if (x > 0)` / �緵�ر�����`maxMs`��`total`��`grand`��`totals[index]`��`max`��`plays`����

���ۣ�**δ���ֿ�����������ı�����**��
