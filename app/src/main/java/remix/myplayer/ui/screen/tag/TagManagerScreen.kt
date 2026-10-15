package remix.myplayer.ui.screen.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteTagFilter
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.ext.activityViewModel
import remix.myplayer.viewmodel.TagManagerViewModel

private val TAG_COLORS = listOf(
  0xFFE06666, 0xFFF6B26B, 0xFFFFD966, 0xFF93C47D,
  0xFF6FA8DC, 0xFF8E7CC3, 0xFFC27BA0, 0xFF9AA0A6
).map { it.toInt() }

private sealed interface TagDialog {
  data object Create : TagDialog
  data class Actions(val def: TagDef, val usage: Int) : TagDialog
  data class Rename(val def: TagDef) : TagDialog
  data class Color(val def: TagDef) : TagDialog
  data class Merge(val def: TagDef) : TagDialog
  data class Delete(val def: TagDef, val usage: Int) : TagDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen() {
  val viewModel: TagManagerViewModel = activityViewModel()
  val state by viewModel.state.collectAsStateWithLifecycle()
  var dialog by remember { mutableStateOf<TagDialog?>(null) }

  LaunchedEffect(Unit) { viewModel.load() }

  Scaffold(
    topBar = {
      CommonAppBar(
        title = stringResource(R.string.song_tags),
        actions = {
          val nav = LocalNavController.current
          Text(
            text = stringResource(R.string.tag_filter),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
              .padding(end = 16.dp)
              .clickable { nav.navigate(RouteTagFilter) }
          )
          Text(
            text = "+",
            color = Color.White,
            fontSize = 26.sp,
            modifier = Modifier
              .padding(end = 16.dp)
              .clickable { dialog = TagDialog.Create }
          )
        }
      )
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { padding ->
    if (state.loading) return@Scaffold
    LazyColumn(modifier = Modifier.padding(padding)) {
      if (state.groups.isEmpty()) {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            TextSecondary(text = stringResource(R.string.tag_empty), fontSize = 15.sp)
          }
        }
      }
      state.groups.forEach { (category, tagItems) ->
        item(key = "header_" + category) {
          TextPrimary(
            text = categoryLabel(category),
            fontSize = 14.sp,
            color = LocalTheme.current.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
          )
        }
        items(tagItems, key = { it.def.tagId }) { item ->
          TagRow(item) { dialog = TagDialog.Actions(item.def, item.usage) }
        }
      }
      item { Spacer(Modifier.height(24.dp)) }
    }
  }

