package remix.myplayer.ui.component.report

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import remix.myplayer.ui.theme.report.LocalReportTokens

/** 上下文字安全区占画布的比例（与 docs/report-asset-spec.md 的 570/2430、1860/2430 对齐）。 */
private const val TEXT_BAND_FRACTION = 0.235f

/** scrim 只需要 8×8 的亮度统计，采样用 4 倍降采样的小图即可，避免整张大图常驻内存。 */
private const val SCRIM_SAMPLE_IN_SAMPLE_SIZE = 4

/**
 * 报告页底图：灰度底图 **按主色染色** + 压暗 + 上下文字区自动 scrim。
 *
 * 底图必须是灰度的（见资产规格），否则染色无从谈起；scrim 走 [scrimAlphaFor] 反解，
 * 不要改成手写 alpha。
 */
@Composable
fun ThemedBackground(
  @DrawableRes resId: Int,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  val context = LocalContext.current
  val scrim = remember(resId) { scrimAlphas(context, resId) }

  Box(modifier = modifier.fillMaxSize()) {
    Image(
      painter = painterResource(resId),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      colorFilter = ColorFilter.tint(tokens.accentSoft, BlendMode.Color),
      modifier = Modifier.fillMaxSize()
    )

    // 整体压暗，保证主体区不会盖过文字
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(tokens.bgTop.copy(alpha = 0.55f))
    )

    // 上下文字区 scrim：用渐变过渡，避免出现可见的压暗带
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(TEXT_BAND_FRACTION)
          .background(
            Brush.verticalGradient(
              listOf(Color.Black.copy(alpha = scrim.first), Color.Transparent)
            )
          )
      )
      Spacer(modifier = Modifier.weight(1f - 2f * TEXT_BAND_FRACTION))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(TEXT_BAND_FRACTION)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color.Black.copy(alpha = scrim.second))
            )
          )
      )
    }

    content()
  }
}

/** 计算上下文字区各自需要的 scrim 强度（上、下）。解码失败时退回最小值。 */
private fun scrimAlphas(context: Context, @DrawableRes resId: Int): Pair<Float, Float> {
  val options = BitmapFactory.Options().apply { inSampleSize = SCRIM_SAMPLE_IN_SAMPLE_SIZE }
  val bitmap = runCatching {
    BitmapFactory.decodeResource(context.resources, resId, options)
  }.getOrNull() ?: return 0.05f to 0.05f

  val image = bitmap.asImageBitmap()
  val width = image.width.toFloat()
  val height = image.height.toFloat()
  val top = scrimAlphaFor(image, Rect(0f, 0f, width, height * TEXT_BAND_FRACTION))
  val bottom = scrimAlphaFor(
    image,
    Rect(0f, height * (1f - TEXT_BAND_FRACTION), width, height)
  )
  bitmap.recycle()
  return top to bottom
}
