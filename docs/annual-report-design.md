# 年度听歌报告 · 报告层设计文档

> 本文档是《播放事件与年度报告 · 全局唯一设计方案》的**报告层（第 4 层）子设计**。
> 全局分层与路线图见 `docs/play-event-design.md` 第 0 章；数据模型与协议见其第 2/3/10 章。

## 1. 目标与范围

- **目标**：把数据层已经记录/聚合好的指标，呈现为一个“好看、易懂、可分享”的年度听歌报告。
- **范围**：展示、交互、文案、分享、年度歌单生成。**不改变采集与存储语义。**
- **唯一写操作**：把报告里的歌单一键生成为本地播放列表（其余全部只读）。
- **非目标（属于其它层）**：事件采集、存储/迁移、跨端导入与多设备合并（M3）。

## 2. 与数据层的关系

- **读取**：`PlayEventRepository.annualReport(year)` 返回 `AnnualReport`；年份列表用 `availableYears()`。
- **已有可用字段**（`AnnualReport`）：
  `year, plays, listenScore, completedPlays, listenMs, listenedDays, distinctSongs, distinctArtists, distinctAlbums, firstListenedSongs, addedSongs, topSongs, topArtists, topAlbums, monthDistribution, hourDistribution, sourceBreakdown`。
- **报告层不直接查原始事件表**（除导出/分享需要时）；一律走聚合。
- **需要新增的数据层接口**见第 5 章。

## 3. 总体结构

```
ReportScreen（页面）
 ├─ 年份选择（availableYears）
 ├─ 概览卡片区（核心数字）
 ├─ 年度“一句话”/关键词（模板文案）
 ├─ 排行区（TOP 歌曲 / 歌手 / 专辑）
 ├─ 时间区（月份趋势 / 时段分布 / 星期分布 / 日历热力图）
 ├─ 趣味区（深夜听歌 / 单曲循环王 / 跳过率 / 重复 vs 探索 / 年度第一首·最后一首）
 ├─ 内容区（新增歌曲 / 流派占比 / 来源占比）
 └─ 操作区（生成年度歌单 / 导出 JSONL / 分享卡片 / 清除统计）
```

数据流：`ReportViewModel` →（聚合查询）→ `AnnualReport` → 各 Section 只读渲染。分享卡片为一次性渲染到位图后分享。

## 4. 功能清单

> 口径若无特别说明，均基于“有效播放事件”（`eventType='playback'` 且已通过 10% 阈值）。

