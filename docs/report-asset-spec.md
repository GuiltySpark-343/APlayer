# 年度报告视觉资产规格（P3-7）

> 本文件是**硬约定**：出图、命名、接入全部按此执行，执行时不得临时更改。
> 上游设计：`docs/annual-report-presentation-design.md` §5
> 实施步骤：`docs/annual-report-presentation-implementation.md` T7.x

---

## 1. 总原则

1. **底图不含任何文字、数字、UI、边框、logo、签名。** 文字层永远由代码绘制。
2. **底图只出灰度/单色**，运行时由代码按 `ReportTokens` 染色（一份资产适配所有用户配色）。
3. **只在开发期离线产出**，绝不在运行时于手机生成。
4. 底图数量 **4–6 张**，靠参数化变换（旋转/缩放/裁切/翻转/噪点/染色）覆盖全部页面。

---

## 2. 画布与安全区

| 项 | 规定 |
| --- | --- |
| 画布 | 1620 × 2430 px（3:4.5，与手机竖屏同比例；海报仍为 1080×1620，两者按比例对应） |
| 生成分辨率 | 1024×1536 + hi-res fix ×1.5（等效 1536×2304），最终缩放至 1620×2430 |
| 上安全区 | y ∈ [0, 570] px **禁止放主体**（放年份、关键词、文案） |
| 下安全区 | y ∈ [1860, 2430] px **禁止放主体**（放页脚、进度点） |
| 中区 | y ∈ [570, 1860] px 为主体可占区域，但需保证任意 8×8 采样区块的平均亮度可控（见 scrim 规则） |
| 色彩空间 | sRGB |
| 边缘 | 不画硬边框；画面自然出边，避免"贴纸感" |

---

## 3. 格式、体积与命名

| 项 | 规定 |
| --- | --- |
| 格式 | WebP，quality 80（有 alpha 需求时才用 lossless） |
| 单张体积 | ≤ 300 KB |
| 总体积 | ≤ 4 MB（4–6 张合计） |
| 目录 | `app/src/main/res/drawable-nodpi/`（禁止放进 `drawable-*dpi`，避免多密度冗余） |
| 命名 | `report_bg_<slot>_<variant>.webp` |
| slot 取值 | `cover` \| `overview` \| `media` \| `chart` \| `rank` \| `night` |
| variant 取值 | `a` \| `b` \| `c`（同一 slot 的备选，最终只保留选中项） |

示例：`report_bg_night_a.webp`、`report_bg_chart_b.webp`。

**slot 与页面的对应关系**

| slot | 用于页面 |
| --- | --- |
| `cover` | S0 开场 |
| `overview` | S1 总览数字 |
| `media` | S2 音乐颜色、S6 深夜党 |
| `chart` | S5 时段、S7 年历、S10 来源 |
| `rank` | S4 年度之最、S8 循环王、S9 探索/重复 |
| `night` | S6 深夜党（暗色专用） |

---

## 4. 灰度要求（染色前提）

1. 出图后**转为灰度**再落库：保留明度结构，丢弃色相；
2. 灰度直方图检查：**中位明度 ∈ [0.25, 0.55]**，避免整体过亮（白字压不住）或过暗（染色后看不出层次）；
3. 峰值亮度 ≤ 0.92（避免纯白死区），最低亮度 ≥ 0.06（避免纯黑糊成一块）；
4. 转换命令（ImageMagick）：

```powershell
magick convert in.png -colorspace Gray -resize 1620x2430^ -gravity center -extent 1620x2430 out_gray.png
```

---

## 5. 接入契约

1. 页面用 `ThemedBackground(resId = R.drawable.report_bg_<slot>_<variant>)` 包裹内容（见实施包 T7.4）；
2. 染色：`ColorFilter.tint(tokens.accentSoft, BlendMode.Color)`；
3. 压暗：叠一层 `tokens.bgTop.copy(alpha = 0.55f)`；
4. scrim：按 `Scrim.kt` 的 `scrimAlphaFor()` 自动计算，**不得手工写死 alpha**；
5. 底图加载失败（资源缺失）时必须退回纯渐变背景，不允许出现空白页。

---

## 6. 验收清单（每张图都要过）

- [ ] 无任何可读字符
- [ ] 上下安全区内无主体
- [ ] 已转灰度，中位明度在 [0.25, 0.55]
- [ ] WebP q80，≤300 KB
- [ ] 命名符合 `report_bg_<slot>_<variant>.webp`
- [ ] 放进 `drawable-nodpi/`
- [ ] 在报告页实机看过：白字仍可读（scrim 生效）
- [ ] 换一个用户主色后仍协调（染色生效）
