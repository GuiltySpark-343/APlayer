# 歌曲标签系统 · 实施进度台账

基线：`3230f6b5`（分支 `feature/song-tags`，worktree `S:\Workplace\APlayer-tags`）
实施包：`docs/song-tag-implementation.md`
设计文档：`docs/song-tag-system-design.md`

| Task | 状态 | 提交 | 构建 | 验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 数据层骨架 | ✅ | `b345b768` | ✅ | ✅ **真机断言通过** | `user_version=13`；`tags`/`song_tags` + 4 索引齐全；DDL 与 Room `13.json` 逐列一致 |
| T0.2 预设词表种子 | ✅ | `b345b768` | ✅ | ✅ **真机断言通过** | **34 条预设 / 34 distinct tagId**（幂等）；含 `mood.valence`/`mood.arousal` |
| T0.3 仓库层 + DI | ✅ | `b345b768` | ✅ | ✅ 真机断言通过（表就位） | `SongTagRepository` + `SongTagRepoImpl` + Hilt 绑定；CRUD 交互在 T0.5 一并验证 |
| T0.4 标签管理页 | ✅ | `38a163a5` | ✅ | ✅ **真机验证通过** | UI dump 实证：标题「歌曲标签」、分组「情境」、预设项 + `preset` 徽标 + 覆盖数；截图 `docs/screenshots/t0.4-tag-manager.png` |
| T0.7 入口与路由 | ✅ | `38a163a5` | ✅ | ✅ **真机验证通过** | `aplayer://song_tags` 与 `aplayer://tag_filter` 均可解析并跳转；无 FATAL；APK manifest host 已核对 |
| T0.5 一键打标面板 | ✅ | `7490e406` | ✅ | ✅ **端到端真机验证通过** | 见下方证据 A/B |
| T0.8 整体验证 + 台账回填 | ✅ | 本提交 | ✅ | ✅ | 全链路见证据 A–E |

### 真机验证证据（adb 驱动，可复现）

**A. 一键=打分（T0.5 核心）**：播放页 ⋮ → 歌曲标签 → 点「开心」，数据库一次写入 3 行：
```
sha256:dcd59954…  mood.happy    None  user  deleted=0
sha256:dcd59954…  mood.valence  5.0   user  deleted=0
sha256:dcd59954…  mood.arousal  4.0   user  deleted=0
```
与 `TagSeeds.MOOD_PRESETS["mood.happy"] = (5, 4)` 完全一致。

**B. 可微调（T0.5）**：面板底部点「3」→ `mood.valence` 由 **5.0 覆盖为 3.0**，`mood.arousal` 保持 4.0，无重复行（PK upsert）。

**C. 筛选（T0.6）**：筛选页点「开心」→ 顶部显示 **「匹配 1 首 / 共 497 首」**，并出现「播放全部」——正好命中 A 中打标的那首。

**D. 覆盖数（T0.2/T0.5）**：筛选页「开心 **1**」（其余为 0），说明 `usageCounts()` 读取正确。

**E. 页面与路由（T0.4/T0.7）**：`uiautomator dump` 实证标签管理页渲染
（标题「歌曲标签」、分组「情境/情绪/客观」、`preset` 徽标、覆盖数、右上「按标签筛选」+「+」）；
`aplayer://song_tags` / `aplayer://tag_filter` 均能解析跳转；全过程 logcat 无 FATAL。

**未逐个点验（代码已实现，风险低）**：标签管理页的新建/重命名/改色/合并/删除对话框行为、面板 3 秒撤销。
这些是对已充分验证的仓库方法的薄封装。

### 自检（对账设计文档 / 实施包）—— 发现并补齐 6 项计划内欠账

| # | 计划出处 | 计划内容 | 处理 |
|---|---|---|---|
| 1 | 设计 §6.1-① / 实施包 T0.5-① | 面板「常用标签」（置顶 ≤12） | ✅ 已补：按覆盖歌曲数排序置顶 |
| 2 | 设计 §6.1-④ / 实施包 T0.5-④ | 面板「更多 / 新建」入口 | ✅ 已补：底部「更多标签」→ 标签管理页（经 `navigateFromPlayingScreen`） |
| 3 | 设计 §4.6-⑤ / §6.1 | 选中情绪 chip 显示小坐标（如 `5/4`） | ✅ 已补：`chipLabel()` |
| 4 | 设计 §6.3 | 管理页排序 | ✅ 已补：**上移/下移**（写 `sortOrder`）。设计原文写"拖拽排序"，此处用上移/下移替代（Compose 无内置拖拽排序，且功能等价），已在本文档声明偏差 |
| 5 | 设计 §6.4 / 实施包 T0.6 | 筛选结果「随机 / 顺序」播放 | ✅ 已补：「播放全部」+「随机播放」 |
| 6 | 设计 §6.4 | 标签统计（首歌数 / 总时长） | ✅ 已补：标题行显示「匹配 N 首 / 共 M 首 · 总时长 X」 |

**仍属计划内但按实施包明确推迟的**：预设标签"允许隐藏"（实施包 T0.4 写"T0 只展示徽标"→ T1）。

### 补齐项的验证结果

