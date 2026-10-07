package remix.myplayer.ui.component.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens

/**
 * 一页的内边距与居中。
 *
 * 背景由 StoryPager 统一绘制，这里不画背景，避免两层渐变叠加。
 */
@Composable
fun StoryPage(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = tokens.pagePadding, vertical = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content
  )
}