| # | 功能 | 口径 / 算法 | 数据来源 | 需新增数据层能力 | 呈现 | 优先级 |
|---|---|---|---|---|---|---|
| R1 | 年份选择 | `availableYears` | 已有 | 无 | 顶部切换 | **P0** |
| R2 | 核心数字卡片 | 次数 / 积分 / 完整 / 时长 / 天数 / 去重歌曲·歌手·专辑 | 已有 | 无 | 卡片组 | **P0** |
| R3 | 时段分布 | 按 `hour` 分组次数 | `hourDistribution`（**已算未展示**） | 无 | 柱状/极坐标 | **P0** |
| R4 | 新增歌曲 | `song_added` 且 `year=该年` 去重 `canonicalId` | `addedSongs`（**已算未展示**） | 新增“新增歌曲收听表现”查询 | 数字 + 列表 | **P0** |
| R5 | TOP 歌曲/歌手/专辑 | 按 Σ`listenedMs` | 已有 | 无 | 榜单 | **P0** |
| R6 | 来源占比 | 按 `source` 分组，**中文文案** | `sourceBreakdown` | 无（文案映射） | 列表/环形图 | **P0** |
| R7 | 跳过率 / 完整率 | 跳过=`endReason ∈ {SKIP_TO_NEXT,SKIP_TO_PREVIOUS}`；完整=`completed` | 需新增 `endReason` 计数 | `skipStats(year)` | 数字/进度 | **P0** |
| R8 | 重复 vs 探索 | 重复度=有效事件/去重歌曲；探索度=首次收听/去重歌曲 | 已有字段可算 | 无 | 双指标 | **P0** |
| R9 | 年度第一首 / 最后一首 | `MIN/MAX(startedAt)` 对应歌曲 | 需新增 | `firstAndLastEvent(year)` | 卡片 | **P0** |
| R10 | 年度歌单一键生成 | 取 TOP N 或循环最多，写入本地歌单 | `topSongs` + `PlayListRepository` | 无（写歌单） | 按钮 | **P0** |
| R11 | 日历热力图 | 按天聚合次数/时长 | 需新增 | `dailyDistribution(year)` | 热力图 | P1 |
| R12 | 连续听歌天数 / 最长连续 | 由“按天集合”推导连续段 | 同上 | `dailyDistribution(year)` | 数字 | P1 |
| R13 | 深夜听歌 | `hour ∈ [0,6)` 的事件数/时长占比 + 深夜最常听 | `hourDistribution` + 新查询 | `lateNightTopSongs(year)` | 数字 + 列表 | P1 |
| R14 | 单曲循环王 | 按 `canonicalId` 聚合 `loopCount>1` 的轮数 | 需新增 | `loopRanking(year, limit)` | 榜单 | P1 |
| R15 | 星期分布 | 按 `weekday` 分组 | 字段已有 | `weekdayDistribution(year)` | 柱状 | P1 |
| R16 | 月份趋势曲线 | 按 `month` 的次数/时长 | `monthDistribution`（含时长） | 无 | 折线 | P1 |
| R17 | 流派占比 | 按 `genreSnapshot` 分组（过滤空值） | 需新增 | `genreBreakdown(year)` | 列表/环形 | P1 |
| R18 | 多年对比 | 今年 vs 去年各项指标 | 多次 `annualReport` | 无 | 对比条 | P1 |
| R19 | 分享卡片 / 长图 | 渲染报告为位图并分享 | 全部指标 | 无 | 图片分享 | P2 |
| R20 | 文案故事 | 模板化“年度一句话/关键词” | 全部指标 | 无 | 文案 | P2 |
| R21 | 多设备合并报告 | 合并 `android-`/`pc-` 事件 | 导出/导入 | 属 M3 | 开关 | P2 |

## 5. 数据层待补接口（报告层依赖）

在 `PlayEventDao` / `PlayEventRepository` 增加（均只统计 `eventType='playback'`）：

1. `dailyDistribution(year): List<DayCount(month, day, plays, listenedMs)>` —— 热力图/连续天数。
2. `weekdayDistribution(year): List<WeekdayCount(weekday, plays, listenedMs)>`。
3. `loopRanking(year, limit): List<LoopItem(canonicalId, title, artist, album, loops, listenedMs)>` —— `loopCount>1` 或 `SUM(loopCount)`。
4. `lateNightTopSongs(year, limit)` —— `hour BETWEEN 0 AND 5`。
5. `skipStats(year): SkipStats(total, skipped, completed)`。
6. `firstAndLastEvent(year): Pair<PlayEvent?, PlayEvent?>`。
7. `genreBreakdown(year)` —— 按 `genreSnapshot`（非空）。
8. `addedSongsPerformance(year)` —— `song_added` 的 `canonicalId` 在 `playback` 中的次数/积分。

> 若接口较多，建议在 `AnnualReport` 上按 Section 分组扩展（如 `timeSection` / `funSection`），保持“一次聚合、一次返回”。

## 6. 呈现规范

- **单位**：时长统一“小时/分钟”（`x.x 小时`、`xx 分钟`），比率用百分比；次数用整数。
- **本地化**：`source`、`endReason`、`mediaType` 一律映射为中文文案（如 `SEARCH_CLICK → 搜索`、`QUEUE_AUTO → 自动播放`）。
- **空态**：无数据时显示“暂无收听数据”，不显示空图表。
- **排序**：榜单默认按收听时长降序，可在 P1 增加切换（次数/积分）。
- **配色**：跟随主题（`LocalTheme`），图表使用主题主色/辅色，暗色与 AMOLED 下需可读。
- **无障碍**：图表需提供文本摘要。

