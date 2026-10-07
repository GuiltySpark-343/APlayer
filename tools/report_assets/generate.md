# 报告底图出图手册（P3-7 / T7.3）

> 目标：从零产出一批**无文字、灰度、可染色**的背景底图，规格见 `docs/report-asset-spec.md`。
> 本机硬件：**NVIDIA RTX 4060 Ti 16GB**，驱动 610.88（`nvidia-smi` 可用）。
> 本机现状：**尚未安装 ComfyUI**；python 为 anaconda 3.11.5，**未安装 torch**。因此第 1 节是从零搭建。

---

## 1. 环境搭建（一次性）

### 1.1 建独立 conda 环境

```powershell
conda create -n comfy python=3.11 -y
conda activate comfy
```

### 1.2 装 PyTorch（CUDA 版）

到 https://pytorch.org/get-started/locally/ 取**当前**的 CUDA 12.x 稳定版命令，不要抄本文档里的版本号（会过期）。装完必须验证：

```powershell
python -c "import torch; print(torch.__version__, torch.cuda.is_available(), torch.cuda.get_device_name(0))"
```

**期望输出**：版本号 + `True` + `NVIDIA GeForce RTX 4060 Ti`。
若 `cuda.is_available()` 为 `False`，**停下来修**，不要继续——后面全部白做。

### 1.3 装 ComfyUI

```powershell
cd D:\
git clone https://github.com/comfyanonymous/ComfyUI.git
cd ComfyUI
pip install -r requirements.txt
```

启动验证：

```powershell
python main.py --listen 127.0.0.1 --port 8188
```

浏览器打开 `http://127.0.0.1:8188` 能看到界面即成功。**保持这个进程运行**再继续。

### 1.4 显存策略（16GB 的关键设置）

| 场景 | 启动参数 | 说明 |
| --- | --- | --- |
| Flux.1-dev fp8 | `python main.py --lowvram` | 16GB 可跑 1024×1536；若 OOM 改 `--novram` 或降分辨率 |
| SDXL | 默认即可 | 有富余，适合快速迭代构图 |

**首选 Flux.1-dev fp8**（画面结构更稳），**构图试错阶段用 SDXL**（快）。

---

## 2. 模型文件（放到 `ComfyUI/models/`）

> 具体文件名与下载地址**以 ComfyUI 官方文档当前页面为准**（会变），下面给出的是所需**角色**与放置目录。
> 下载后必须用第 1.3 节的界面加载一次对应 workflow，确认模型能被识别，再继续。

| 角色 | 放置目录 | 用途 |
| --- | --- | --- |
| Flux.1-dev fp8 主模型（或 SDXL base） | `models/checkpoints/` | 出图主体 |
| Flux 用文本编码器（T5 + CLIP-L）与 VAE | `models/text_encoders/`、`models/vae/` | 仅在用**拆分版** Flux 时需要；用 **all-in-one fp8 单文件**可跳过 |
| 可选：放大模型 | `models/upscale_models/` | hi-res fix |

推荐走 **all-in-one fp8 单文件**路线，省掉编码器/VAE 的版本匹配问题。

---

## 3. 出图参数（锁定，不要自由发挥）

### 3.1 负面提示词（固定）

```
text, letters, numbers, watermark, logo, signature, ui, frame, border, people, faces, hands
```

### 3.2 风格前缀（固定，保证 6 张图同一画风）

```
abstract dark ambient background, soft volumetric light, subtle grain, cinematic depth,
low saturation, large empty space at top and bottom, no subject in the upper third
```

### 3.3 每张图只改 subject 与 seed

| slot | subject 追加 |
| --- | --- |
| `cover` | `night sky gradient, distant stars, deep calm` |
| `overview` | `single floating luminous orb, soft halo, minimal` |
| `media` | `soft color fields, blurred bokeh light spots` |
| `chart` | `geometric grid of faint lines, subtle perspective` |
| `rank` | `stacked abstract slabs, gentle rim light` |
| `night` | `moonlit haze, deep blue darkness, faint light beam` |

### 3.4 采样参数

| 参数 | 值 |
| --- | --- |
| 分辨率 | 1024×1536 |
| steps | 20（Flux）/ 25（SDXL） |
| cfg | 1.0（Flux）/ 7.0（SDXL） |
| sampler | `euler`（Flux）/ `dpmpp_2m`（SDXL） |
| seed | 固定族：`20261007 + 0..9`，同一 slot 只换 seed 选构图 |
| hi-res fix | ×1.5，denoise 0.35 |

### 3.5 每张出 4 个候选

同一 subject 用 seed `+0/+1/+2/+3` 各出一张，**人工挑 1 张**。挑图标准：上下安全区是否留空、中区是否有可用于叠字的中间调区域。

---

## 4. 后处理（必须做，否则染色会翻车）

```powershell
# 1) 裁切到目标比例并缩放到最终画布
magick convert raw.png -resize 1620x2430^ -gravity center -extent 1620x2430 step1.png

# 2) 转灰度
magick convert step1.png -colorspace Gray step2.png

# 3) 检查灰度中位明度是否落在 [0.25, 0.55]
magick identify -format "%[fx:mean]\n" step2.png

# 4) 转 WebP q80
magick convert step2.png -quality 80 "report_bg_<slot>_<variant>.webp"
```

第 3 步结果不在区间内就**重新出图或调整曲线**，不要靠运行时补偿：

```powershell
# 过亮时压暗
magick convert step2.png -level 0%,85% step2_fixed.png
```

---

## 5. 落库与接入

1. 文件放到 `app/src/main/res/drawable-nodpi/`，命名严格按 `docs/report-asset-spec.md` §3；
2. 在页面用 `ThemedBackground(R.drawable.report_bg_<slot>_<variant>)` 包裹（实施包 T7.4）；
3. 实机验证两件事：白字可读（scrim 生效）、换主色后协调（染色生效）。

---

## 6. 禁止事项

1. **禁止把文字/数字烘进底图**——中文与数字在扩散模型里不可靠，且无法本地化。
2. **禁止运行时生成**：算力、耗电、包体都不允许；底图只能是打进包里的静态资源。
3. **禁止彩色底图**：会破坏"按用户主色染色"的能力。
4. **禁止放进 `drawable-*dpi`**：只放 `drawable-nodpi`。
5. **禁止超过 6 张**：风格一致性与包体都会失控。
