package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextSecondary

/**
 * 环形占比图 + 右侧图例。
 *
 * [slices] 为 (标签, 数值, 颜色 ARGB)；数值总和为 0 时整块不渲染。
 */
@Composable
fun DonutChart(
  title: String,
  slices: List<Triple<String, Long, Int>>,
  modifier: Modifier = Modifier,
  size: Dp = 150.dp
) {
  val tokens = LocalReportTokens.current
  val total = slices.sumOf { it.second }
  if (slices.isEmpty() || total <= 0L) return

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(600),
    label = "donut"
  )
  val measurer = rememberTextMeasurer()

  ChartFrame(title = title, modifier = modifier, height = size) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Canvas(modifier = Modifier.size(size)) {
        val stroke = 18.dp.toPx()
        val inset = stroke / 2f
        val boxSize = Size(this.size.width - stroke, this.size.height - stroke)

        var startAngle = -90f
        slices.forEach { (_, value, argb) ->
          val sweep = 360f * value / total
          // 段间留 1° 缝隙，避免圆角处粘连
          val drawnSweep = ((sweep - 1f) * progress).coerceAtLeast(0f)
          if (drawnSweep > 0f) {
            drawArc(
              color = Color(argb),
              startAngle = startAngle,
              sweepAngle = drawnSweep,
              useCenter = false,
              topLeft = Offset(inset, inset),
              size = boxSize,
              style = Stroke(width = stroke)
            )
          }
          startAngle += sweep * progress
        }

        // 圆心写占比最大的那一项
        val top = slices.maxByOrNull { it.second }
        if (top != null && progress > 0.99f) {
          val percent = (top.second * 100.0 / total).toInt()
          val center = Offset(this.size.width / 2f, this.size.height / 2f)
          drawCircle(
            color = tokens.cardBg,
            radius = (this.size.minDimension / 2f - stroke - 4.dp.toPx()).coerceAtLeast(0f),
            center = center
          )
          val layout = measurer.measure(
            AnnotatedString("$percent%"),
            style = TextStyle(color = tokens.accent, fontSize = tokens.titleSize)
          )
          drawText(
            textLayoutResult = layout,
            topLeft = Offset(
              center.x - layout.size.width / 2f,
              center.y - layout.size.height / 2f
            )
          )
        }
      }

      Spacer(Modifier.width(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slices.forEach { (label, value, argb) ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(argb))
            )
            Spacer(Modifier.width(6.dp))
            TextSecondary(text = label, fontSize = tokens.captionSize)
            Spacer(Modifier.width(6.dp))
            TextSecondary(
              text = "${value * 100 / total}%",
              fontSize = tokens.captionSize
            )
          }
        }
      }
    }
  }
}
