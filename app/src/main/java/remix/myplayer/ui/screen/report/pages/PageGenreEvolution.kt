package remix.myplayer.ui.screen.report.pages

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.db.room.dao.GenreCount
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.component.report.chart.GenreBands
import remix.myplayer.ui.component.report.chart.GenreStage

/** S3 曲风进化史：按季度分段，段内按曲风细分。 */
@Composable
fun PageGenreEvolution(report: AnnualReport, context: Context) {
  val stages = (1..4).mapNotNull { quarter ->
    val genres = report.genreByQuarter
      .filter { it.quarter == quarter }
      .sortedByDescending { it.plays }
      .map { GenreCount(it.genre, it.plays, it.listenedMs) }
    if (genres.isEmpty()) {
      null
    } else {
      GenreStage(context.getString(R.string.chart_quarter, quarter), genres)
    }
  }
  if (stages.isEmpty()) return

  StoryPage {
    GenreBands(
      title = stringResource(R.string.story_genre_evolution),
      stages = stages,
      height = 140.dp
    )
    Spacer(Modifier.height(16.dp))
    Caption(stringResource(R.string.story_genre_evolution))
  }
}
