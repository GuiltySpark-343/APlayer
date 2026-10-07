package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.FadeInStaggered
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.screen.report.ReportStoryResult
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary

/** S6 深夜党：0–6 点占比 + 深夜最常听。 */
@Composable
fun PageNight(report: AnnualReport, story: ReportStoryResult) {
  val tokens = LocalReportTokens.current
  val latePlays = report.hourDistribution.filter { it.hour in 0..5 }.sumOf { it.plays }
  if (latePlays <= 0) return

  StoryPage {
    FadeInStaggered(0) {
      TextPrimary(
        text = "%.0f%%".format(story.lateNightRatio * 100),
        fontSize = tokens.heroSize,
        fontWeight = FontWeight.Bold,
        color = tokens.accent
      )
    }
    Spacer(Modifier.height(8.dp))
    FadeInStaggered(1) {
      Caption(stringResource(R.string.stat_late_night), color = tokens.textSecondary)
    }
    Spacer(Modifier.height(24.dp))
    FadeInStaggered(2) {
      TextSecondary(
        text = stringResource(R.string.stat_late_night_top),
        fontSize = tokens.captionSize
      )
      Spacer(Modifier.height(8.dp))
      report.lateNightTopSongs.take(3).forEach { item ->
        TextPrimary(text = item.title, fontSize = tokens.bodySize)
        TextSecondary(text = item.artist, fontSize = tokens.captionSize)
        Spacer(Modifier.height(6.dp))
      }
    }
  }
}
