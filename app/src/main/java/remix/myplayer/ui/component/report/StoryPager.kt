package remix.myplayer.ui.component.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import remix.myplayer.ui.theme.report.LocalReportTokens

/**
 * 报告主线：横向分页 + 底部进度点。
 *
 * 背景渐变在这里统一定义，各页不要重复绘制。
 */
@Composable
fun StoryPager(
  pageCount: Int,
  modifier: Modifier = Modifier,
  state: PagerState = rememberPagerState(pageCount = { pageCount }),
  content: @Composable (Int) -> Unit
) {
  val tokens = LocalReportTokens.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(tokens.bgTop, tokens.bgBottom)))
  ) {
    HorizontalPager(state = state, modifier = Modifier.weight(1f)) { page ->
      // 翻页视差：相邻页按偏移量反向微移，幅度固定 40f
      val reduceMotion = LocalReduceMotion.current
      val offset = (page - state.currentPage) + state.currentPageOffsetFraction
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { translationX = if (reduceMotion) 0f else -offset * 40f }
      ) {
        content(page)
      }
    }
    StoryProgress(current = state.currentPage, count = pageCount)
  }
}

@Composable
private fun StoryProgress(current: Int, count: Int) {
  val tokens = LocalReportTokens.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 20.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    repeat(count) { index ->
      Box(
        modifier = Modifier
          .padding(horizontal = 3.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(
            if (index == current) tokens.accent else tokens.textSecondary.copy(alpha = 0.3f)
          )
      )
    }
  }
}