## 7. 交互规范

- 年份切换：顶部下拉/横向切换；切换即刷新（聚合查询）。
- 条目点击：歌曲→ 播放/详情；歌手/专辑→ 对应详情页。
- 长按/分享按钮：生成分享卡片。
- 下拉/滚动动画：Section 逐个淡入（P2）。
- “生成年度歌单”：弹出确认 → 写入歌单 → 提示成功并可跳转。

## 8. 分享与导出

- **分享卡片（P2）**：Compose 渲染报告摘要到 `Bitmap` → 存到 `cacheDir`/`externalCacheDir` → `FileProvider` + `ACTION_SEND`（复用现有 `Util.createShareImageFileIntent`）。
- **JSONL 导出（已有）**：`play-events-<year>.jsonl`，保留现状。
- 分享内容建议：年份、核心数字、TOP3、年度关键词、来源占比。

## 9. 性能与约束

- 报告页**只查聚合**，不加载全量事件；导出时才读全量。
- 聚合查询在 `Dispatchers.IO`；切换年份时若数据量大可加缓存（按 `year` 缓存 `AnnualReport`）。
- 分享卡片渲染为大图时注意内存（必要时降采样）。

## 10. 分阶段计划

| 阶段 | 内容 | 对应里程碑 | 状态 |
|---|---|---|---|
| **P0** | R1–R10：展示“已算未展示”（时段、新增歌曲）、来源中文化、跳过率/完整率、重复 vs 探索、年度首末曲、年度歌单 | M2 起步 | ✅ **已完成** |
| **P1** | R11–R18：日历热力图、连续天数、深夜、循环王、星期、趋势、流派、多年对比（需补第 5 章接口） | M2 | ✅ **已完成** |
| **P2** | R19–R21：分享卡片、文案故事、多设备合并（依赖 M3 导入） | M2/M3 | ⬜ 未开始（**详细方案见第 13 章**） |

## 11. 验收与测试

- 各指标与数据库直接查询结果一致（抽样校验）。
- 切换年份、无数据、单年/多年数据均正常。
- 中文化完整（无英文枚举泄漏）。
- 生成歌单成功且内容与 TOP 一致。
- 分享卡片能正确生成并分享（含暗色主题）。
- 报告页不因数据量大而卡顿（只查聚合）。

## 12. 相关文档

- 全局唯一设计方案：`docs/play-event-design.md`（第 0 章分层与路线图、第 10 章 v2 字段）
- 跨平台协议规范：`docs/play-event-protocol-spec.md`
- 导出样例：`docs/samples/play-events-2026.sample.jsonl`

---

## 13. P2 详细方案（分享卡片 / 文案）

> 本节是 **P2 的实现方案**：R20 年度关键词/文案 + R19 分享卡片（海报图）。
> 约束：**不引入新依赖、不联网**，全部本地规则 + 本地渲染；数据全部来自现有 `AnnualReport`，**无需新增数据库查询或迁移**。

### 13.1 产物

一张 **年度报告海报图（PNG）**，可分享 / 保存；由两部分组成：

- **R20 年度关键词 + 一句文案**（规则生成）
- **R19 海报卡片**（把报告核心内容渲染成图片）

### 13.2 R20：关键词与文案（规则引擎）

新增 `ReportStory`：输入 `AnnualReport`，输出 1–2 个关键词 + 一句话。**确定性**（同一份数据结果一致）、可复现、可扩展。

| 判定条件 | 关键词 |
|---|---|
| 深夜(0–5 点)占比 ≥ 25% | 🌙 夜猫子 |
| 重复度 ≥ 3.0 | 🔁 单曲循环狂 |
| 探索度（首次收听占比）≥ 60% | 🧭 探索者 |
| 跳过率 ≥ 40% | 🎯 挑剔的耳朵 |
| 完整率 ≥ 70% | 🎧 沉浸式聆听 |
| 自动播放占比 ≥ 60% | 🍃 佛系听众 |
| 当新增歌曲 ≥ 100 | 🆕 追新族 |
| 收听最长月份 | 📅 「X 月是你的主场」 |

