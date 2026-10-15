package remix.myplayer.ui.screen.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.viewmodel.TagPanelViewModel

/**
 * T0.5：一键打标面板。
 *
 * 点一下 = 打标（情绪词同时写入 valence/arousal 预设坐标）；已打分时可 5 档微调覆盖；
 * 打标后 3 秒内可撤销。全程不中断播放。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPanelSheet(
  state: TagPanelViewModel.UiState,
  onToggle: (TagDef) -> Unit,
  onScore: (String, Int) -> Unit,
  onUndo: () -> Unit,
  onClearUndo: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!state.visible) return

  LaunchedEffect(state.lastTagName) {
    if (state.lastTagName != null) {
      delay(3000)
      onClearUndo()
    }
  }

  val moodScales = state.groups
    .firstOrNull { it.first == TagDef.CATEGORY_MOOD }
    ?.second
    ?.filter { it.kind == TagDef.KIND_SCALE }
    .orEmpty()
  val showScores = state.scores.isNotEmpty() && moodScales.isNotEmpty()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { onDismiss() }
      )
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.75f),
        color = LocalTheme.current.dialogBackground,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        shadowElevation = 8.dp,
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          TextPrimary(text = state.songTitle, fontSize = 16.sp)
          Spacer(Modifier.height(2.dp))
          TextSecondary(text = stringResource(R.string.tag_score_hint), fontSize = 11.sp)
          Spacer(Modifier.height(10.dp))

          Column(
            modifier = Modifier
              .weight(1f)
              .verticalScroll(rememberScrollState())
          ) {
            state.groups.forEach { (category, tags) ->
              val chips = tags.filter { it.kind != TagDef.KIND_SCALE }
              if (chips.isEmpty()) return@forEach
              TextPrimary(
                text = categoryLabel(category),
                fontSize = 13.sp,
                color = LocalTheme.current.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
              )
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { tag ->
                  FilterChip(
                    selected = state.selected.contains(tag.tagId),
                    onClick = { onToggle(tag) },
                    label = { Text(tag.name) }
                  )
                }
              }
            }

            if (showScores) {
              TextPrimary(
                text = stringResource(R.string.tag_more),
                fontSize = 13.sp,
                color = LocalTheme.current.primary,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
              )
              moodScales.forEach { scale ->
                val current = state.scores[scale.tagId]?.toInt()
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  TextSecondary(
                    text = if (scale.tagId == "mood.valence") {
                      stringResource(R.string.tag_valence)
                    } else {
                      stringResource(R.string.tag_arousal)
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                  )
                  (1..5).forEach { value ->
                    ScoreDot(
                      value = value,
                      selected = current == value,
                      accent = LocalTheme.current.primary
                    ) { onScore(scale.tagId, value) }
                  }
                }
              }
            }
          }

          state.lastTagName?.let { name ->
            Spacer(Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextSecondary(
                text = stringResource(R.string.tag_added_toast, name),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = stringResource(R.string.tag_undo),
                color = LocalTheme.current.primary,
                fontSize = 14.sp,
                modifier = Modifier
                  .clickable { onUndo() }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ScoreDot(value: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .padding(horizontal = 3.dp)
      .size(28.dp)
      .background(if (selected) accent else accent.copy(alpha = 0.15f), CircleShape)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = value.toString(),
      color = if (selected) Color.White else LocalTheme.current.textPrimary,
      fontSize = 12.sp
    )
  }
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
  TagDef.CATEGORY_CONTEXT -> stringResource(R.string.tag_category_context)
  TagDef.CATEGORY_MOOD -> stringResource(R.string.tag_category_mood)
  else -> stringResource(R.string.tag_category_objective)
}
