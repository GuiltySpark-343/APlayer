# 歌曲标签系统 · 设计方案（v0.2 · 已审阅通过）

> 状态：**已审阅通过**（2026-09-27）。下一步：转「实施包」并按第 10 章 T0 → T3 推进。决策记录见第 13 章。
> 负责人：本线（标签系统）。展示层（S1–S12 页面与资产）由另外两条线负责，见第 9 章接口冻结。

---

## 0. 文档定位与边界

| 项 | 说明 |
|---|---|
| 本文是什么 | 歌曲**语义标签系统**的完整设计方案：调研、模型、数据、交互、协议、分期、验收 |
| 本文不是什么 | 不是展示层（年度报告页面/海报）的设计；不是播放事件采集层的设计 |
| 与既有文档关系 | 复用 `docs/play-event-design.md` 的 `canonicalId` 身份体系；复用 `docs/play-event-protocol-spec.md` 的 JSONL 互操作思路 |
| 协作边界 | 本线**只新增文件**（新表、新仓库、新页面、新文档），不改动展示层文件；对展示层只提供**只读接口**（第 7 章） |

---

## 1. 背景与目标

### 1.1 用户诉求（原始意图转述）

> 为每首歌加上标签。除了网络上能搜到的客观标签，用户自己也能在**听的时候**加上主观标签 —— 有的歌听了开心、适合回家放；有的亢奋、适合户外运动；有的情感浓烈或致郁低沉、引人思考，适合静下心来听。
>
> 最终目的是让**智能家居**根据情境放歌，让**个人助理**分析听歌习惯，同时也能为**年度总结**所用。

### 1.2 目标（按优先级）

| # | 目标 | 说明 |
|---|---|---|
| G1 | 用户能**低摩擦**地给歌曲打主观标签 | 尤其是"听的时候"这个时机 |
| G2 | 能按标签**筛选与播放** | 曲库筛选、按标签随机/顺序播放 |
| G3 | 标签可作为**情境查询条件**对外输出 | 智能家居："回家模式" → 查歌 → 播放 |
| G4 | 标签数据可**导出/合并** | 跨端（手机 ↔ PC）、可被助理消费 |
| G5 | 标签能为**年度报告**提供新维度 | 情绪分布、场景 TOP、情绪时间线 |

### 1.3 非目标（YAGNI，本期明确不做）

- ❌ 云端社交标签、公开标签社区
- ❌ 训练音乐情绪识别模型（不训练，最多用现成规则/特征）
- ❌ 自动把标签写回音频文件（见 5.3，默认不写）
- ❌ 全自动打标（情绪层永远由用户主导，见 11.1 风险）
- ❌ 标签的多用户/权限体系（单用户本地数据）

---

## 2. 调研：别人怎么做

### 2.1 六类标签体系

| # | 类型 | 代表 | 谁打标 | 特点 |
|---|---|---|---|---|
| 1 | **官方元数据标签** | ID3 / Vorbis / iTunes | 用户或工具 | `title/artist/album/genre/year`。**ID3v2.4 存在标准情绪帧 `TMOO`(Mood)**，iTunes/Apple Music 也有 Mood 字段 —— 但实际几乎无人填写，生态支持极差 |
| 2 | **社区标签（folksonomy）** | **Last.fm**、MusicBrainz | 用户自由打 | 标签云；"标签共现"用于计算相似艺人。beets 的 `lastgenre` 插件即拉取 Last.fm 标签。**自由标签鼻祖，但碎片化严重** |
| 3 | **平台运营 / 歌单标签** | **网易云、QQ 音乐** | 运营 + 用户 | 重点不在"给单曲打标"，而是**给歌单打标签**（心情 / 场景 / 风格 / 语种 / 年代 / 主题）；用户按标签消费内容，推荐侧走「歌单共现 + 标签 + 协同过滤」 |
| 4 | **算法音频特征** | **Spotify audio features** | 系统自动 | `valence`（情绪正负）、`energy`（强度）、`danceability`、`acousticness`、`tempo` 等，用于 mood 歌单与向量召回。⚠️ **2024-11 起 Spotify 对新应用关闭 audio-features API** |
| 5 | **DJ 软件自定义标签** ⭐ | **Rekordbox "My Tag"**、Serato | DJ 自己 | **分类化的自定义标签**（如 Energy / Mood / Situation）+ 颜色 + 交叉筛选；Mixed In Key 提供 energy 1–10。**"用户主观标签"最成熟的工业实践，结构最接近本方案** |
| 6 | **智能家居场景** | Home Assistant + **Music Assistant** | 用户 / 自动化 | 社区已有"AI 生成歌单 / 按场景放歌"实践，本质 = **场景标签 → 查询 → 播放队列** |

