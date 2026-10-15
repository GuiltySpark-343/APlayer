# 歌曲标签系统 · 实施进度台账

基线：`3230f6b5`（分支 `feature/song-tags`，worktree `S:\Workplace\APlayer-tags`）
实施包：`docs/song-tag-implementation.md`
设计文档：`docs/song-tag-system-design.md`

| Task | 状态 | 提交 | 构建 | 验证 | 备注 |
| --- | --- | --- | --- | --- | --- |
| T0.1 数据层骨架 | 🟡 代码完成 | 见下 | ✅ 36s | ⏳ 真机 DB 断言待做 | 迁移 DDL 与 Room 生成的 `13.json` 逐列一致 |
| T0.2 预设词表种子 | 🟡 代码完成 | 见下 | ✅ 36s | ⏳ 待断言：34 条预设 + 幂等 | 含情绪坐标表 `MOOD_PRESETS` |
| T0.3 仓库层 + DI | 🟡 代码完成 | 见下 | ✅ 36s | ⏳ 待断言：CRUD/墓碑/合并 | `SongTagRepository` + `SongTagRepoImpl` + Hilt 绑定 |

> 说明：T0.1–T0.3 为纯数据层改动，**真机验证需先安装新包**（`/sdcard/Download/APlayer-T0-debug.apk`）。
> 安装后打开 App 一次触发迁移，再执行实施包 0.3.2 的拉库断言脚本 `tools/assert_tags_db.py`。
> 设备侧 `adb install` 会被 vivo 的安装确认框拦住（需人工点击），故改用「推送 + 手动安装」。
