# 歌曲标签系统 · T0 实施包

> **本文档是唯一执行依据。** 设计动机与调研见 `docs/song-tag-system-design.md`（那份回答"为什么"，本文档回答"怎么做"，冲突时**以本文档为准**）。
> 目标：执行者**不需要再做任何设计决策**，按任务逐条落地即可。
> 基线提交：`3230f6b5`（分支 `feature/song-tags`，独立 worktree `S:\Workplace\APlayer-tags`）。
> 范围：只做 **T0（MVP）**；T1–T3 见设计文档第 10 章。

---

## 0. 执行须知

### 0.1 纪律（违反即回滚）

1. **只在 `S:\Workplace\APlayer-tags` 工作**（独立 worktree，分支 `feature/song-tags`）。**不要**在主 worktree `S:\Workplace\APlayer` 里改代码 —— 那里有另外两条线（展示层）在并行工作。
2. **只推 `origin`**（`GuiltySpark-343/APlayer`），任何情况都不推 `upstream`。
3. **不新增任何依赖**。本实施包只用项目已有的 Room / Hilt / Compose / Flow / Coil。
4. **不改动既有数据层接口**：`PlayEventDao`、`PlayEventRepository`、`AnnualReport`、`SongRepository` 的现有签名与语义**一律不动**，只允许新增。
5. **不改动既有表**：`migration12to13` 只允许 `CREATE TABLE` / `CREATE INDEX` / 对**新表**写种子数据。
6. **不改动展示层文件**：`ui/screen/report/**`、`ui/screen/setting/SettingScreen.kt`、`ReportPoster.kt`、`ReportStory.kt`、`annual-report-*.md`、`docs/screenshots/` 下他人产物。
   - **唯一例外**：T0.7 允许对 `ui/nav/AppNav.kt`（+1 route）与 `app/src/main/AndroidManifest.xml`（+1 deep link host）做**最小 patch**（各 1–2 行）。
7. **一次只做一个 Task**，做完即按验收命令验证并提交；验证不过不得进入下一个 Task。
8. 每个 Task 结束必须 `:app:assembleNormalDebug` 成功。

### 0.2 环境与固定命令

```powershell
# 工作目录（注意是 -tags）
cd S:\Workplace\APlayer-tags
$env:JAVA_HOME="D:\Application2\jdk-17.0.3.1"; $env:ANDROID_HOME="D:\Application2\Android\Sdk"; $env:ANDROID_SDK_ROOT="D:\Application2\Android\Sdk"

# 构建
.\gradlew.bat :app:assembleNormalDebug --console=plain

# 构建失败且报 Gradle 类加载器 / configuration-cache 错误（守护进程损坏，非代码问题）
.\gradlew.bat --stop
Remove-Item -Recurse -Force ".gradle\configuration-cache" -ErrorAction SilentlyContinue

# 产物
app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk

# 设备（vivo V2425A，Android 16；debug 包名 remix.myplayer.debug）
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
& $adb devices
& $adb install -r "app/build/outputs/apk/normal/debug/APlayer-v2.1.1.0-normal-debug.apk"
```

**装不上时**（`INSTALL_FAILED_ABORTED: User rejected permissions`）：手机**解锁并亮屏**后重试；仍失败则
`adb push <apk> /sdcard/Download/` 让用户手动安装。

> ⚠️ **共用设备的安装冲突（实测踩到）**：本工作区与展示层工作区**共用同一台手机、同一个包名** `remix.myplayer.debug`，
> 谁后安装谁覆盖。已实测到「本线的 build 被展示层的 build 覆盖」，表现为 deep link 解析失败
> （`am start` 报 `unable to resolve Intent ... aplayer://song_tags`）。
>
> **判定当前装的是谁的包**（唯一可靠办法，看 manifest 里的 host）：
> ```powershell
> $p = (& $adb shell pm path remix.myplayer.debug) -replace 'package:',''
> & $adb pull $p.Trim() "$env:TEMP\installed.apk"
> & 'D:/Application2/Android/Sdk/build-tools/35.0.0/aapt2.exe' dump xmltree --file AndroidManifest.xml "$env:TEMP\installed.apk" | Select-String 'android:host'
> ```
> 验证本线功能前，必须先确认 host 列表包含 `song_tags`；否则先装本线的包。

### 0.3 验证协议（本实施包的核心验收手段）

#### 0.3.1 图像能力说明（重要）