### 2.2 推荐系统如何使用标签（关键结论）

- **协同过滤（行为共现）**是主力召回；
- **标签 / 歌单**负责**冷启动与可解释性**（网易云、QQ 音乐的核心链路）；
- **音频特征（valence / arousal）**负责**内容侧召回**（Spotify mood 歌单）；
- 学术上情绪分类主流是 **valence–arousal 二维模型**（Russell / Thayer）；MIREX 音频情绪分类为 5 簇：passionate/rousing、sad/melancholic、calm/content、happy/cheerful、angry/aggressive；
- 行业标准做法是 **三者混合（hybrid）**。

### 2.3 对本项目的 5 条启发

1. **抄结构，不抄实现**：Rekordbox 的"分类 + 自由值 + 颜色"结构值得直接借鉴，避免 Last.fm 式的标签碎片化。
2. **情绪用维度而不是打勾**：`valence × arousal` 是行业与学术的公共语言，天然适配"回家听 / 运动听"的强弱差异。
3. **客观层自动、主观层手动**：Spotify 已证明"平台自算特征"对外不开放；但**本地能算的客观特征（BPM、能量）应该自动补**，把人从重复劳动里解放出来，只让人做机器做不了的事（情绪与情境）。
4. **平台标签可"抄"，个人语境抄不到**：协同过滤永远算不出"你在什么心情、什么场景下听这首歌"。这是本系统的**真正壁垒**，也是智能家居/助理最需要的输入。
5. **一切对外输出都要有稳定 ID**：跨端与家居对接的前提是稳定标识（本项目已有 `canonicalId`）。

---

## 3. 需求拆解与用户故事

| ID | 用户故事 | 验收要点 |
|---|---|---|
| US1 | 我**正在听**一首歌，突然觉得"这首适合回家放"，我想立刻标上 | 从播放页到打标完成 ≤ 2 次点击；可不中断播放 |
| US2 | 我想给**一批歌**统一打标（如整个专辑标记为"专注"） | 多选 → 贴标签，批量写库 |
| US3 | 我想**按标签听歌**（"把标了回家的歌都放一遍"） | 标签 → 播放队列（随机/顺序） |
| US4 | 我想让**智能家居**在"回家模式"下取歌 | 标签可作为查询条件导出（第 7、8 章） |
| US5 | 我想让**助理**分析我的听歌习惯 | 标签 + 播放事件可 join 分析 |
| US6 | 我想在**年度报告**里看到情绪维度 | 情绪分布、场景 TOP、情绪时间线 |
| US7 | 我打错了标签 / 想合并同义词 | 撤销（3 秒内）、标签管理页可改可并 |

---

## 4. 标签模型设计

### 4.1 三层结构

| 层 | 类别 | 例子 | 打标方式 | 数据形态 |
|---|---|---|---|---|
| **A 客观层** | `OBJECTIVE` | 风格、语种、年代、BPM 档、能量档、器乐/人声 | 自动（本地计算 / 网络获取） | 多值 |
| **B 情绪层** | `MOOD` | 开心、治愈、亢奋、热烈、致郁、低沉、引人思考、温柔、孤独、浪漫 | 用户打分 | **valence(1–5) × arousal(1–5) 二维打分** + 自由情绪词 |
| **C 情境层** | `CONTEXT` | 回家、通勤、户外运动、健身、专注、学习、睡前、深夜、雨天、聚会、开车 | 用户多选 | 布尔多值 |

**为什么是"受控分类 + 自由值"而不是纯自由标签**：纯自由标签（Last.fm 模式）会迅速碎片化（`chill` / `Chill` / `chilling` / `放松` 各算一个）。本方案：**维度受控**（三层固定），**值可在类内自由新增**，并提供**同义词归并**（4.3）。

### 4.2 情绪层为什么用 valence × arousal

