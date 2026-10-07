"""程序化生成报告底图（设计文档里的"方案 A"）。

规格见 docs/report-asset-spec.md：
  - 1620x2430，灰度、无文字
  - 文字安全区（上 0-570 / 下 1860-2430）平均亮度 <= 0.20
  - 主体区中位明度 ∈ [0.25, 0.55]
  - 全图峰值 <= 0.92、最低 >= 0.06
  - WebP q80，<= 300KB

产物与 AI 出图**同名**，将来用扩散模型出图后直接覆盖文件即可，代码零改动。
固定随机种子，保证可复现。

注意：PIL 的 GaussianBlur 不支持 'F'(float) 模式，模糊一律走 `blur01()` 的 L 模式。
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

W, H = 1620, 2430
TOP_BAND = (0, 570)
BOTTOM_BAND = (1860, 2430)
SUBJECT = (570, 1860)

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
PREVIEW_DIR = os.path.join(ROOT, "docs", "samples")

SEED = 20261007


def blur01(arr01: np.ndarray, radius: float) -> np.ndarray:
    """对 0..1 的灰度数组做高斯模糊（经 L 模式，因为 F 模式不支持 filter）。"""
    img = Image.fromarray((np.clip(arr01, 0.0, 1.0) * 255.0).round().astype(np.uint8), mode="L")
    out = np.asarray(img.filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0
    return out


def vertical_gradient(top: float, bottom: float) -> np.ndarray:
    ramp = np.linspace(top, bottom, H, dtype=np.float32)
    return np.repeat(ramp[:, None], W, axis=1)


def radial_glow(cx: float, cy: float, radius: float, peak: float) -> np.ndarray:
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    d = np.sqrt((xs - cx * W) ** 2 + (ys - cy * H) ** 2)
    return (peak * np.exp(-(d ** 2) / (2 * radius ** 2))).astype(np.float32)


def noise(rng: np.random.Generator, amount: float, radius: float = 0.0) -> np.ndarray:
    n = rng.normal(0.0, amount, size=(H, W)).astype(np.float32)
    if radius > 0:
        n = blur01(n * 0.5 + 0.5, radius) * 2.0 - 1.0
    return n


def darkening_envelope() -> np.ndarray:
    """把上下文字安全区压暗，并在 190px 内平滑过渡，避免出现可见的压暗带。"""
    ys = np.arange(H, dtype=np.float32)
    env = np.ones(H, dtype=np.float32)
    band, ramp = 0.28, 190.0

    top_end = TOP_BAND[1]
    env[ys < top_end] = band
    m = (ys >= top_end) & (ys < top_end + ramp)
    env[m] = band + (1.0 - band) * (ys[m] - top_end) / ramp

    bottom_start = BOTTOM_BAND[0]
    m2 = (ys >= bottom_start - ramp) & (ys < bottom_start)
    env[m2] = 1.0 - (1.0 - band) * (ys[m2] - (bottom_start - ramp)) / ramp
    env[ys >= bottom_start] = band
    return env[:, None]


def new_layer() -> tuple:
    layer = Image.new("L", (W, H), 0)
    return layer, ImageDraw.Draw(layer)


def layer_to01(layer: Image.Image, radius: float = 0.0) -> np.ndarray:
    if radius > 0:
        layer = layer.filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(layer, dtype=np.float32) / 255.0


# ---------- 六个槽位的画面 ----------

def art_cover(rng):
    base = vertical_gradient(0.10, 0.58)
    stars = np.zeros((H, W), dtype=np.float32)
    for _ in range(220):
        x, y = int(rng.integers(0, W)), int(rng.integers(280, 1750))
        r = int(rng.integers(1, 3))
        stars[max(0, y - r):y + r, max(0, x - r):x + r] = float(rng.uniform(0.5, 0.9))
    return base + blur01(stars, 1.2) + noise(rng, 0.012, radius=1.5)


def art_overview(rng):
    base = vertical_gradient(0.12, 0.30)
    return base + radial_glow(0.5, 0.46, 300, 0.62) + noise(rng, 0.010, radius=2.0)


def art_media(rng):
    base = vertical_gradient(0.14, 0.34)
    layer, d = new_layer()
    for _ in range(6):
        cx, cy = int(rng.integers(150, W - 150)), int(rng.integers(700, 1750))
        r = int(rng.integers(180, 420))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=int(rng.uniform(0.25, 0.6) * 255))
    return base + layer_to01(layer, 110) + noise(rng, 0.010, radius=2.0)


def art_chart(rng):
    base = vertical_gradient(0.16, 0.36)
    layer, d = new_layer()
    y, step = 640, 74
    while y < 1820:
        d.line([(0, y), (W, y)], fill=int(0.34 * 255), width=2)
        y += step
        step = int(step * 1.06)
    for x in range(0, W + 1, 162):
        d.line([(x, 600), (x, 1840)], fill=int(0.16 * 255), width=2)
    return base + layer_to01(layer, 1.6) + noise(rng, 0.010, radius=2.0)


def art_rank(rng):
    base = vertical_gradient(0.13, 0.32)
    layer, d = new_layer()
    for i in range(4):
        top = 720 + i * 250
        left = 180 + i * 30
        d.rounded_rectangle([left, top, W - left, top + 190], radius=28,
                            fill=int((0.22 + 0.06 * (3 - i)) * 255))
        d.rounded_rectangle([left, top, W - left, top + 10], radius=5, fill=int(0.55 * 255))
    return base + layer_to01(layer, 3.0) + noise(rng, 0.010, radius=2.0)


def art_night(rng):
    # 高光别太亮：超过 0.92 会被 clip 成死白椭圆，很难看
    base = vertical_gradient(0.07, 0.20) + radial_glow(0.78, 0.24, 260, 0.32)
    haze = blur01(vertical_gradient(0.06, 0.0), 60)
    return base + haze + noise(rng, 0.008, radius=3.0)


ART = {
    "cover": art_cover,
    "overview": art_overview,
    "media": art_media,
    "chart": art_chart,
    "rank": art_rank,
    "night": art_night,
}


def finalize(art: np.ndarray) -> np.ndarray:
    art = np.clip(art, 0.0, 1.0) * darkening_envelope()
    art = art + 0.06  # 保底：最低亮度 >= 0.06
    return np.clip(art, 0.0, 0.92)


def band_stats(a: np.ndarray, y0: int, y1: int) -> dict:
    seg = a[y0:y1]
    return {"mean": float(seg.mean()), "median": float(np.median(seg)),
            "max": float(seg.max())}


def build(slot: str) -> np.ndarray:
    rng = np.random.default_rng(SEED + (abs(hash(slot)) % 1000))
    art = finalize(ART[slot](rng))

    # 注意顺序：先调主体区明度，**最后**再压文字安全区。
    # 反过来会让整体缩放把安全区重新顶回 0.20 以上（早期版本踩过）。
    med = float(np.median(art[SUBJECT[0]:SUBJECT[1]]))
    if med < 0.25:
        art = art * min(0.45 / max(med, 1e-3), 1.6)
    elif med > 0.55:
        art = art * (0.45 / med)
    art = np.clip(art, 0.0, 0.92)

    # 文字安全区平均亮度必须 <= 0.20（最后一步，且不再做任何全局缩放）
    for y0, y1 in (TOP_BAND, BOTTOM_BAND):
        mean = float(art[y0:y1].mean())
        if mean > 0.20:
            art[y0:y1] *= 0.20 / mean

    # 收尾加一点抖动（约 1 LSB）：包络斜坡每像素变化正好 ~1/255，
    # 不抖动会出现明显色带；抖动必须在包络之后加，否则会被一起压小。
    art = art + rng.normal(0.0, 0.004, size=art.shape).astype(np.float32)
    return np.clip(art, 0.0, 0.92)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    ok = True
    for slot in ART:
        arr = build(slot)
        name = f"report_bg_{slot}_a.webp"
        path = os.path.join(OUT_DIR, name)
        gray = Image.fromarray((arr * 255.0).round().astype(np.uint8), mode="L")
        gray.convert("RGB").save(path, "WEBP", quality=80, method=6)

        top = band_stats(arr, *TOP_BAND)
        bottom = band_stats(arr, *BOTTOM_BAND)
        subject = band_stats(arr, *SUBJECT)
        size_kb = os.path.getsize(path) / 1024.0

        checks = [
            ("top<=0.20", top["mean"] <= 0.201),
            ("bottom<=0.20", bottom["mean"] <= 0.201),
            ("subj_median", 0.245 <= subject["median"] <= 0.555),
            ("peak<=0.92", max(top["max"], bottom["max"], subject["max"]) <= 0.921),
            ("min>=0.055", float(arr.min()) >= 0.055),
            ("size<=300KB", size_kb <= 300),
        ]
        bad = [n for n, passed in checks if not passed]
        ok = ok and not bad
        print(f"{name:30} top={top['mean']:.3f} bot={bottom['mean']:.3f} "
              f"subj_med={subject['median']:.3f} min={arr.min():.3f} max={arr.max():.3f} "
              f"{size_kb:6.1f}KB  {'OK' if not bad else 'FAIL:' + ','.join(bad)}")

        if slot in ("cover", "night", "rank"):
            gray.save(os.path.join(PREVIEW_DIR, f"bg-preview-{slot}.png"))
    print("RESULT:", "ALL OK" if ok else "SOME FAILED")


if __name__ == "__main__":
    main()
