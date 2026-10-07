package remix.myplayer.ui.screen.report.pages

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import remix.myplayer.R
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage
import remix.myplayer.ui.nav.RouteAnnualReport
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.viewmodel.AnnualReportViewModel

/** S12 分享页：生成海报缩略图 + 分享 + 进入「全部数据」附录页。 */
@Composable
fun PageShare(viewModel: AnnualReportViewModel, nav: NavController) {
  val tokens = LocalReportTokens.current
  val state by viewModel.state.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(state.year) {
    if (state.year != null && state.posterBitmap == null) viewModel.generatePoster()
  }

  LaunchedEffect(state.sharePosterIntent) {
    val intent = state.sharePosterIntent
    if (intent != null) {
      context.startActivity(Intent.createChooser(intent, null))
      viewModel.consumeSharePosterIntent()
    }
  }

  StoryPage {
    val bitmap = state.posterBitmap
    if (bitmap == null) {
      Caption(stringResource(R.string.story_poster_loading))
    } else {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxWidth()
          .height(360.dp)
      )
    }
    Spacer(Modifier.height(16.dp))
    androidx.compose.foundation.layout.Row(
      horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
      TextPrimary(
        text = stringResource(R.string.share),
        fontSize = tokens.titleSize,
        color = tokens.accent,
        modifier = Modifier.clickable { viewModel.sharePoster() }
      )
      TextPrimary(
        text = stringResource(R.string.story_all_data),
        fontSize = tokens.titleSize,
        color = tokens.accent,
        modifier = Modifier.clickable { nav.navigate(RouteAnnualReport) }
      )
    }
  }
}