- 是学术界与工业界的**公共坐标系**：valence = 情绪正负（低落 ↔ 愉悦），arousal = 唤醒强度（平静 ↔ 激烈）；
- 用户描述可自然映射：**"开心/治愈" = 高 valence**；**"亢奋/热烈" = 高 arousal**；**"致郁/低沉" = 低 valence**；**"引人思考/静心" = 低 arousal + 中性或偏低 valence**；
- 二维比"打勾"信息量大得多：同一首歌"回家听"（低 arousal）与"运动听"（高 arousal）都能表达；
- 为**智能家居**提供了可计算的查询条件（如 `arousal<=2`）；
- **兼顾「一键」与「精细」**：快捷情绪词自带二维坐标，点一下即完成打分，需要精确的人再展开微调 —— 见 4.6。

### 4.3 命名、同义词与归并

- **显示名**：中文优先（`回家`），可附英文别名（`home`）供 PC/助理使用；
- **稳定 ID**：每个标签有稳定字符串 `tagId`（如 `ctx.home`、`mood.arousal`），**导出与跨端合并只认它**，不认数据库自增 id；
- **同义词归并**：`tag.aliasOf` 指向另一个 `tagId`；合并后查询按目标标签聚合，历史 `song_tag` 不丢；
- **大小写/空白规范化**：新增自由值时先 trim + 大小写归一，命中已有别名则直接复用，避免重复标签。

### 4.4 标签来源 `source`

| 值 | 含义 |
|---|---|
| `preset` | 内置预设词表（随版本发布） |
| `user` | 用户手动打标（**最高可信度**） |
| `derived` | 本地自动推导（BPM/能量等客观特征） |
| `import` | 从外部 JSONL 导入 |

> 冲突策略：同一 `(canonicalId, tagId)` 上，**`user` 覆盖 `derived`**；同类来源按 `updatedAt` 后者胜（LWW）。

### 4.5 内置预设词表（草案，待审）

| 类别 | 预设值 |
|---|---|
| `CONTEXT` | 回家、通勤、户外运动、健身、专注工作、学习、睡前、深夜、雨天、聚会、开车、咖啡馆 |
| `MOOD`（自由词） | 开心、治愈、温柔、浪漫、亢奋、热烈、燃、致郁、低沉、孤独、引人思考、怀旧 |
| `MOOD`（维度） | `mood.valence`（1–5）、`mood.arousal`（1–5） |
| `OBJECTIVE` | 器乐、人声、纯音乐、BPM 档（慢/中/快）、能量档（低/中/高） |

> 预设只给"起点"，用户可删可改；`preset` 标签允许隐藏但不建议删除（保持跨设备一致性）。

### 4.6 情绪打分的「兼顾」设计（一键 = 打分，可微调）

**问题**：二维打分信息量大，但"每次都要拉两个滑条"摩擦高；纯打勾又快又糙。**两者必须兼得。**

**方案：把「打勾」和「打分」合成同一条路径 —— 快捷情绪词即预设坐标，点一下即完成打分；需要精确的人再展开微调。**

| 情绪词（快捷 chip） | valence | arousal |
|---|---|---|
| 开心 | 5 | 4 |
| 亢奋 | 4 | 5 |
| 热烈 | 4 | 5 |
| 燃 | 3 | 5 |
| 治愈 | 4 | 2 |
| 温柔 | 4 | 2 |
| 浪漫 | 4 | 3 |
| 引人思考 | 3 | 2 |
| 怀旧 | 3 | 2 |
| 致郁 | 2 | 2 |
| 低沉 | 2 | 2 |
| 孤独 | 2 | 2 |

**交互语义**：

1. **快捷模式（默认，1 次点击）**：点情绪词 chip → 同时写入 ①该情绪词（`MOOD` 自由标签）②对应的 `mood.valence` / `mood.arousal` 预设分。用户感知只是"打了个勾"，底层已拿到二维坐标。
2. **精细模式（可选，多 1 次操作）**：在已选 chip 上再次点击（或长按）→ 展开 `valence` / `arousal` 各 5 档滑条，可覆盖预设值。
3. **分数语义**：1–5 整数；**未打分则该维度无记录（NULL）**，不写默认值，避免污染。
4. **覆盖关系**：精细值覆盖预设值；均为 `source=user`，更新 `updatedAt`。
5. **UI 反馈**：chip 选中后显示小坐标（如 `4/5`）暗示可微调，但不强制。

**收益**：只想"打个标"的人 1 次点击、零学习成本；想精确的人多一次操作拿到二维分数；**数据层面两者写入同一张表**，智能家居与报告的查询逻辑不受影响。

