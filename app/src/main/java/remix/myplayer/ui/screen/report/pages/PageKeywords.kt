package remix.myplayer.ui.screen.report.pages

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.screen.report.ReportStoryResult
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** S11 年度关键词：标签组 + 一句话总结。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageKeywords(report: AnnualReport, story: ReportStoryResult, context: Context) {
  val tokens = LocalReportTokens.current

  // joinToString 的 transform 不是 inline lambda，不能在里面调 stringResource，
  // 因此先用 forEach（inline）解析成字符串。
  val labels = ArrayList<String>(story.keywords.size)
  story.keywords.forEach { labels.add(stringResource(it.titleRes)) }

  val duration = if (report.listenMs >= 3_600_000L) {
    context.getString(R.string.poster_hours, report.listenMs / 3_600_000.0)
  } else {
    context.getString(R.string.poster_minutes, (report.listenMs / 60_000L).toInt())
  }
  val sentence = context.getString(
    R.string.story_template,
    duration,
    report.distinctSongs,
    story.peakHour,
    report.firstListenedSongs
  )

  StoryPage {
    FlowRow(
      horizontalArrangement = Arrangement.Center,
      verticalArrangement = Arrangement.Center
    ) {
      labels.forEach { label ->
        Box(
          modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(tokens.cardRadius))
            .background(tokens.cardBg)
            .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
          TextPrimary(
            text = label,
            fontSize = tokens.titleSize,
            fontWeight = FontWeight.Bold,
            color = tokens.accent
          )
        }
      }
    }
    Spacer(Modifier.height(24.dp))
    Caption(sentence, maxLine = 4)
  }
}
