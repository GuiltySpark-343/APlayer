package remix.myplayer.ui.component.report

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * 系统"移除动画"（开发者选项里动画缩放设为关闭）时为 true，所有报告动效直接到终态。
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** 页内元素的入场顺序索引，默认 0（第一个元素）。 */
val LocalStaggerIndex = staticCompositionLocalOf { 0 }

/**
 * 统一的动画时长入口：尊重系统的"移除动画"。
 * 组件里不要直接写 tween(...)，否则关掉动画后仍会播。
 */
@Composable
fun reportTween(durationMs: Int): AnimationSpec<Float> =
  if (LocalReduceMotion.current) snap() else tween(durationMs)

/**
 * 逐段淡入：第 [index] 个元素延后 80ms × index 出现。
 * 只有真正需要分段的页面才需要包这一层。
 */
@Composable
fun FadeInStaggered(
  index: Int = LocalStaggerIndex.current,
  content: @Composable () -> Unit
) {
  val reduceMotion = LocalReduceMotion.current
  var visible by remember { mutableStateOf(reduceMotion) }

  LaunchedEffect(index, reduceMotion) {
    if (reduceMotion) {
      visible = true
    } else {
      delay(80L * index)
      visible = true
    }
  }

  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = if (reduceMotion) snap() else tween(400),
    label = "stagger$index"
  )

  Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { content() }
}
