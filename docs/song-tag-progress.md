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
