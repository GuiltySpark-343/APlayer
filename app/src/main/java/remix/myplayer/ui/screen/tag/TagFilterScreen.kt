package remix.myplayer.ui.screen.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ext.activityViewModel
import remix.myplayer.viewmodel.TagFilterViewModel

/**
 * T0.6：按标签筛选曲库。
 *
 * 同类内 OR、跨类 AND（见 TagFilterViewModel）；点歌播放该结果列表，或「播放全部」。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterScreen() {
  val viewModel: TagFilterViewModel = activityViewModel()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val songs = state.songs

  LaunchedEffect(Unit) { viewModel.load() }

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.tag_filter), actions = {}) },
    containerColor = LocalTheme.current.mainBackground,
  ) { padding ->
    Column(modifier = Modifier.padding(padding)) {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        state.groups.forEach { (category, tags) ->
          TextPrimary(
            text = filterCategoryLabel(category),
            fontSize = 13.sp,
            color = LocalTheme.current.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
              val count = state.usage[tag.tagId] ?: 0
              FilterChip(
                selected = state.selected.contains(tag.tagId),
                onClick = { viewModel.toggle(tag.tagId) },
                label = { Text(tag.name + " " + count) }
              )
            }
          }
        }

        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val totalMs = songs.sumOf { song -> song.duration }
          TextSecondary(
            text = stringResource(R.string.tag_matched, songs.size, state.allCount) +
              if (songs.isEmpty()) "" else "  ·  " + formatDuration(totalMs),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
          )
          if (songs.isNotEmpty()) {
            Text(
              text = stringResource(R.string.tag_play_all),
              color = LocalTheme.current.primary,
              fontSize = 14.sp,
              modifier = Modifier
                .clickable {
                  setPlayQueue(
                    songs,
                    MusicUtil.makeCmdIntent(Command.PLAY_AT)
                      .putExtra(MusicService.EXTRA_POSITION, 0)
                  )
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
              text = stringResource(R.string.tag_play_shuffle),
              color = LocalTheme.current.primary,
              fontSize = 14.sp,
              modifier = Modifier
                .clickable {
                  setPlayQueue(
                    songs.shuffled(),
                    MusicUtil.makeCmdIntent(Command.PLAY_AT)
                      .putExtra(MusicService.EXTRA_POSITION, 0)
                  )
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        Spacer(Modifier.height(4.dp))
      }

      if (songs.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          TextSecondary(text = stringResource(R.string.tag_filter_empty), fontSize = 14.sp)
        }
      } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
          itemsIndexed(songs, key = { _, song -> song.id }) { pos, song ->
            ListSong(
              modifier = Modifier.height(64.dp),
              song = song,
              modelParent = song,
              selected = false,
              playing = false,
              onClickSong = {
                setPlayQueue(
                  songs,
                  MusicUtil.makeCmdIntent(Command.PLAY_AT)
                    .putExtra(MusicService.EXTRA_POSITION, pos)
                )
              },
              onLongClickSong = {}
            )
          }
        }
      }
    }
  }
}

private fun formatDuration(ms: Long): String {
  val minutes = ms / 60000.0
  return if (minutes < 60) "%.0f min".format(minutes) else "%.1f h".format(minutes / 60.0)
}

@Composable
private fun filterCategoryLabel(category: String): String = when (category) {
  TagDef.CATEGORY_CONTEXT -> stringResource(R.string.tag_category_context)
  TagDef.CATEGORY_MOOD -> stringResource(R.string.tag_category_mood)
  else -> stringResource(R.string.tag_category_objective)
}
