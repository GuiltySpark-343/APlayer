package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.component.report.chart.DualBarCompare
import remix.myplayer.ui.screen.report.ReportStoryResult

/** S9 探索 vs 重复：左条重复度、右条探索度。 */
@Composable
fun PageRepeatExplore(story: ReportStoryResult) {
  StoryPage {
    DualBarCompare(
      leftLabel = stringResource(R.string.stat_repeat),
      leftValue = story.repeat.toFloat(),
      rightLabel = stringResource(R.string.stat_explore),
      rightValue = (story.explore * 100).toFloat(),
      leftText = "%.2f".format(story.repeat),
      rightText = "%.0f%%".format(story.explore * 100)
    )
    Spacer(Modifier.height(16.dp))
    Caption(stringResource(R.string.stat_repeat) + " / " + stringResource(R.string.stat_explore))
  }
}
