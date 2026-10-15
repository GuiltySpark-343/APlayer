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
| T0.5 一键打标面板 | 🟡 代码完成 | `7490e406` | ✅ 26s | ⏳ **仅差人工交互验证** | 入口在播放页 ⋮ → 歌曲标签（需先播放一首歌，adb 无法可靠模拟） |

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
