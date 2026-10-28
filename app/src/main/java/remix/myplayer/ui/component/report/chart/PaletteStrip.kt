package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.component.report.reportTween

/**
 * 专辑封面主色色板：等宽色块，按顺序依次填充。
 *
 * 不显示十六进制值：9 个色块时每列只有约 110px，`#RRGGBB` 会被压成 `#…`，信息量为零。
 */
@Composable
fun PaletteStrip(
  colors: List<Int>,
  modifier: Modifier = Modifier,
  height: Dp = 72.dp
) {
  if (colors.isEmpty()) return

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = reportTween(600),
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
  }
}