本线执行者**不支持图像输入**（`read_image` 会报 `model does not declare image input`）。因此：

- **视觉验收由人类用户执行**（截图发给用户看）；
- 执行者只能做**程序化检查**（像素/区域/尺寸断言，见 0.3.3）。

#### 0.3.2 数据断言（数据层 Task 必做，最强证据）

把设备上的真实数据库拉回本地，用 Python 断言结构与数据**幂等**：

```powershell
# 注意：**不要**用 PowerShell 的 `>` 重定向接 adb 二进制输出（会按文本处理导致文件损坏，
# 表现为 sqlite3 "file is not a database"）。必须用 Start-Process -RedirectStandardOutput。
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
$tmp = "$env:TEMP\tagsdb"; New-Item -ItemType Directory -Force -Path $tmp | Out-Null
Remove-Item "$tmp\*" -Force -ErrorAction SilentlyContinue
foreach ($f in @('aplayer.db','aplayer.db-wal','aplayer.db-shm')) {
  Start-Process -FilePath $adb -ArgumentList @('exec-out','run-as','remix.myplayer.debug','cat',"databases/$f") `
    -RedirectStandardOutput "$tmp\$f" -NoNewWindow -Wait
}
python tools/assert_tags_db.py "$tmp\aplayer.db"     # 见 T0.1 交付物
```

> 三个文件必须拉到**同一目录**，SQLite 打开时会自动重放 WAL，否则可能看不到最新写入。

#### 0.3.3 程序化截图检查（无人类时的退路）

```powershell
& $adb shell am start -a android.intent.action.VIEW -d "aplayer://song_tags"   # 依赖 T0.7
Start-Sleep -Seconds 3
& $adb exec-out screencap -p > "docs/screenshots/<task-id>.png"
python tools/check_shot.py "docs/screenshots/<task-id>.png" --bands 3   # 分带 ink 检查（非空/不重叠）
```

#### 0.3.4 截图存放

统一存 `docs/screenshots/`，随 Task 提交；命名 `<task-id>-<name>.png`（如 `t0.5-tag-panel.png`）。

### 0.4 进度台账

每个 Task 完成后，在 **`docs/song-tag-progress.md`** 追加一行：

```
| Task | 状态 | 提交 | 构建 | 验证 | 备注 |
| T0.1 | ✅ | abc1234 | 通过 | 数据断言：tags/song_tags 存在，索引 3 个 | migration12to13 |
```

---

## 1. 基线（执行前必须全部为真）

| # | 事实 | 校验方式 |
|---|---|---|
| 1 | 分支 `feature/song-tags`，HEAD = `3230f6b5` | `git log --oneline -1` |
| 2 | `AppDatabase.VERSION = 12`；实体含 `AlbumColor` / `ReportOverride` / `PlayEvent` | 读 `AppDatabase.kt:81`、`:46-58` |
| 3 | 迁移写法：`val migrationXtoY = object : Migration(X, Y)`，DDL 用 `CREATE TABLE IF NOT EXISTS` | 读 `DbMigrations.kt:111/119` |
| 4 | 表名风格：snake_case 复数（`album_colors` / `report_overrides` / `play_events`） | 读实体文件 |
| 5 | 数据库文件名 `aplayer.db`（非 `APlayer.db`） | 读 `AppDatabase.kt:100` |
| 6 | 独立 worktree 可构建（已验证 `BUILD SUCCESSFUL in 3m37s`）；`local.properties` 与 `third-party/taglib` 源码已就位 | 已在建立隔离环境时验证 |
| 7 | **不存在**任何标签代码：无 `tags` / `song_tags` 表、无 `SongTag*` 类 | `git grep -i "song_tag"` 应为空 |
| 8 | `canonicalId` 生成器存在 | `util/CanonicalIdProvider.kt` |
| 9 | DI 绑定模板 | `di/RepositoryModule.kt`（`@Binds` + `@Singleton`） |

---

## 2. 交付物与数据契约

### 2.1 表结构（`migration12to13` 必须与此完全一致）

```sql
CREATE TABLE IF NOT EXISTS `tags` (
  `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `tagId`     TEXT    NOT NULL,
  `name`      TEXT    NOT NULL,
  `alias`     TEXT,
  `category`  TEXT    NOT NULL,
  `kind`      TEXT    NOT NULL,
  `colorArgb` INTEGER,
  `sortOrder` INTEGER NOT NULL DEFAULT 0,
  `isPreset`  INTEGER NOT NULL DEFAULT 0,
  `aliasOf`   TEXT,
  `createdAt` INTEGER NOT NULL,
  `updatedAt` INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_tagId` ON `tags` (`tagId`);
CREATE INDEX IF NOT EXISTS `index_tags_category` ON `tags` (`category`, `sortOrder`);

CREATE TABLE IF NOT EXISTS `song_tags` (
  `canonicalId` TEXT    NOT NULL,
  `tagId`       TEXT    NOT NULL,
  `score`       REAL,
  `source`      TEXT    NOT NULL,
  `updatedAt`   INTEGER NOT NULL,
  `deleted`     INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(`canonicalId`, `tagId`)
);
CREATE INDEX IF NOT EXISTS `index_song_tags_tagId` ON `song_tags` (`tagId`);
CREATE INDEX IF NOT EXISTS `index_song_tags_canonicalId` ON `song_tags` (`canonicalId`);
```

> 唯一索引名必须与 Room 生成的名称一致（`index_<table>_<col>`），否则 schema 校验会失败。

### 2.2 预设词表（种子，`TagSeeds.kt`）

**category**：`OBJECTIVE` | `MOOD` | `CONTEXT`；**kind**：`BOOLEAN` | `SCALE`

| tagId | name | category | kind | 附加 |
|---|---|---|---|---|
| `ctx.home` … | 回家 / 通勤 / 户外运动 / 健身 / 专注工作 / 学习 / 睡前 / 深夜 / 雨天 / 聚会 / 开车 / 咖啡馆 | CONTEXT | BOOLEAN | `ctx.*` |
| `mood.happy` | 开心 | MOOD | BOOLEAN | 预设坐标 5/4 |
| `mood.energetic` | 亢奋 | MOOD | BOOLEAN | 4/5 |
| `mood.passionate` | 热烈 | MOOD | BOOLEAN | 4/5 |
| `mood.burn` | 燃 | MOOD | BOOLEAN | 3/5 |
| `mood.healing` | 治愈 | MOOD | BOOLEAN | 4/2 |
| `mood.gentle` | 温柔 | MOOD | BOOLEAN | 4/2 |
| `mood.romantic` | 浪漫 | MOOD | BOOLEAN | 4/3 |
| `mood.thoughtful` | 引人思考 | MOOD | BOOLEAN | 3/2 |
| `mood.nostalgic` | 怀旧 | MOOD | BOOLEAN | 3/2 |
| `mood.depressive` | 致郁 | MOOD | BOOLEAN | 2/2 |
| `mood.low` | 低沉 | MOOD | BOOLEAN | 2/2 |
| `mood.lonely` | 孤独 | MOOD | BOOLEAN | 2/2 |
| `mood.valence` | 情绪正负 | MOOD | SCALE | 值域 1–5 |
| `mood.arousal` | 唤醒强度 | MOOD | SCALE | 值域 1–5 |
| `obj.instrumental` / `obj.vocal` | 器乐 / 人声 | OBJECTIVE | BOOLEAN | — |
| `obj.bpm.slow` / `.mid` / `.fast` | 慢速 / 中速 / 快速 | OBJECTIVE | BOOLEAN | T3 自动填充 |
| `obj.energy.low` / `.mid` / `.high` | 低能量 / 中能量 / 高能量 | OBJECTIVE | BOOLEAN | T3 自动填充 |

- 种子写入用 `INSERT OR IGNORE`（幂等，可重复执行）。
- **情绪坐标表**（4.6 的预设坐标）以常量形式放在 `TagSeeds.kt`，供打标面板一键写分：`mood.happy → 5/4` 等。

### 2.3 仓库 API（T0.3）

```kotlin
interface SongTagRepository {
  suspend fun allTags(): List<TagDef>
  suspend fun tagsOf(canonicalId: String): List<SongTagRow>            // 排除 deleted
  suspend fun tagSong(canonicalId: String, tagId: String, score: Double? = null, source: String = "user")
  suspend fun untagSong(canonicalId: String, tagId: String)            // 写墓碑
  suspend fun songsOf(tagId: String, limit: Int = 0): List<String>     // canonicalId 列表
  suspend fun usageCounts(): Map<String, Int>                          // tagId -> 歌曲数
  suspend fun createTag(name: String, category: String, kind: String, colorArgb: Int?): String  // 返回 tagId
  suspend fun renameTag(tagId: String, newName: String)
  suspend fun mergeTag(fromTagId: String, intoTagId: String)
  suspend fun deleteTag(tagId: String)
}
```

> T0 只做上面这些；设计文档第 7 章的聚合 API（`moodDistribution` 等）属于 T2，不在本包范围。

### 2.4 路由与入口（T0.7）

| 项 | 值 |
|---|---|
| route 常量 | `const val RouteSongTags = "song_tags"`（`ui/nav/AppNav.kt`） |
| deep link | `aplayer://song_tags` |
| 设置入口 | 设置最外层新增一项「歌曲标签」（与「年度听歌报告」同级） |
| 标签管理页 | `ui/screen/tag/TagManagerScreen.kt` |
| 打标面板 | `ui/screen/tag/TagPanelSheet.kt`（BottomSheet，可被播放页与曲库多选复用） |

---

## 3. 任务包

### T0.1 数据层骨架（entity + dao + migration + 版本号）

**新增**
- `data/db/room/entity/TagDef.kt`（表 `tags`）
- `data/db/room/entity/SongTag.kt`（表 `song_tags`）
- `data/db/room/dao/TagDao.kt`
- `tools/assert_tags_db.py`（数据断言语料）

**修改（最小）**
- `data/db/DbMigrations.kt`：新增 `migration12to13`
- `data/db/room/AppDatabase.kt`：`VERSION = 13`；`entities` 加两个实体；加 `abstract fun tagDao(): TagDao`；`.addMigrations(migration12to13)`

**要点**
- 实体 `@Entity(tableName=..., indices=[...])` 的索引声明必须与 2.1 的 DDL 名称/列一致（Room 会校验 `exportSchema=true`）。
- `song_tags` 用 `primaryKeys = ["canonicalId", "tagId"]`。
- 迁移里**只**建表建索引（种子数据放 T0.2）。

**验收**
1. `.\gradlew.bat :app:assembleNormalDebug --console=plain` → BUILD SUCCESSFUL
2. 装包启动一次（触发迁移）→ 0.3.2 数据断言：
   - `tags` / `song_tags` 表存在
   - 索引 `index_tags_tagId`（唯一）、`index_tags_category`、`index_song_tags_tagId`、`index_song_tags_canonicalId` 存在
   - `PRAGMA user_version` = 13
3. **回归**：`play_events` / `album_colors` / `report_overrides` 行数与迁移前一致（先记录再比对）

**回滚**：`git checkout -- <改动文件>` + 删除新增文件。

---

### T0.2 预设词表种子 + 情绪坐标

**新增**
- `data/model/tag/TagSeeds.kt`：`PRESET_TAGS`（tagId/name/category/kind/color/sortOrder）与 `MOOD_PRESETS`（tagId → valence/arousal）

**修改（最小）**
- `DbMigrations.kt` 的 `migration12to13`：追加 `INSERT OR IGNORE INTO tags(...) VALUES ...`（每条一行 SQL 或循环 `execSQL`）
- 迁移同时写入 `mood.valence` / `mood.arousal` 两个 SCALE 定义

**要点**
- 种子必须在**迁移**里写（而不是首次启动 upsert），保证任何设备/PC 的预设 tagId 集合一致。
- `createdAt/updatedAt` 用固定常量（如 `0`）或迁移执行时的当前时间；**必须确定性**，便于断言。

**验收**
1. 构建通过
2. 数据断言：`SELECT COUNT(*) FROM tags WHERE isPreset=1` = 预设条数；`SELECT tagId FROM tags ORDER BY tagId` 与 `TagSeeds.PRESET_TAGS` 完全一致
3. 重复执行迁移不产生重复行（幂等）：断言 `COUNT(*) = COUNT(DISTINCT tagId)`

---

### T0.3 仓库层 + DI

**新增**
- `repo/SongTagRepository.kt`（接口，签名见 2.3）
- `repo/SongTagRepoImpl.kt`（`@Inject constructor(private val tagDao: TagDao)`）
- `di/RepositoryModule.kt` 追加 `@Binds`（**只追加，不改已有行**）

**要点**
- `untagSong` 写墓碑（`deleted = 1`，更新 `updatedAt`），不物理删除 —— 为跨端合并保留语义。
- `mergeTag(from, into)`：把 `song_tags` 中 `fromTagId` 行改写为 `intoTagId`（冲突时保留 `updatedAt` 较新者），再删除 `from` 的标签定义。
- `createTag` 生成稳定 `tagId`：`user.<sha256(name).take(12)>`（或 `user.<timestamp>`）；在文档注释里写明规则。

**验收**
1. 构建通过
2. 数据断言（脚本里加一段）：`tagSong` → 行存在且 `score` 正确；重复 `tagSong` 幂等（行数不变、`updatedAt` 更新）；`untagSong` → `deleted=1`；`mergeTag` → 关系行迁移且无重复

---

### T0.4 标签管理页

**新增**
- `ui/screen/tag/TagManagerScreen.kt` + `viewmodel/TagManagerViewModel.kt`

**功能**
- 按 `category` 分组列表（客观 / 情绪 / 情境），显示每项**覆盖歌曲数**
- 新建标签（名称 + 类别 + 颜色）、重命名、改色、删除（二次确认并提示影响歌曲数）
- 合并同义词（选两个 → 合并）
- `preset` 项显示徽标；允许隐藏（T1 再做隐藏开关，T0 只展示徽标）

**要点**
- 复用项目既有组件：`CommonAppBar`、`Preference`/`TextPrimary`/`TextSecondary`，配色走 `LocalTheme.current`。
- 删除/合并危险操作必须有确认对话框。

**验收**
1. 构建通过
2. 截图（人工看）：分组正确、中文无乱码、长标签名不溢出
3. 数据断言：新建/重命名/删除后 `tags` 行与预期一致

---

### T0.5 播放中一键打标面板（**核心**）

**新增**
- `ui/screen/tag/TagPanelSheet.kt`（ModalBottomSheet，入参 `canonicalId` + 标题）
- `viewmodel/TagPanelViewModel.kt`（或复用 `TagManagerViewModel`，二选一，不得两套逻辑）

**面板结构（自上而下）**
1. 常用标签（置顶，≤ 12）——点选即生效
2. 情境区（`CONTEXT` chips）
3. 情绪区（`MOOD` chips）：
   - **点一下 = 打分**：写入该情绪词 + 该词的预设 `valence`/`arousal`（见 2.2 坐标表）
   - **已选 chip 再点一下** → 展开 `valence` / `arousal` 各 5 档微调，可覆盖预设值
4. 更多/新建入口

**要点**
- 点选即写库（无"保存"按钮）；关闭面板不中断播放
- 打标后 Snackbar 提供 **3 秒撤销**（撤销 = `untagSong`）
- chip 选中后显示小坐标（如 `4/5`）暗示可微调
- **未打分的维度不写记录**（不写默认值）

**验收**
1. 构建通过
2. 交互：从播放页到打上第一个标签 ≤ 2 次点击
3. 数据断言：点 `开心` → `song_tags` 中有 `mood.happy`；且 `mood.valence=5`、`mood.arousal=4` 两行；微调后分数被覆盖；撤销后三行均为 `deleted=1`

---

### T0.6 曲库按标签筛选 + 按标签播放

**新增**
- `ui/screen/tag/TagFilterSheet.kt`（筛选面板）+ 曲库接入点
- 查询：`songsOf(tagId)` → 与 `SongRepository` 的全量歌曲列表求交（`canonicalId` 匹配）

**要点**
- 筛选逻辑：**同类内 OR，跨类 AND**
- 支持"筛选结果直接播放"（随机/顺序），投递到既有播放队列入口（复用现成 API，不新造）
- 大列表分页/懒加载，避免一次查全量

**验收**
1. 构建通过
2. 数据断言 + 功能：给 3 首歌打 `ctx.home` → 筛选"回家"结果恰为 3 首
3. 性能：1 万首歌下筛选响应 < 200ms（真机初测）

---

### T0.7 入口、路由与字符串（**唯一允许改 AppNav / manifest 的 Task**）

**修改（最小 patch）**
- `ui/nav/AppNav.kt`：`const val RouteSongTags = "song_tags"` + `normalAnimatedScreen(RouteSongTags, deepLinks = listOf(NavDeepLink("aplayer://song_tags".toUri()))) { TagManagerScreen() }`
- `app/src/main/AndroidManifest.xml`：既有 `aplayer` scheme 的 intent-filter 内 +1 行 `<data android:scheme="aplayer" android:host="song_tags" />`
- `ui/screen/setting/SettingScreen.kt`：设置最外层 +1 个入口（与「年度听歌报告」同级）
- 播放页：+1 个「标签」按钮（打开 T0.5 面板）

**冲突提示**：`AppNav.kt` / `AndroidManifest.xml` / `SettingScreen.kt` 是与展示层重叠的文件，合并时**按行解冲突**，保持 patch 最小。

**验收**
1. 构建通过，安装成功
2. `adb shell am start -a android.intent.action.VIEW -d "aplayer://song_tags"` 直达标签管理页
3. 截图（人工看）

---

### T0.8 真机整体验证 + 台账回填 + 提交

1. 全流程走一遍：新建标签 → 播放页打标（一键 + 微调 + 撤销）→ 曲库筛选 → 按标签播放
2. 数据断言全绿（含**迁移幂等**：连装两次不重复、不丢 `play_events`）
3. `docs/song-tag-progress.md` 回填全部行
4. 提交（只提交本包涉及的文件）

---

## 4. 任务依赖图

```
T0.1 ──> T0.2 ──> T0.3 ──┬─> T0.4 ──┐
                          ├─> T0.5 ──┼─> T0.7 ──> T0.8
                          └─> T0.6 ──┘
```
T0.1/T0.2 必须串行；T0.3 完成后 T0.4/T0.5/T0.6 可并行（但本包要求串行提交，避免冲突）。

---

## 5. 风险与回滚

| # | 风险 | 对策 |
|---|---|---|
| R1 | Room schema 校验失败（索引名/列名不匹配） | 严格照 2.1 的 DDL；`exportSchema=true` 会直接报错 |
| R2 | 迁移把既有数据搞坏 | 迁移**只建新表**；T0.1 验收含既有表行数回归比对 |
| R3 | 与展示层合并冲突 | 独立分支；只有 3 个文件重叠且 patch 极小的 Task 集中在 T0.7 |
| R4 | 打标摩擦高导致功能空转 | T0.5 的 ≤2 次点击是硬验收；快捷 chip 即打分 |
| R5 | 情绪数据被污染 | **不写默认值**；未打分维度不落库；撤销可用 |
| R6 | 并发构建撞车（主树另一线在构建）| 本线在独立 worktree，`app/build` 独立；若同时构建变慢属正常，不视为失败 |
| R7 | 设备未解锁导致装不上 | 先亮屏解锁；仍失败改 push 到 Download 手动装 |

**全局回滚**：`git reset --hard 3230f6b5`（本分支只做新增，回滚无副作用）。

---

## 6. 自检记录（执行中回填）

| Task | 提交 | 构建 | 数据断言 | 截图 | 备注 |
|---|---|---|---|---|---|
| T0.1 | | | | | |

---

## 7. 附录：新增字符串总表（唯一清单）

| key | 中文 | 英文 | 用途 |
|---|---|---|---|
| `song_tags` | 歌曲标签 | Song Tags | 条目/标题 |
| `tag_category_objective` | 客观 | Objective | 分组 |
| `tag_category_mood` | 情绪 | Mood | 分组 |
| `tag_category_context` | 情境 | Context | 分组 |
| `tag_new` | 新建标签 | New Tag | 按钮 |
| `tag_rename` | 重命名 | Rename | 菜单 |
| `tag_change_color` | 改颜色 | Color | 菜单 |
| `tag_merge` | 合并同义词 | Merge | 菜单 |
| `tag_delete` | 删除 | Delete | 菜单 |
| `tag_delete_confirm` | 删除后会影响 %1$d 首歌，确定？ | Affects %1$d songs. Continue? | 确认框 |
| `tag_merged_toast` | 已合并到「%1$s」 | Merged into %1$s | Toast |
| `tag_added_toast` | 已打上「%1$s」 | Tagged %1$s | Toast |
| `tag_undo` | 撤销 | Undo | Snackbar |
| `tag_filter` | 按标签筛选 | Filter by Tag | 曲库 |
| `tag_play_all` | 播放全部 | Play All | 按钮 |
| `tag_score_hint` | 点一下即打分，再点可微调 | Tap to rate, tap again to fine-tune | 面板说明 |
| `tag_valence` | 情绪正负 | Valence | 微调标签 |
| `tag_arousal` | 唤醒强度 | Arousal | 微调标签 |
| `tag_more` | 更多标签 | More | 面板 |
| `tag_empty` | 还没有标签，先新建一个吧 | No tags yet | 空态 |

> 中英两套都要加；`values-zh-rCN/strings.xml` 与 `values/strings.xml`。**同一 key 不得重复定义**（历史上踩过 `share`/`close` 重名的坑）。
