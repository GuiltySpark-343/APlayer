package remix.myplayer.ui.component.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/**
 * 报告页的真实卡片：圆角 + 半透明背景 + 内边距。
 *
 * Modifier 顺序不能调换：padding(外边距) → clip → background → padding(内边距)，
 * 否则背景会盖住圆角、或外边距被算进卡片内部。
 */
@Composable
fun StoryCard(
  title: String? = null,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = tokens.pagePadding, vertical = tokens.cardGap / 2)
      .clip(RoundedCornerShape(tokens.cardRadius))
      .background(tokens.cardBg)
      .padding(tokens.cardPadding)
  ) {
    if (title != null) {
      TextPrimary(text = title, fontSize = tokens.titleSize, color = tokens.accent)
      Spacer(Modifier.height(8.dp))
    }
    content()
  }
}
