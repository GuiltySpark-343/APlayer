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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.data.db.room.dao.MonthCount
import remix.myplayer.ui.component.report.reportTween
import remix.myplayer.ui.theme.report.LocalReportTokens

/**
 * 12 个月收听时长折线：带网格线、渐变填充、峰值标注，按 PathMeasure 生长。
 */
@Composable
fun TrendLineChart(
  title: String,
  months: List<MonthCount>,
  modifier: Modifier = Modifier,
  height: Dp = 150.dp
) {
  val tokens = LocalReportTokens.current
  if (months.isEmpty()) return

  val byMonth = HashMap<Int, Long>()
  months.forEach { byMonth[it.month] = it.listenedMs }
  val values = (1..12).map { byMonth[it] ?: 0L }
  val maxMs = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = reportTween(600),
    label = "trendLine"
  )
  val measurer = rememberTextMeasurer()

  ChartFrame(title = title, modifier = modifier, height = height) {
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
      val labelPad = 18.dp.toPx()
      val left = 0f
      val right = size.width
      val top = labelPad
      val bottom = size.height - labelPad
      val stepX = (right - left) / 11f

      // 4 条水平网格线
      repeat(5) { index ->
        val y = top + (bottom - top) * index / 4f
        drawLine(
          color = tokens.textSecondary.copy(alpha = 0.15f),
          start = Offset(left, y),
          end = Offset(right, y),
          strokeWidth = 1.dp.toPx()
        )
      }

      val points = values.mapIndexed { index, ms ->
        val ratio = ms.toFloat() / maxMs
        Offset(left + stepX * index, bottom - (bottom - top) * ratio)
      }

      val line = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
      }
      val fill = Path().apply {
        addPath(line)
        lineTo(points.last().x, bottom)
        lineTo(points.first().x, bottom)
        close()
      }

      val lineMeasure = PathMeasure().apply { setPath(line, false) }
      val fillMeasure = PathMeasure().apply { setPath(fill, false) }

      val partialFill = Path()
      fillMeasure.getSegment(0f, fillMeasure.length * progress, partialFill, true)
      drawPath(partialFill, tokens.accent.copy(alpha = 0.18f))

      val partialLine = Path()
      lineMeasure.getSegment(0f, lineMeasure.length * progress, partialLine, true)
      drawPath(
        partialLine,
        tokens.accent,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
      )

      // 峰值点标注
      val peakIndex = values.indices.maxByOrNull { values[it] } ?: 0
      if (progress > 0.99f) {
        val peak = points[peakIndex]
        drawCircle(color = tokens.accent, radius = 4.dp.toPx(), center = peak)
        val layout = measurer.measure(
          AnnotatedString("%d 月".format(peakIndex + 1)),
          style = TextStyle(color = tokens.textSecondary, fontSize = tokens.captionSize)
        )
        drawText(
          textLayoutResult = layout,
          topLeft = Offset(
            (peak.x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
            top - labelPad
          )
        )
      }

      // X 轴刻度 1/4/7/10
      listOf(1, 4, 7, 10).forEach { month ->
        val x = left + stepX * (month - 1)
        val layout = measurer.measure(
          AnnotatedString(month.toString()),
          style = TextStyle(color = tokens.textSecondary, fontSize = tokens.captionSize)
        )
        drawText(
          textLayoutResult = layout,
          topLeft = Offset((x - layout.size.width / 2f).coerceIn(0f, size.width), bottom + 2.dp.toPx())
        )
      }
    }
  }
}
