package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.component.report.chart.DonutChart
import remix.myplayer.ui.screen.report.sourceLabel
import remix.myplayer.ui.theme.report.LocalReportTokens

/** S10 来源占比：环形图 + 图例。 */
@Composable
fun PageSources(report: AnnualReport) {
  if (report.sourceBreakdown.isEmpty()) return
  val tokens = LocalReportTokens.current
  val palette = listOf(
    tokens.accent.toArgb(),
    tokens.accentSoft.toArgb(),
    tokens.textSecondary.toArgb()
  )

  val slices = report.sourceBreakdown
    .sortedByDescending { it.plays }
    .take(3)
    .mapIndexed { index, item ->
      Triple(sourceLabel(item.source), item.plays.toLong(), palette[index % palette.size])
    }

  StoryPage {
    DonutChart(
      title = stringResource(R.string.stat_sources),
      slices = slices,
      size = 180.dp
    )
    Spacer(Modifier.height(8.dp))
  }
}
