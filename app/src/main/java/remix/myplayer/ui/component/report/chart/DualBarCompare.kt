package remix.myplayer.ui.component.report.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary

/**
 * 左右对撞双条：左条自左向右生长、右条自右向左生长，长度按两者较大值归一化。
 *
 * 用 Compose 布局而不是 Canvas 实现——无坐标计算，不会有溢出风险。
 */
@Composable
fun DualBarCompare(
  leftLabel: String,
  leftValue: Float,
  rightLabel: String,
  rightValue: Float,
  leftText: String,
  rightText: String,
  modifier: Modifier = Modifier
) {
  val tokens = LocalReportTokens.current
  val max = maxOf(leftValue, rightValue).coerceAtLeast(1f)
  val progress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(600),
    label = "dualBar"
  )

  Column(modifier = modifier.fillMaxWidth()) {
    ComparisonRow(
      label = leftLabel,
      text = leftText,
      fraction = leftValue / max * progress,
      barColor = tokens.textSecondary,
      textFirst = false,
      tokens = tokens
    )
    Spacer(Modifier.height(10.dp))
    ComparisonRow(
      label = rightLabel,
      text = rightText,
      fraction = rightValue / max * progress,
      barColor = tokens.accent,
      textFirst = true,
      tokens = tokens
    )
  }
}

@Composable
private fun ComparisonRow(
  label: String,
  text: String,
  fraction: Float,
  barColor: androidx.compose.ui.graphics.Color,
  textFirst: Boolean,
  tokens: remix.myplayer.ui.theme.report.ReportTokens
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (textFirst) {
      TextPrimary(text = text, fontSize = tokens.bodySize, color = tokens.accent)
      Spacer(Modifier.width(8.dp))
    } else {
      TextSecondary(text = label, fontSize = tokens.captionSize)
      Spacer(Modifier.width(8.dp))
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .height(14.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(tokens.cardBg)
    ) {
      Box(
        modifier = Modifier
          .align(if (textFirst) Alignment.CenterEnd else Alignment.CenterStart)
          .fillMaxHeight()
          .fillMaxWidth(fraction.coerceIn(0f, 1f))
          .background(barColor)
      )
    }

    if (textFirst) {
      Spacer(Modifier.width(8.dp))
      TextSecondary(text = label, fontSize = tokens.captionSize)
    } else {
      Spacer(Modifier.width(8.dp))
      TextPrimary(text = text, fontSize = tokens.bodySize, color = tokens.textSecondary)
    }
  }
}
