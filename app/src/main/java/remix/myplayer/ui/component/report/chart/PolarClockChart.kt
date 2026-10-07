package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.data.db.room.dao.HourCount
import remix.myplayer.ui.component.report.reportTween
import remix.myplayer.ui.theme.report.LocalReportTokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * 24 小时极坐标时钟：0 点在正上方，顺时针；扇区半径表示该小时播放量。
 *
 * 0 点角 = -90°（Canvas 的 0° 在 3 点方向，故起始角减 90）。
 */
@Composable
fun PolarClockChart(
  title: String,
  hours: List<HourCount>,
  modifier: Modifier = Modifier,
  height: Dp = 200.dp
) {
  val tokens = LocalReportTokens.current

  val counts = IntArray(24)
  hours.forEach { if (it.hour in 0..23) counts[it.hour] = it.plays }
  val max = counts.maxOrNull() ?: 0
  if (max <= 0) return

  val peakHour = counts.indices.maxByOrNull { counts[it] } ?: 0
  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = reportTween(600),
    label = "polarClock"
  )
  val measurer = rememberTextMeasurer()

  ChartFrame(title = title, modifier = modifier, height = height) {
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val shortest = minOf(size.width, size.height)
      val outer = shortest / 2f * 0.92f
      val inner = shortest / 2f * 0.34f
      val strokeWidth = 10.dp.toPx()

      counts.forEachIndexed { hour, count ->
        if (count <= 0) return@forEachIndexed
        val ratio = count.toFloat() / max
        val radius = inner + (outer - inner) * ratio * progress
        drawArc(
          color = tokens.accent.copy(alpha = 0.35f + 0.65f * ratio),
          startAngle = hour * 15f - 90f,
          sweepAngle = 13.5f,
          useCenter = false,
          topLeft = Offset(cx - radius, cy - radius),
          size = Size(radius * 2f, radius * 2f),
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }

      // 0/6/12/18 四个刻度
      val tickRadius = outer + 14.dp.toPx()
      listOf(0, 6, 12, 18).forEach { hour ->
        val angle = Math.toRadians((hour * 15f - 90f).toDouble())
        val x = cx + (tickRadius * cos(angle)).toFloat()
        val y = cy + (tickRadius * sin(angle)).toFloat()
        val layout = measurer.measure(
          AnnotatedString(hour.toString()),
          style = TextStyle(color = tokens.textSecondary, fontSize = tokens.captionSize)
        )
        drawText(
          textLayoutResult = layout,
          topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
        )
      }

      // 圆心：峰值小时
      val centerText = "$peakHour:00"
      val centerLayout = measurer.measure(
        AnnotatedString(centerText),
        style = TextStyle(color = tokens.accent, fontSize = tokens.titleSize)
      )
      drawText(
        textLayoutResult = centerLayout,
        topLeft = Offset(cx - centerLayout.size.width / 2f, cy - centerLayout.size.height / 2f)
      )
    }
  }
}