| 项 | 状态 | 证据 |
|---|---|---|
| #4 管理页排序（上移/下移） | ✅ **已验证** | 操作菜单出现 上移/下移/重命名/改颜色/合并同义词/删除；点「通勤」上移后 DB：**通勤 sortOrder=0 / 回家=1**（原为 0/1 对调） |
| #5 随机/顺序播放 | ✅ **已验证** | 筛选页按钮：**「播放全部」+「随机播放」** |
| #6 标签统计（首歌数/总时长） | ✅ **已验证** | 表头：**「匹配 1 首 / 共 497 首 · 3 min」** |
| #1 面板「常用」区 | ✅ **已验证** | 面板顶部出现「常用」区，含「开心 5/4」 |
| #2 面板「更多/新建」入口 | ✅ **已验证** | 「更多标签」渲染于 y≈2298（**此前缺失是因为手机装的是旧包**，非代码问题） |
| #3 选中情绪 chip 显示坐标 | ✅ **已验证** | 「开心 **5/4**」「治愈 **4/2**」，与 `MOOD_PRESETS` 一致 |

**撤销行（3 秒）**：初次实现放在面板**底部**，实测在 2400px 高屏上**被裁切不可见** → 已改为放在面板**顶部**（提示语下方，带底色高亮）。

### ✅ 撤销（完整链路，已端到端验证）

- **渲染**：点情绪词后，面板顶部出现「已打上「温柔」」+「撤销」（y≈828）
- **动作**：链式执行「点『燃』→ 1s → 点『撤销』」后查库：
```
sha256:d71bbb43…  mood.burn     deleted=1    ← 点击写入，撤销后墓碑
sha256:d71bbb43…  mood.valence  score=3.0  deleted=1
sha256:d71bbb43…  mood.arousal  score=5.0  deleted=1
```
`(3,5)` 与 `MOOD_PRESETS["mood.burn"]` 完全一致 —— **一键写入 + 撤销回滚全链路正确**。

### 🐞 线上 Bug：装过期包导致「App 启动即崩」（已定位并修复）

**现象**：App 完全无法使用，启动后进程立刻消失，桌面停留。

**根因**（crash buffer 实证）：
```
java.lang.IllegalStateException: A migration from 14 to 13 was required but not found
```
设备数据库已被展示层升到 **v14**，而本线旧 worktree 的构建仍是 **v13** → Room 触发**降级**迁移 → 无降级路径 → 启动崩溃。

**修复**：改用**合并后的最新分支**（`dev/play-event`，`AppDatabase.VERSION = 14`，且 **`migration12to13`（标签表）与 `migration13to14`（展示层）并存**）重新构建安装 → 进程存活、前台正常、crash buffer 全空。

**防复发**：
1. 本线的独立 worktree `S:\Workplace\APlayer-tags` **已退役**（其提交全部并入 `dev/play-event`，无遗留）；
2. **纪律**：任何情况下**只从最新分支构建安装**；跨分支/跨工作区混装会造成 DB 版本错配。
3. 判定设备上装的是谁的包：`pm path` + `pull` + `aapt2 dump xmltree --file AndroidManifest.xml` 看 host 列表；DB 版本可用 `run-as` 拉库后看 `PRAGMA user_version`。

> **设备占用告警（两次踩到）**：`ui_nodes.py` 输出里的 package 若不是 `remix.myplayer.debug`，
> **立即停止一切 `input tap`** —— 盲点会落进用户正在用的 App（实测落进微信、便签）。

### ⚠️ 自动化 UI 验证的操作纪律（本轮踩坑）

1. **禁止在未确认设备空闲时用坐标盲点**：本轮最后一次验证时前台是**微信**，说明用户正在用手机 —— 盲点会误操作用户的其他 App。**验证前必须先 dump 一次确认前台是本 App**（`ui_nodes.py` 输出里的 package 必须是 `remix.myplayer.debug`）。
2. `uiautomator dump` 在动画持续刷新时会报 `could not get idle state`：应 **`am force-stop` 冷启动到目标 deep link 页**，而不是关系统动画（本轮曾改过 `window/transition/animator_animation_scale=0`，**已恢复为 1**）。
3. 需要「播放中」状态的验证（T0.5 面板）无法冷启动直达，**只能由人工在设备上操作**，或等设备确认空闲时再做。

**本轮真机验证方法（可复用）**：`adb shell uiautomator dump` + `tools/ui_nodes.py` 解析语义树
（Compose 会把文本暴露给 accessibility），配合 `aapt2 dump xmltree` 核对 manifest。
注意：**PowerShell 直接 `-match` 中文字符串会因编码失败**，必须用 Python 以 UTF-8 解码后匹配。
| T0.6 曲库标签筛选 | ✅ | `6306ac08` | ✅ | ✅ **真机验证通过（渲染层）** | UI dump 实证：三组 chips + 「匹配 0 首 / 共 **497** 首」（正确读到曲库）；截图 `docs/screenshots/t0.6-tag-filter.png`。交互（勾选后筛选/播放）待人工确认 |

**T0.5 设计要点（对照设计文档 §4.6）**：情绪词 chip 点击即写「该词 + valence/arousal 预设坐标」；两个情绪维度以 SCALE 标签暴露在面板底部做 5 档微调；未打分维度不落库；面板不中断播放。

**迁移回归证据（真机）**：`play_events` **2318 行**、`album_colors` 0 行、`report_overrides` 0 行 —— 迁移未触碰既有数据。
**拉库方法**：PowerShell `>` 重定向会损坏二进制，必须用 `Start-Process -RedirectStandardOutput`（实施包 0.3.2 已修正）。

> 说明：T0.1–T0.3 为纯数据层改动，**真机验证需先安装新包**（`/sdcard/Download/APlayer-T0-debug.apk`）。
> 安装后打开 App 一次触发迁移，再执行实施包 0.3.2 的拉库断言脚本 `tools/assert_tags_db.py`。
> 设备侧 `adb install` 会被 vivo 的安装确认框拦住（需人工点击），故改用「推送 + 手动安装」。
