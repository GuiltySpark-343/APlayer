package remix.myplayer.ui.screen.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R

/**
 * 播放来源的中文文案映射。附录页与叙事页共用，避免两处各写一份。
 * 未知来源原样返回（协议允许新增来源值）。
 */
@Composable
fun sourceLabel(source: String): String = when (source) {
  "SEARCH_CLICK" -> stringResource(R.string.source_search)
  "LIBRARY_CLICK" -> stringResource(R.string.source_library)
  "PLAYLIST_CLICK" -> stringResource(R.string.source_playlist)
  "QUEUE_AUTO" -> stringResource(R.string.source_queue_auto)
  "RESUME" -> stringResource(R.string.source_resume)
  "EXTERNAL_INTENT" -> stringResource(R.string.source_external)
  else -> source
}
