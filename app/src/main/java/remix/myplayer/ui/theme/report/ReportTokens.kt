package remix.myplayer.ui.theme.report

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils

/**
 * 报告展示层的唯一视觉来源：页面与海报共用同一份。
 *
 * 默认值刻意等于海报原本硬编码的配色，保证替换后无视觉回归。
 */
data class ReportTokens(
  val accent: Color,
  val accentSoft: Color,
  val bgTop: Color,
  val bgBottom: Color,
  val cardBg: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textFooter: Color,
  val pagePadding: Dp,
  val cardPadding: Dp,
  val cardGap: Dp,
  val cardRadius: Dp,
  val sectionGap: Dp,
  val titleSize: TextUnit,
  val bodySize: TextUnit,
  val captionSize: TextUnit,
  val heroSize: TextUnit,
  val chartHeight: Dp
)

object ReportTokenDefaults {

  val Dark = ReportTokens(
    accent = Color(0xFF8C9BFF),
    accentSoft = Color(0xFF5C63A8),
    bgTop = Color(0xFF141628),
    bgBottom = Color(0xFF2A2450),
    cardBg = Color(0x22FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB9BCDA),
    textFooter = Color(0xFF7A7E9E),
    pagePadding = 20.dp,
    cardPadding = 16.dp,
    cardGap = 10.dp,
    cardRadius = 16.dp,
    sectionGap = 10.dp,
    titleSize = 16.sp,
    bodySize = 14.sp,
    captionSize = 12.sp,
    heroSize = 56.sp,
    chartHeight = 120.dp
  )

  /**
   * 由专辑封面主色派生整套配色（S2 音乐颜色）。
   *
   * 规则固定：保留色相，饱和度/明度夹到可读区间；背景取同色相深色并向暖侧偏 18°，
   * 保证白字始终可读，同时每份报告看起来都不一样。
   */
  fun fromAccent(seedArgb: Int): ReportTokens {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedArgb, hsl)
    val hue = hsl[0]

    fun at(hueOffset: Float, saturation: Float, lightness: Float): Color =
      Color(
        ColorUtils.HSLToColor(
          floatArrayOf((hue + hueOffset + 360f) % 360f, saturation, lightness)
        )
      )

    return Dark.copy(
      accent = at(0f, 0.62f, 0.68f),
      accentSoft = at(0f, 0.40f, 0.42f),
      bgTop = at(0f, 0.28f, 0.09f),
      bgBottom = at(18f, 0.36f, 0.17f)
    )
  }
}

val LocalReportTokens = staticCompositionLocalOf { ReportTokenDefaults.Dark }
