package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.FadeInStaggered
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.component.report.chart.PaletteStrip
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** S2 音乐颜色：你听得最多的专辑封面主色。 */
@Composable
fun PageColors(colors: List<Int>) {
  val tokens = LocalReportTokens.current
  if (colors.isEmpty()) return

  StoryPage {
    FadeInStaggered(0) {
      TextPrimary(
        text = stringResource(R.string.story_color),
        fontSize = tokens.titleSize * 1.5f,
        fontWeight = FontWeight.Bold,
        color = tokens.accent
      )
    }
    Spacer(Modifier.height(24.dp))
    FadeInStaggered(1) { PaletteStrip(colors = colors) }
    Spacer(Modifier.height(24.dp))
    FadeInStaggered(2) { Caption(stringResource(R.string.story_color_caption)) }
  }
}
