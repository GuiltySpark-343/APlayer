package remix.myplayer.ui.screen.report.pages

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.FadeInStaggered
import remix.myplayer.ui.component.report.HeroNumber
import remix.myplayer.ui.component.report.StoryPage

/** S1 总览：一年听了多久。 */
@Composable
fun PageOverview(report: AnnualReport, context: Context) {
  StoryPage {
    FadeInStaggered(0) {
      HeroNumber(
        target = report.listenMs / 3_600_000f,
        format = { "%.1f".format(it) },
        suffix = " " + context.getString(R.string.poster_hours_suffix)
      )
    }
    Spacer(Modifier.height(16.dp))
    FadeInStaggered(1) {
      Caption(
        context.getString(
          R.string.story_overview_caption,
          report.plays,
          report.distinctSongs,
          report.listenedDays
        )
      )
    }
  }
}
