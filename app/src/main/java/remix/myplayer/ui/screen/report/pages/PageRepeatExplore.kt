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

/**
 * S9 探索 vs 重复。
 *
 * 两条都必须是**百分比**才能放在同一根轴上比较；早期版本拿"平均播放次数 6.11"
 * 去和"探索度 100%" 比长度，单位不同、毫无意义（已修）。
 */
@Composable
fun PageRepeatExplore(story: ReportStoryResult) {
  val explore = (story.explore * 100).coerceIn(0.0, 100.0)
  val repeat = 100.0 - explore

  StoryPage {
    DualBarCompare(
      leftLabel = stringResource(R.string.stat_repeat),
      leftValue = repeat.toFloat(),
      rightLabel = stringResource(R.string.stat_explore),
      rightValue = explore.toFloat(),
      leftText = "%.0f%%".format(repeat),
      rightText = "%.0f%%".format(explore)
    )
    Spacer(Modifier.height(16.dp))
    Caption(stringResource(R.string.story_repeat_avg, "%.2f".format(story.repeat)))
  }
}