  when (val d = dialog) {
    null -> Unit
    TagDialog.Create -> CreateDialog(
      onDismiss = { dialog = null },
      onConfirm = { name, category, color ->
        viewModel.createTag(name, category, color)
        dialog = null
      }
    )
    is TagDialog.Actions -> ActionsDialog(
      def = d.def,
      onDismiss = { dialog = null },
      onMoveUp = {
        viewModel.moveTag(d.def.tagId, true)
        dialog = null
      },
      onMoveDown = {
        viewModel.moveTag(d.def.tagId, false)
        dialog = null
      },
      onRename = { dialog = TagDialog.Rename(d.def) },
      onColor = { dialog = TagDialog.Color(d.def) },
      onMerge = { dialog = TagDialog.Merge(d.def) },
      onDelete = { dialog = TagDialog.Delete(d.def, d.usage) }
    )
    is TagDialog.Rename -> RenameDialog(
      def = d.def,
      onDismiss = { dialog = null },
      onConfirm = { name ->
        viewModel.renameTag(d.def.tagId, name)
        dialog = null
      }
    )
    is TagDialog.Color -> ColorDialog(
      def = d.def,
      onDismiss = { dialog = null },
      onPick = { color ->
        viewModel.updateColor(d.def.tagId, color)
        dialog = null
      }
    )
    is TagDialog.Merge -> MergeDialog(
      def = d.def,
      candidates = state.all.filter { it.tagId != d.def.tagId },
      onDismiss = { dialog = null },
      onPick = { into ->
        viewModel.mergeTag(d.def.tagId, into)
        dialog = null
      }
    )
    is TagDialog.Delete -> DeleteDialog(
      def = d.def,
      usage = d.usage,
      onDismiss = { dialog = null },
      onConfirm = {
        viewModel.deleteTag(d.def.tagId)
        dialog = null
      }
    )
  }
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
  TagDef.CATEGORY_CONTEXT -> stringResource(R.string.tag_category_context)
  TagDef.CATEGORY_MOOD -> stringResource(R.string.tag_category_mood)
  else -> stringResource(R.string.tag_category_objective)
}

@Composable
private fun TagRow(item: TagManagerViewModel.TagItem, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(12.dp)
        .background(Color(item.def.colorArgb ?: 0xFF9AA0A6.toInt()), CircleShape)
    )
    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
      TextPrimary(text = item.def.name, fontSize = 15.sp)
      if (item.def.isPreset) {
        TextSecondary(text = "preset", fontSize = 11.sp)
      }
    }
    TextSecondary(text = item.usage.toString(), fontSize = 13.sp)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, Int?) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(TagDef.CATEGORY_CONTEXT) }
  var color by remember { mutableStateOf<Int?>(TAG_COLORS.first()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.tag_new)) },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.tag_new)) },
          singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            TagDef.CATEGORY_CONTEXT,
            TagDef.CATEGORY_MOOD,
            TagDef.CATEGORY_OBJECTIVE
          ).forEach { c ->
            FilterChip(
              selected = category == c,
              onClick = { category = c },
              label = { Text(categoryLabel(c)) }
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        ColorPickerRow(color) { color = it }
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name, category, color) }) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}

@Composable
private fun ActionsDialog(
  def: TagDef,
  onDismiss: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRename: () -> Unit,
  onColor: () -> Unit,
  onMerge: () -> Unit,
  onDelete: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(def.name) },
    text = {
      Column {
        ActionText(stringResource(R.string.tag_move_up), onMoveUp)
        ActionText(stringResource(R.string.tag_move_down), onMoveDown)
        ActionText(stringResource(R.string.tag_rename), onRename)
        ActionText(stringResource(R.string.tag_change_color), onColor)
        ActionText(stringResource(R.string.tag_merge), onMerge)
        ActionText(stringResource(R.string.tag_delete), onDelete)
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}

@Composable
private fun ActionText(text: String, onClick: () -> Unit) {
  TextPrimary(
    text = text,
    fontSize = 15.sp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 10.dp)
  )
}

@Composable
private fun RenameDialog(def: TagDef, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var name by remember { mutableStateOf(def.name) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.tag_rename)) },
    text = {
      OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name) }) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}

@Composable
private fun ColorDialog(def: TagDef, onDismiss: () -> Unit, onPick: (Int?) -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.tag_change_color)) },
    text = { ColorPickerRow(def.colorArgb) { onPick(it) } },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}

@Composable
private fun ColorPickerRow(selected: Int?, onPick: (Int) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    TAG_COLORS.forEach { c ->
      Box(
        modifier = Modifier
          .size(if (c == selected) 28.dp else 24.dp)
          .background(Color(c), CircleShape)
          .clickable { onPick(c) }
      )
    }
  }
}

@Composable
private fun MergeDialog(
  def: TagDef,
  candidates: List<TagDef>,
  onDismiss: () -> Unit,
  onPick: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.tag_merge)) },
    text = {
      LazyColumn {
        items(candidates, key = { it.tagId }) { candidate ->
          TextPrimary(
            text = candidate.name,
            fontSize = 15.sp,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPick(candidate.tagId) }
              .padding(vertical = 10.dp)
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}

@Composable
private fun DeleteDialog(def: TagDef, usage: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.tag_delete)) },
    text = { Text(stringResource(R.string.tag_delete_confirm, usage)) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    }
  )
}
