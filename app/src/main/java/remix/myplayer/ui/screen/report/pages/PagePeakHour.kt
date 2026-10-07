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
import remix.myplayer.ui.component.report.chart.PolarClockChart

/** S5 最爱听歌时段：24 小时极坐标时钟。 */
@Composable
fun PagePeakHour(report: AnnualReport) {
  if (report.hourDistribution.isEmpty()) return
  StoryPage {
    PolarClockChart(
      title = stringResource(R.string.stat_hours),
      hours = report.hourDistribution,
      height = 240.dp
    )
    Spacer(Modifier.height(8.dp))
  }
}