---

## 5. 数据模型（Room，v12 → v13）

> 注：本方案起草时 DB 为 v10；实施基线（`3230f6b5`）已因展示层工作需要升到 **v12**（新增 `album_colors`、`report_overrides`）。故本系统的迁移为 **v12 → v13**，表名对齐既有命名风格（snake_case 复数）。

### 5.1 表结构

```sql
-- 标签定义
CREATE TABLE tags (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  tagId       TEXT    NOT NULL UNIQUE,   -- 稳定字符串 ID（导出/跨端唯一依据）
  name        TEXT    NOT NULL,          -- 显示名（中文优先）
  alias       TEXT,                      -- 英文/别名
  category    TEXT    NOT NULL,          -- OBJECTIVE | MOOD | CONTEXT
  kind        TEXT    NOT NULL,          -- BOOLEAN | SCALE
  colorArgb   INTEGER,
  sortOrder   INTEGER NOT NULL DEFAULT 0,
  isPreset    INTEGER NOT NULL DEFAULT 0,
  aliasOf     TEXT,                      -- 同义词归并指向的 tagId
  createdAt   INTEGER NOT NULL,
  updatedAt   INTEGER NOT NULL
);
CREATE INDEX idx_tags_category ON tags(category, sortOrder);

-- 歌曲 × 标签
CREATE TABLE song_tags (
  canonicalId TEXT    NOT NULL,          -- 复用 M1 的 canonicalId
  tagId       TEXT    NOT NULL,
  score       REAL,                      -- SCALE 类：1..5；BOOLEAN 类为 NULL
  source      TEXT    NOT NULL,          -- user | derived | import | preset
  updatedAt   INTEGER NOT NULL,
  deleted     INTEGER NOT NULL DEFAULT 0,-- 墓碑（跨端合并需要）
  PRIMARY KEY (canonicalId, tagId)
);
CREATE INDEX idx_song_tags_tag ON song_tags(tagId);
CREATE INDEX idx_song_tags_canonical ON song_tags(canonicalId);
```

### 5.2 为什么复用 `canonicalId`

M1 已实现 `CanonicalIdProvider`（`sha256:` 归一化元数据或来源 URI）：

- **跨端稳定**：手机与 PC 对同一首歌算出同一个 ID；
- **抗移动/改名**：文件移位、标签改写都不会丢标签；
- **可与播放事件 join**：`play_events.canonicalId` 与 `song_tag.canonicalId` 直接关联 → 报告与助理能做"情绪 × 播放行为"的联合分析；
- **省掉一套新身份体系**（YAGNI）。

### 5.3 为什么不把标签写进音频文件

APlayer 已有基于 taglib 的 ID3 读写能力（`AudioTagWriter`），但语义标签**默认不写文件**：

| 理由 | 说明 |
|---|---|
| 风险 | 写文件会修改用户原始文件，失败可能损坏 |
| 性能 | 批量打标 → 批量写文件，慢且耗电 |
| 表达能力 | ID3 `TMOO` 是**单值**帧，装不下"三层 + 分数" |
| 生态 | 其他播放器基本不读 `TMOO`，写了也白写 |
| 结论 | **DB 为准**；写文件做成**默认关闭的可选开关**（导出用），不作为数据源 |

### 5.4 迁移

- `AppDatabase.VERSION` 12 → 13，新增 `migration12to13`：**纯新增两张表（`tags` / `song_tags`）+ 索引 + 预设词表种子数据**，不触碰既有表（`play_events` / `album_colors` / `report_overrides`）；
- 种子数据在迁移中插入（保证版本一致、可重复执行幂等：`INSERT OR IGNORE`）；
- 回滚策略：仅需删除两张新表即可回到 v10 语义（不阻塞）。

---

## 6. 交互设计

### 6.1 播放中一键打标（**核心**，US1）

**动机**：用户"听的时候有感受" —— 这是唯一情绪最准确、意愿最强的时机；错过就只能事后回忆。

**入口**：播放页底部「标签」按钮 / 长按封面 → 弹出 `BottomSheet`。

