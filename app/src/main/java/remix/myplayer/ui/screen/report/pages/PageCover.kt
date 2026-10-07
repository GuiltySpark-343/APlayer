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
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.screen.report.ReportStoryResult
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** S0 开场：年份 + 主题词 + 开始提示。 */
@Composable
fun PageCover(report: AnnualReport, story: ReportStoryResult) {
  val tokens = LocalReportTokens.current

  // joinToString 的 transform 不是 inline  lambda，不能在里面调 stringResource，
  // 因此先用 forEach（inline）把资源解析成字符串。
  val keywordLabels = ArrayList<String>(story.keywords.size)
  story.keywords.forEach { keywordLabels.add(stringResource(it.titleRes)) }

  StoryPage {
    TextPrimary(
      text = report.year.toString(),
      fontSize = tokens.heroSize * 2f,
      fontWeight = FontWeight.Bold,
      color = tokens.textPrimary
    )
    Spacer(Modifier.height(48.dp))
    TextPrimary(
      text = keywordLabels.joinToString(" · "),
      fontSize = tokens.titleSize * 1.5f,
      fontWeight = FontWeight.Bold,
      color = tokens.accent
    )
    Spacer(Modifier.height(24.dp))
    Caption(stringResource(R.string.story_start_hint), color = tokens.textFooter)
  }
}
