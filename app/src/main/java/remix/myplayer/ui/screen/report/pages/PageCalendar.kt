package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.component.report.chart.CalendarHeatmap

/** S7 年度年历：12 × 31 热力图。 */
@Composable
fun PageCalendar(report: AnnualReport) {
  if (report.dailyDistribution.isEmpty()) return
  StoryPage {
    CalendarHeatmap(
      title = stringResource(R.string.stat_heatmap),
      days = report.dailyDistribution,
      height = 260.dp
    )
    Spacer(Modifier.height(8.dp))
  }
}