**面板结构（自上而下）**：
1. **常用标签**（用户置顶，≤ 12 个）—— 一行 chips，点选即生效；
2. **情境**区：预设情境 chips（回家 / 通勤 / 运动 / 专注 / 睡前 / 深夜…）；
3. **情绪**区：**快捷情绪词 chips（点一下 = 打分，坐标见 4.6）**；已选 chip 可再次点击展开 `valence` / `arousal` 各 5 档微调；末行为自由情绪词/新建；
4. **更多标签**入口（进标签管理/新建）。

**交互规则**：
- **点选即写库**，无需"保存"按钮；
- 情绪词**一次点击即完成打分**（写入预设坐标），选中后再点一次进入微调（见 4.6）；
- 已选状态高亮；非情绪类标签再次点击即取消；
- 打标后 Snackbar 提供 **3 秒撤销**；
- 全程**不中断播放**；面板关闭后回到播放页原位。

**摩擦预算**：从播放页到"打上第一个标签" ≤ **2 次点击**。

### 6.2 批量打标（US2）

- 曲库/歌单/专辑页进入多选模式 → 选择歌曲 → 贴标签（同一面板，多选语义）；
- 支持"从已有标签中选"与"新建标签"；
- 支持按**当前筛选结果**全选打标。

### 6.3 标签管理页

- 按 `category` 分组列表（客观 / 情绪 / 情境）；
- 支持：新建、重命名、改色、拖拽排序、**合并同义词**（选择两个标签 → 合并）、删除（二次确认，提示会影响多少首歌）；
- 显示每个标签的**覆盖歌曲数**（便于发现"僵尸标签"）；
- 预设标签标识 `preset` 徽标，允许隐藏。

### 6.4 曲库筛选与按标签播放（US3）

- 曲库筛选栏新增「标签」入口：多选标签 + 类别分组；
- 筛选逻辑：**同类内 OR，跨类 AND**（"情境=回家 OR 通勤" 且 "arousal ≤ 2"）；
- 支持"筛选结果直接播放"（随机/顺序）；
- 标签详情页：该标签下的歌曲列表 + 统计（首歌数、总时长）。

### 6.5 摩擦控制原则

| 原则 | 落地 |
|---|---|
| 高频标签必须触手可及 | 置顶常用 ≤ 12，自动按使用频次排序 |
| 不要让人"输入" | 优先点选；新建标签时才输入 |
| 允许犯错 | 3 秒撤销 + 管理页批量修改 |
| 先给后要 | 能自动推的客观标签直接给，不让人填 |

---

## 7. 只读查询接口（给报告层 / 智能家居 / 助理）

### 7.1 仓库 API（只读部分）

```kotlin
interface SongTagRepository {
  // 歌曲 → 标签
  suspend fun tagsOf(canonicalId: String): List<SongTagItem>
  // 标签 → 歌曲
  suspend fun songsOf(tagId: String, limit: Int = 0): List<String>   // 返回 canonicalId 列表
  // 标签定义
  suspend fun allTags(): List<TagDef>
  suspend fun topTags(limit: Int = 20): List<TagUsage>              // 按覆盖歌曲数
  // 报告层用（第 7.3 与 PlayEvent join）
  suspend fun moodDistribution(year: Int): List<MoodBucket>         // valence/arousal 分桶
  suspend fun contextTop(year: Int, limit: Int = 10): List<TagUsage>
  suspend fun tagCoverage(): TagCoverage                            // 打标覆盖率
}
```

### 7.2 场景查询 DSL 草案（供家居 / 助理，后续落成文件或本地 HTTP）

```
context=回家
mood.arousal<=2
mood.valence>=3
tag=深夜
year=2026
played_days>=2
sort=plays|recent|random
limit=50
```
- 语义：同一 key 多值 = OR；不同 key = AND；`sort` 默认 `plays`；
- 该 DSL 是**契约**：家居/助理侧只依赖它，不依赖数据库结构。

### 7.3 与 PlayEvent 的 join

`play_events.canonicalId` ↔ `song_tag.canonicalId`，可直接产出：

| 指标 | 用途 |
|---|---|
| 各情绪桶的播放占比 | 年度报告"情绪分布" |
| 场景 × 播放次数 | "你在'回家'场景听了 N 首 / M 小时" |
| valence 随时间（按月）的均值 | "年度情绪曲线" |
| 打标覆盖率 | 数据质量提示 |

---

## 8. 跨端互操作：`song-tags.jsonl`（schema v1）

### 8.1 格式

每行一个 JSON 对象，两种 `type`：

