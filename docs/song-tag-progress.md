# 歌曲标签系统 · 实施进度台账

基线：`3230f6b5`（分支 `feature/song-tags`，worktree `S:\Workplace\APlayer-tags`）
实施包：`docs/song-tag-implementation.md`
设计文档：`docs/song-tag-system-design.md`

| Task | 状态 | 提交 | 构建 | 验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 数据层骨架 | ✅ | `b345b768` | ✅ | ✅ **真机断言通过** | `user_version=13`；`tags`/`song_tags` + 4 索引齐全；DDL 与 Room `13.json` 逐列一致 |
| T0.2 预设词表种子 | ✅ | `b345b768` | ✅ | ✅ **真机断言通过** | **34 条预设 / 34 distinct tagId**（幂等）；含 `mood.valence`/`mood.arousal` |
| T0.3 仓库层 + DI | ✅ | `b345b768` | ✅ | ✅ 真机断言通过（表就位） | `SongTagRepository` + `SongTagRepoImpl` + Hilt 绑定；CRUD 交互在 T0.5 一并验证 |
| T0.4 标签管理页 | 🟡 代码完成 | 见下 | ✅ 35s | ⏳ 真机截图待做 | 分组列表/新建/改名/改色/合并/删除 |
| T0.7 入口与路由 | 🟡 代码完成 | 见下 | ✅ 28s | ⏳ deep link 待验证 | 最小 patch：AppNav/Manifest/ComposeActivity/SettingScreen |

**迁移回归证据（真机）**：`play_events` **2318 行**、`album_colors` 0 行、`report_overrides` 0 行 —— 迁移未触碰既有数据。
**拉库方法**：PowerShell `>` 重定向会损坏二进制，必须用 `Start-Process -RedirectStandardOutput`（实施包 0.3.2 已修正）。

> 说明：T0.1–T0.3 为纯数据层改动，**真机验证需先安装新包**（`/sdcard/Download/APlayer-T0-debug.apk`）。
> 安装后打开 App 一次触发迁移，再执行实施包 0.3.2 的拉库断言脚本 `tools/assert_tags_db.py`。
> 设备侧 `adb install` 会被 vivo 的安装确认框拦住（需人工点击），故改用「推送 + 手动安装」。
