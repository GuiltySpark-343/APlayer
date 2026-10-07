package remix.myplayer.ui.component.report

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/**
 * 会滚动到目标值的大数字。
 *
 * 动画时长固定 800ms；[format] 负责把当前动画值转成字符串，
 * 组件本身不猜测业务格式（时长、次数、百分比各自传自己的格式）。
 */
@Composable
fun HeroNumber(
  target: Float,
  format: (Float) -> String,
  modifier: Modifier = Modifier,
  suffix: String = "",
  durationMs: Int = 800
) {
  val tokens = LocalReportTokens.current
  val anim = remember { Animatable(0f) }
  LaunchedEffect(target) {
    anim.snapTo(0f)
    anim.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing))
  }
  TextPrimary(
    text = format(anim.value) + suffix,
    modifier = modifier,
    fontSize = tokens.heroSize,
    fontWeight = FontWeight.Bold,
    color = tokens.accent
  )
}
