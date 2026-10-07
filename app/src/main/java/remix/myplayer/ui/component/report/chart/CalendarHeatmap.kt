package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.db.room.dao.DayCount
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextSecondary

private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

/** 色阶 5 档：无数据最浅，超过 75% 峰值最深。 */
private fun levelAlpha(ms: Long, maxMs: Long): Float = when {
  ms <= 0L -> 0.08f
  else -> {
    val ratio = ms.toFloat() / maxMs
    when {
      ratio < 0.25f -> 0.25f
      ratio < 0.5f -> 0.45f
      ratio < 0.75f -> 0.7f
      else -> 1.0f
    }
  }
}

/**
 * 年历热力图：12 行（月）× 31 列（日）。
 *
 * cell 尺寸**必须由可用宽度反推**，不允许写死（否则会冲出画布，海报曾踩过这个坑）。
 */
@Composable
fun CalendarHeatmap(
  title: String,
  days: List<DayCount>,
  modifier: Modifier = Modifier,
  monthLabelWidth: Dp = 28.dp,
  height: Dp = 190.dp
) {
  val tokens = LocalReportTokens.current
  if (days.isEmpty()) return

  val byDay = HashMap<Pair<Int, Int>, Long>()
  days.forEach { byDay[it.month to it.day] = it.listenedMs }
  val maxMs = days.maxOf { it.listenedMs }.coerceAtLeast(1L)

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(600),
    label = "heatmap"
  )
  val measurer = rememberTextMeasurer()

  ChartFrame(
    title = title,
    modifier = modifier,
    height = height,
    legend = { HeatmapLegend() }
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
      val gap = 2.dp.toPx()
      val labelWidth = monthLabelWidth.toPx()
      val cell = (size.width - labelWidth - 30f * gap) / 31f
      if (cell <= 0f) return@Canvas

      val rowPitch = cell + gap
      val gridHeight = 12f * rowPitch - gap
      val top = ((size.height - gridHeight) / 2f).coerceAtLeast(0f)

      for (month in 1..12) {
        val layout = measurer.measure(
          AnnotatedString("%02d".format(month)),
          style = TextStyle(color = tokens.textSecondary, fontSize = tokens.captionSize)
        )
        val y = top + (month - 1) * rowPitch + (cell - layout.size.height) / 2f
        drawText(textLayoutResult = layout, topLeft = Offset(0f, y.coerceAtLeast(0f)))
      }

      for (month in 1..12) {
        val rowReveal = ((progress * 12f) - (month - 1)).coerceIn(0f, 1f)
        if (rowReveal <= 0f) continue
        for (day in 1..DAYS_IN_MONTH[month - 1]) {
          val ms = byDay[month to day] ?: 0L
          val left = labelWidth + (day - 1) * rowPitch
          val cellTop = top + (month - 1) * rowPitch
          drawRect(
            color = tokens.accent.copy(alpha = levelAlpha(ms, maxMs) * rowReveal),
            topLeft = Offset(left, cellTop),
            size = Size(cell, cell)
          )
        }
      }
    }
  }
}

@Composable
private fun HeatmapLegend() {
  val tokens = LocalReportTokens.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    TextSecondary(
      text = stringResource(R.string.chart_legend_less),
      fontSize = tokens.captionSize
    )
    listOf(0.08f, 0.25f, 0.45f, 0.7f, 1.0f).forEach { alpha ->
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(tokens.accent.copy(alpha = alpha))
      )
    }
    TextSecondary(
      text = stringResource(R.string.chart_legend_more),
      fontSize = tokens.captionSize
    )
  }
}
