package remix.myplayer.ui.component.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary

/** 一句情绪文案：默认最多 3 行、居中、次要色。 */
@Composable
fun Caption(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = LocalReportTokens.current.textSecondary,
  fontSize: TextUnit = LocalReportTokens.current.bodySize,
  maxLine: Int = 3
) {
  TextPrimary(
    text = text,
    modifier = modifier,
    fontSize = fontSize,
    color = color,
    maxLine = maxLine,
    textAlign = TextAlign.Center
  )
}