```json
{"schemaVersion":1,"type":"tag_def","tagId":"ctx.home","name":"回家","alias":"home","category":"CONTEXT","kind":"BOOLEAN","color":"#FF8C9BFF","updatedAt":"2026-09-27T12:00:00Z"}
{"schemaVersion":1,"type":"song_tag","canonicalId":"sha256:ab12…","tagId":"ctx.home","score":null,"source":"user","updatedAt":"2026-09-27T12:00:00Z","deviceId":"android-1f3a…","track":{"title":"Color Your Night","artist":"Atlus Sound Team","album":"P3R"}}
```

要点：
- **只认 `tagId` 字符串**（不导出数据库自增 id）；
- `track` 是**快照**，便于 PC/助理在没有音频文件时也能展示；
- 时间统一 **UTC ISO 8601**（与 `play-events-<year>.jsonl` 保持一致）；
- 删除用**墓碑**：`{"type":"song_tag","deleted":true,…}`。

### 8.2 合并语义

| 场景 | 规则 |
|---|---|
| 主键 | `(canonicalId, tagId)` |
| 冲突 | 按 `updatedAt` 后者胜（LWW）；`source=user` 优先于 `derived` |
| 删除 | 墓碑参与比较，时间新的墓碑生效 |
| `tag_def` | 按 `tagId` 幂等 upsert |
| 幂等 | 同一文件重复导入结果不变（仿 M3 的 `eventId` 幂等） |

### 8.3 与 M3 的关系

M3 已实现 `play-events-<year>.jsonl` 的**导出 + 幂等导入**；本系统沿用同一套思路，产出 `song-tags.jsonl`。二者通过 `canonicalId` 在同一份数据上对齐，PC 侧只需实现一次"按 key 幂等合并"。

---

## 9. 与另外两条线（展示层）的接口冻结

| 约定 | 内容 |
|---|---|
| 本线**不修改**的文件 | `annual-report-presentation-*.md`、`AnnualReportScreen.kt`、S1–S12 页面、`ReportPoster.kt`、`tools/shots.ps1` |
| 本线**新增**的文件 | 标签表/DAO/仓库、标签管理页、打标面板、导出器、本设计文档与实施包 |
| 展示层如何拿到标签数据 | 只走 **7.1 的只读 API**（`moodDistribution` / `contextTop` / `tagCoverage`） |
| 变更流程 | 先改本文档并同步双方，再改实现；**接口冻结后各自独立实现** |
| 隔离方式 | 本线在**独立 worktree + 独立分支**（`feature/song-tags`）开发，物理隔离 |
| 已知冲突点 | 需要动 `AppNav.kt`（+1 route）与 `AndroidManifest.xml`（+1 deep link host），是**唯一**两处与展示层重叠的文件；采用最小 patch，合并时按行解冲突 |

---

## 10. 分期计划

| 阶段 | 内容 | 交付物 | 依赖 |
|---|---|---|---|
| **T0（MVP）** | 标签表 + 迁移 v13 + 种子词表 + 标签管理页 + **播放中一键打标** + 曲库按标签筛选 | 可打标、可筛选 | 无 |
| **T1** | 情境面板完善 + 批量打标 + 撤销 + 标签详情页/按标签播放 | 完整体验闭环 | T0 |
| **T2** | `song-tags.jsonl` 导出 + 幂等导入 + 只读聚合 API（给展示层） | 跨端与报告可用 | T1 |
| **T3** | 自动客观标签（本地算 BPM/能量）+ 场景查询 DSL 落地（文件/HTTP）+ 智能家居对接 | 家居/助理可用 | T2 |

> 每个阶段独立可验收；T0 完成即可开始真实使用（用户先积累标签数据）。

---

## 11. 风险与对策

| # | 风险 | 影响 | 对策 |
|---|---|---|---|
| 11.1 | **自动情绪打标不准**，污染用户数据 | 信任崩塌 | 情绪层**只由用户打**；`derived` 仅用于客观层，且可一键清空 |
| 11.2 | **标签碎片化**（同义标签泛滥） | 查询失效 | 受控分类 + 别名归并 + 新增时先查别名 |
| 11.3 | **打标摩擦高**，用户不用 | 系统空转 | 一键面板、点选即生效、≤2 次点击、置顶常用 |
| 11.4 | 写音频文件损坏用户文件 | 数据事故 | 默认不写（5.3），写文件为可选开关 |
| 11.5 | 与展示层**改同一批文件**冲突 | 互相覆盖 | 第 9 章接口冻结 + 只新增文件 |
| 11.6 | 标签数据量大后筛选变慢 | 卡顿 | `song_tag(tagId)` / `(canonicalId)` 建索引；筛选走分页 |
| 11.7 | 跨端合并产生"幽灵标签" | 数据不一致 | 墓碑 + LWW + 幂等导入 + 覆盖率检查 |

