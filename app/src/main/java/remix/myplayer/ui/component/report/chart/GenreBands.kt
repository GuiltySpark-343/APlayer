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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.data.db.room.dao.GenreCount
import remix.myplayer.ui.theme.report.LocalReportTokens

/** 曲风进化的一个阶段（如 1 个季度）。 */
data class GenreStage(
  val label: String,
  val genres: List<GenreCount>
)

/**
 * 分段色带：每段宽度按该阶段播放量占比，段内再按各曲风细分（同色不同 alpha）。
 * 段下方标阶段名。
 */
@Composable
fun GenreBands(
  title: String,
  stages: List<GenreStage>,
  modifier: Modifier = Modifier,
  height: Dp = 110.dp
) {
  val tokens = LocalReportTokens.current
  val totals = stages.map { stage -> stage.genres.sumOf { it.plays } }
  val grand = totals.sum()
  if (stages.isEmpty() || grand <= 0) return

  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(600),
    label = "genreBands"
  )
  val measurer = rememberTextMeasurer()

  ChartFrame(title = title, modifier = modifier, height = height) {
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
      val labelHeight = 20.dp.toPx()
      val barHeight = 24.dp.toPx()
      val top = ((size.height - barHeight - labelHeight) / 2f).coerceAtLeast(0f)

      var x = 0f
      stages.forEachIndexed { index, stage ->
        val stageWidth = size.width * totals[index] / grand
        if (stageWidth <= 0f) return@forEachIndexed

        var segmentX = x
        val genreCount = stage.genres.size
        stage.genres.forEachIndexed { genreIndex, genre ->
          val segmentWidth = if (totals[index] > 0) {
            stageWidth * genre.plays / totals[index]
          } else {
            0f
          }
          // 同一阶段内靠 alpha 区分曲风：靠前的更深
          val alpha = 0.35f + 0.65f * (1f - genreIndex.toFloat() / genreCount.coerceAtLeast(1))
          drawRect(
            color = tokens.accent.copy(alpha = alpha * progress),
            topLeft = Offset(segmentX, top),
            size = Size(segmentWidth, barHeight)
          )
          segmentX += segmentWidth
        }

        val layout = measurer.measure(
          AnnotatedString(stage.label),
          style = TextStyle(color = tokens.textSecondary, fontSize = tokens.captionSize)
        )
        drawText(
          textLayoutResult = layout,
          topLeft = Offset(
            (x + stageWidth / 2f - layout.size.width / 2f)
              .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f)),
            top + barHeight + 2.dp.toPx()
          )
        )
        x += stageWidth
      }
    }
  }
}
