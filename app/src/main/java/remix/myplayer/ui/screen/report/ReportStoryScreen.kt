package remix.myplayer.ui.screen.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import remix.myplayer.ui.component.report.StoryPager
import remix.myplayer.ui.theme.report.LocalReportTokens
import remix.myplayer.ui.theme.report.ReportTokenDefaults
import remix.myplayer.viewmodel.annualReportViewModel

/**
 * 报告主线（叙事流）。
 *
 * 页面集合是动态的：数据不足的页不进入列表，见 [StoryPages.visible]。
 * 老页面（AnnualReportScreen）保留为"全部数据"附录。
 */
@Composable
fun ReportStoryScreen() {
  val viewModel = annualReportViewModel
  val state by viewModel.state.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    if (state.report == null) viewModel.load()
  }

  val report = state.report
  if (report == null) {
    // 加载中：留白，不显示"暂无数据"（附录页负责该提示）
    Box(Modifier.fillMaxSize())
    return
  }

  val story = ReportStory.analyze(report)
  val pages = StoryPages.visible(report)

  CompositionLocalProvider(LocalReportTokens provides ReportTokenDefaults.Dark) {
    StoryPager(pageCount = pages.size) { page ->
      renderStoryPage(page, report, story, context)
    }
  }
}