---

## 12. 验收标准

- **功能**：US1–US7 全部可用；打标 ≤ 2 次点击；批量打标成功；按标签筛选/播放正确。
- **情绪打分**：点情绪词 chip **1 次点击**即写入 `valence`/`arousal` 预设坐标；展开微调可覆盖；**未打分的维度保持 NULL**（不写默认值）。
- **数据**：v12 → v13 迁移不丢数据、可重复执行；`(canonicalId, tagId)` 唯一；同义词合并后查询结果不丢。
- **互操作**：导出的 `song-tags.jsonl` 重新导入结果幂等；跨端（PC）按 `tagId` 可对齐。
- **性能**：1 万首歌 × 5 万个标签关系下，标签筛选响应 < 200ms（真机实测）。
- **边界**：无标签时 UI 不崩；删除标签有影响提示；面板不中断播放。
- **隔离**：不修改展示层任何文件（第 9 章）；展示层只读 API 可用。

---

## 13. 决策记录（已确认）

> 用户审阅通过：**除 D2 需「两者兼顾」外，其余全部按推荐执行**。

| # | 问题 | 结论 |
|---|---|---|
| D1 | 标签风格：受控词表为主 vs 纯自由标签 | ✅ **受控三层 + 类内自由值**（Rekordbox 模式） |
| D2 | 情绪表达：二维打分 vs 简单打勾 | ✅ **两者兼顾**：快捷情绪词 1 次点击 = 写入 valence/arousal 预设坐标（见 4.6）；可展开微调覆盖；未打分维度保持 NULL |
| D3 | 打标时机优先级：播放中 vs 事后批量 | ✅ **播放中为主**（T0 必做），事后批量为 T1 |
| D4 | 是否做自动标签 | ✅ **只对客观层自动**（BPM/能量），情绪层不自动 |
| D5 | 是否写音频文件 | ✅ **默认不写**，仅导出开关 |
| D6 | 是否做跨端导出协议 | ✅ **做**（T2），格式见第 8 章 |
| D7 | 与展示层的优先级 | ✅ 展示层由另两条线推进；本线并行从 T0 起步，T2 再交付只读 API |
| D8 | 预设词表内容（4.5 草案） | ✅ 先按草案落地，使用中迭代 |

---

## 14. 出典

- [主流音乐平台标签体系全详解](https://zhuanlan.zhihu.com/p/2021569324859564523)
- [Spotify audio_features 已下线（2024）与其替代](https://dev.to/birrings/spotifys-audiofeatures-api-died-in-2024-heres-what-i-built-to-replace-it-3dn3)
- [Rekordbox My Tag 用法](https://vibesdj.io/how-to/use-my-tag-in-rekordbox)
- [beets 文档（flexible attributes / lastgenre 取 Last.fm 标签）](https://beets.readthedocs.io/)
- [MIREX Audio Mood Classification](https://www.music-ir.org/mirex/abstracts/2010/WLB4.pdf)
- [Song Emotion Detection Based on Arousal-Valence](https://ieeexplore.ieee.org/document/8982519)
- [MusicBrainz Picard 文档（ID3 帧映射）](https://picard-docs.musicbrainz.org/)
- [Home Assistant + Music Assistant 场景歌单实践](https://community.home-assistant.io/t/ha-music-assistant-ai-playlist-for-local-storage-music/1024499)

---

## 附：与既有文档的衔接

| 既有文档 | 关系 |
|---|---|
| `docs/play-event-design.md` | 提供 `canonicalId` 身份体系与分层路线图；标签系统是其"语义层"扩展 |
| `docs/play-event-protocol-spec.md` | 提供 JSONL 互操作范式，本方案第 8 章沿用 |
| `docs/annual-report-design.md` | 报告层消费本方案 7.3 的聚合指标（情绪分布/场景 TOP/情绪曲线） |
| `docs/annual-report-presentation-design.md` | 展示层（另两条线）消费只读 API，不直接依赖标签表结构 |
