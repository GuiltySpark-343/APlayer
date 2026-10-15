package remix.myplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.repo.SongTagRepository
import javax.inject.Inject

/** T0.4：标签管理页。 */
@HiltViewModel
class TagManagerViewModel @Inject constructor(
  private val songTagRepository: SongTagRepository,
) : ViewModel() {

  data class TagItem(val def: TagDef, val usage: Int)

  data class UiState(
    val loading: Boolean = true,
    val groups: List<Pair<String, List<TagItem>>> = emptyList(),
    val all: List<TagDef> = emptyList()
  )

  private val _state = MutableStateFlow(UiState())
  val state = _state.asStateFlow()

  fun load() {
    viewModelScope.launch { refresh() }
  }

  fun createTag(name: String, category: String, colorArgb: Int?) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      songTagRepository.createTag(trimmed, category, TagDef.KIND_BOOLEAN, colorArgb)
      refresh()
    }
  }

  fun renameTag(tagId: String, newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      songTagRepository.renameTag(tagId, trimmed)
      refresh()
    }
  }

  fun updateColor(tagId: String, colorArgb: Int?) {
    viewModelScope.launch {
      songTagRepository.updateColor(tagId, colorArgb)
      refresh()
    }
  }

  fun mergeTag(fromTagId: String, intoTagId: String) {
    viewModelScope.launch {
      songTagRepository.mergeTag(fromTagId, intoTagId)
      refresh()
    }
  }

  fun deleteTag(tagId: String) {
    viewModelScope.launch {
      songTagRepository.deleteTag(tagId)
      refresh()
    }
  }

  private suspend fun refresh() {
    val tags = songTagRepository.allTags()
    val usage = songTagRepository.usageCounts()
    val items = tags.map { TagItem(it, usage[it.tagId] ?: 0) }
    val groups = listOf(
      TagDef.CATEGORY_CONTEXT,
      TagDef.CATEGORY_MOOD,
      TagDef.CATEGORY_OBJECTIVE
    ).map { category -> category to items.filter { it.def.category == category } }
      .filter { it.second.isNotEmpty() }
    _state.value = UiState(loading = false, groups = groups, all = tags)
  }
}
