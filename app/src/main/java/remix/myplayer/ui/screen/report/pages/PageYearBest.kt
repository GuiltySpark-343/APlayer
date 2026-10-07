package remix.myplayer.ui.screen.report.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.FadeInStaggered
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary

/**
 * S4 年度之最：歌手 / 专辑 / 单曲三张卡，各带「换一个」。
 *
 * 选择结果由 ViewModel 落到 report_overrides 表，退出再进仍然记得。
 */
@Composable
fun PageYearBest(
  report: AnnualReport,
  overrides: Map<String, String>,
  onSwap: (String) -> Unit
) {
  val tokens = LocalReportTokens.current

  StoryPage {
    FadeInStaggered(0) {
      TextPrimary(
        text = stringResource(R.string.story_year_best),
        fontSize = tokens.titleSize * 1.5f,
        fontWeight = FontWeight.Bold,
        color = tokens.accent
      )
    }
    Spacer(Modifier.height(24.dp))
    FadeInStaggered(1) {
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BestSlot(
          label = stringResource(R.string.stat_top_songs),
          value = songOf(report, overrides),
          slot = "song",
          onSwap = onSwap
        )
        BestSlot(
          label = stringResource(R.string.stat_top_albums),
          value = nameOf(report.topAlbums.map { it.name }, overrides, "album"),
          slot = "album",
          onSwap = onSwap
        )
        BestSlot(
          label = stringResource(R.string.stat_top_artists),
          value = nameOf(report.topArtists.map { it.name }, overrides, "artist"),
          slot = "artist",
          onSwap = onSwap
        )
      }
    }
  }
}

@Composable
private fun BestSlot(
  label: String,
  value: Pair<String, String>,
  slot: String,
  onSwap: (String) -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
    TextSecondary(text = label, fontSize = tokens.captionSize)
    Spacer(Modifier.height(4.dp))
    TextPrimary(
      text = value.first.ifEmpty { "—" },
      fontSize = tokens.titleSize,
      fontWeight = FontWeight.Bold,
      color = tokens.textPrimary,
      textAlign = TextAlign.Center
    )
    if (value.second.isNotEmpty()) {
      TextSecondary(text = value.second, fontSize = tokens.captionSize)
    }
    Spacer(Modifier.height(4.dp))
    TextPrimary(
      text = stringResource(R.string.story_swap),
      fontSize = tokens.captionSize,
      color = tokens.accent,
      modifier = Modifier
        .clickable { onSwap(slot) }
        .padding(horizontal = 12.dp, vertical = 4.dp)
    )
  }
}

private fun songOf(report: AnnualReport, overrides: Map<String, String>): Pair<String, String> {
  val list = report.topSongs
  if (list.isEmpty()) return "" to ""
  val key = overrides["song"]
  val item = list.firstOrNull { it.canonicalId == key } ?: list.first()
  return item.title to item.artist
}

private fun nameOf(
  names: List<String>,
  overrides: Map<String, String>,
  slot: String
): Pair<String, String> {
  if (names.isEmpty()) return "" to ""
  val key = overrides[slot]
  return (names.firstOrNull { it == key } ?: names.first()) to ""
}
