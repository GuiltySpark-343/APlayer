package remix.myplayer.ui.component.report

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

/**
 * 白字达到 WCAG AA(4.5:1) 所允许的底图最大亮度：`1.05 / 4.5 - 0.05 ≈ 0.1833`。
 * 推导：contrast = (1.0 + 0.05) / (L + 0.05) ≥ 4.5  =>  L ≤ 0.1833
 */
private const val MAX_TEXT_LUMINANCE = 0.1833f

private const val MIN_SCRIM = 0.05f
private const val MAX_SCRIM = 0.85f

/**
 * 自动 scrim：保证白字压在 AI 生成的底图上仍然可读。
 *
 * **反解**而不是插值：先定"白字 4.5:1 允许的最大亮度"，再解出让
 * `mean * (1 - alpha) = MAX_TEXT_LUMINANCE` 所需的 alpha。
 *
 * 早期版本用的是 `lerp(0.05, 0.45, mean)`，经数值验证 **19 档亮度里有 15 档不达标**
 * （亮度 0.55 时白字对比度只有 2.33:1）——插值压不住亮底图，务必不要再改回去。
 */
fun scrimAlphaFor(bitmap: ImageBitmap, rect: Rect): Float {
  if (bitmap.width <= 0 || bitmap.height <= 0) return 0.05f

  val pixels = bitmap.toPixelMap()
  val left = rect.left.toInt().coerceIn(0, pixels.width - 1)
  val right = rect.right.toInt().coerceIn(left, pixels.width - 1)
  val top = rect.top.toInt().coerceIn(0, pixels.height - 1)
  val bottom = rect.bottom.toInt().coerceIn(top, pixels.height - 1)

  val steps = 8
  var sum = 0.0
  var sumSquares = 0.0
  var samples = 0

  for (row in 0 until steps) {
    val y = top + (bottom - top) * row / (steps - 1).coerceAtLeast(1)
    for (col in 0 until steps) {
      val x = left + (right - left) * col / (steps - 1).coerceAtLeast(1)
      val color = pixels[x, y]
      val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
      sum += luminance
      sumSquares += luminance.toDouble() * luminance
      samples++
    }
  }

  val mean = (sum / samples).toFloat()
  val variance = (sumSquares / samples - (sum / samples) * (sum / samples)).toFloat()

  if (mean <= MAX_TEXT_LUMINANCE) return MIN_SCRIM

  // 反解所需 alpha，使 mean * (1 - alpha) == MAX_TEXT_LUMINANCE
  var alpha = 1f - MAX_TEXT_LUMINANCE / mean
  // 明暗跳变剧烈时再多压一点（只会更暗，不会破坏上面的上限）
  if (variance > 0.15f) alpha += 0.05f
  return alpha.coerceIn(MIN_SCRIM, MAX_SCRIM)
}