一句话模板（示例）：

> 今年你听了 **X 小时、Y 首歌**，最常在 **N 点**按下播放，其中 **Z 首**是第一次遇见。

- 所有文案走字符串资源（中/英），便于本地化。
- 关键词选择：按各规则权重打分，取最高的 1–2 个；无匹配时给兜底关键词（如「🎵 音乐旅人」）。

### 13.3 R19：海报渲染方案

**选型：用 `android.graphics.Canvas` 离屏绘制**（不用 Compose 截图）。

- 理由：分享图是**非交互**的固定尺寸产物；Canvas 离屏绘制**不依赖 Compose 生命周期**（Compose 截图需把 `ComposeView` 挂进窗口再 draw，容易出现 owner 缺失/首帧未布局），输出**确定可控**。
- 尺寸：**1080 × 1620**（竖版），按屏幕密度导出。
- 视觉：**固定风格**（不跟随 App 明暗主题），保证任何主题下导出效果一致。

内容分层（自上而下）：

1. 背景：主题色渐变 + 年度数字（大号）
2. **关键词 + 一句话文案**
3. 核心数字：播放次数 / 收听时长 / 听歌天数 / 完整率
4. **TOP 3 歌曲**（名次 + 歌名 + 歌手）
5. **TOP 3 歌手 / 专辑**
6. **听歌日历缩略**（12×31 方格热力条，数据来自 `dailyDistribution`）
7. 页脚：年份 / 设备（`deviceId` 前缀）/「APlayer」

实现要点：`Paint` 分段绘制，颜色/字号集中为常量；文本超长做省略号处理；圆角卡片用 `drawRoundRect`。

### 13.4 分享链路

1. 生成 `Bitmap` → 写入 `externalCacheDir/share/annual-report-<year>.png`
2. 复用现有 `FileProvider`（`${applicationId}.fileprovider`）+ 现有 `Util.createShareImageFileIntent(file, context)`
3. **配置改动**：`res/xml/provider_paths.xml` 增加 `<cache-path name="internal_cache" path="."/>`（当前只有 `external-path`，否则内部缓存可能拿不到 URI）

### 13.5 UI 交互

- 报告页底部操作区新增 **「分享卡片」**（在 生成歌单 / 导出 / 导入 之后）。
- 点击 → **预览对话框**：显示生成的图片 + 「分享」/「关闭」；
- 生成在 `Dispatchers.IO`，期间显示 loading；失败用 `MessageNotifier` 提示；
- 无数据时不生成，提示“暂无收听数据”。

### 13.6 改动点

| 文件 | 改动 |
|---|---|
| `report/ReportStory.kt`（新） | 关键词 / 文案规则引擎 |
| `ui/screen/report/ReportPoster.kt`（新） | Canvas 海报渲染器 |
| `AnnualReportViewModel.kt` | `sharePoster()`：生成图片 → 存盘 → 返回分享 Intent |
| `AnnualReportScreen.kt` | 「分享卡片」按钮 + 预览对话框 |
| `res/values*/strings.xml` | 关键词、文案模板、按钮文案 |
| `res/xml/provider_paths.xml` | 增加 `cache-path` |

### 13.7 分期

| 阶段 | 内容 |
|---|---|
| **P2-1** | `ReportStory` 关键词 + 文案（最快见效） |
| **P2-2** | 海报渲染 + 分享 + 预览（主体） |
| **P2-3**（可选） | 长图（各板块竖排拼接）、水印 / 自定义主题 |

### 13.8 验收

- 无数据时不生成，给出提示。
- 图片尺寸固定、中文显示正常；暗色/AMOLED 主题下同样清晰（固定风格）。
- 分享可被微信 / 文件管理器正常接收。
- 关键词与文案与数据一致且可复现（同数据同结果）。
