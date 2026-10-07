package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextSecondary

/**
 * 专辑封面主色色板：等宽色块，按顺序依次填充。
 *
 * [colors] 为空时整块不渲染（调用方负责空态）。
 */
@Composable
fun PaletteStrip(
  colors: List<Int>,
  modifier: Modifier = Modifier,
  height: Dp = 72.dp
) {
  val tokens = LocalReportTokens.current
  if (colors.isEmpty()) return

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(600),
    label = "palette"
  )

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(height),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      colors.forEachIndexed { index, argb ->
        // 第 index 个色块在进度到达后完全显示
        val reveal = ((progress * colors.size) - index).coerceIn(0f, 1f)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(argb).copy(alpha = reveal.coerceAtLeast(0.05f)))
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      colors.forEach { argb ->
        TextSecondary(
          text = "#%06X".format(0xFFFFFF and argb),
          fontSize = tokens.captionSize,
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        )
      }
    }
  }
}
