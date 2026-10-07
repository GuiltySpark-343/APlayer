package remix.myplayer.ui.component.report.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/**
 * 图表外壳：标题 + 固定高度的画布 + 可选图例。
 *
 * 横向内边距由外层 StoryPage 负责，这里不要再加，避免双重留白。
 */
@Composable
fun ChartFrame(
  title: String,
  modifier: Modifier = Modifier,
  height: Dp = 120.dp,
  legend: (@Composable () -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(modifier = modifier.fillMaxWidth()) {
    TextPrimary(text = title, fontSize = tokens.titleSize, color = tokens.accent)
    Spacer(Modifier.height(8.dp))
    Box(modifier = Modifier.fillMaxWidth().height(height), content = content)
    if (legend != null) {
      Spacer(Modifier.height(8.dp))
      legend()
    }
  }
}
