package remix.myplayer.ui.screen.report.pages

import androidx.compose.runtime.Composable
import remix.myplayer.ui.component.report.Caption
import remix.myplayer.ui.component.report.StoryPage

/** 尚未实现的页面：只显示页面 id，便于截图确认页序。 */
@Composable
fun PagePlaceholder(id: String) {
  StoryPage { Caption(id) }
}
