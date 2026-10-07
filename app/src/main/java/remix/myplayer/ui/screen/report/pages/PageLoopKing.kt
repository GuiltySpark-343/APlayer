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
import remix.myplayer.ui.component.report.HeroNumber
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** S8 单曲循环王：把一首歌听穿的那首。 */
@Composable
fun PageLoopKing(report: AnnualReport) {
  val tokens = LocalReportTokens.current
  val top = report.loopTop.firstOrNull() ?: return

  StoryPage {
    FadeInStaggered(0) {
      TextPrimary(
        text = stringResource(R.string.stat_loop_top),
        fontSize = tokens.titleSize,
        fontWeight = FontWeight.Bold,
        color = tokens.accent
      )
    }
    Spacer(Modifier.height(16.dp))
    FadeInStaggered(1) {
      HeroNumber(
        target = top.loops.toFloat(),
        format = { "%.0f".format(it) },
        suffix = " x"
      )
    }
    Spacer(Modifier.height(12.dp))
    FadeInStaggered(2) {
      Caption(top.title)
      Spacer(Modifier.height(4.dp))
      Caption(top.artist, fontSize = tokens.captionSize)
    }
  }
}
