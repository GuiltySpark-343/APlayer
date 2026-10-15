package remix.myplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.data.model.audio.Song
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.SongTagRepository
import remix.myplayer.util.CanonicalIdProvider
import javax.inject.Inject

/**
 * T0.6：按标签筛选曲库。
 *
 * 筛选语义（设计文档 §6.4）：**同类内 OR，跨类 AND**。
 * 例：情境选「回家 + 通勤」= 并集；再加情绪「治愈」= 与情绪集合求交。
 */
@HiltViewModel
class TagFilterViewModel @Inject constructor(
  private val songTagRepository: SongTagRepository,
  private val songRepository: SongRepository,
  private val canonicalIdProvider: CanonicalIdProvider,
) : ViewModel() {

  data class UiState(
    val loading: Boolean = true,
    val groups: List<Pair<String, List<TagDef>>> = emptyList(),
    val usage: Map<String, Int> = emptyMap(),
    val selected: Set<String> = emptySet(),
    val songs: List<Song> = emptyList(),
    val allCount: Int = 0
  )

  private val _state = MutableStateFlow(UiState())
  val state = _state.asStateFlow()

  private var allSongs: List<Song> = emptyList()
  private var canonicalIds: Map<Long, String> = emptyMap()

  fun load() {
    viewModelScope.launch {
      val tags = songTagRepository.allTags()
      val usage = songTagRepository.usageCounts()
      val songs = withContext(Dispatchers.IO) { songRepository.allSongs() }
      allSongs = songs
      canonicalIds = withContext(Dispatchers.IO) {
        songs.associate { it.id to canonicalIdProvider.canonicalId(it) }
      }
      val groups = listOf(
        TagDef.CATEGORY_CONTEXT,
        TagDef.CATEGORY_MOOD,
        TagDef.CATEGORY_OBJECTIVE
      ).map { category -> category to tags.filter { it.category == category } }
        .filter { it.second.isNotEmpty() }
      _state.value = _state.value.copy(
        loading = false,
        groups = groups,
        usage = usage,
        allCount = songs.size
      )
    }
  }

  fun toggle(tagId: String) {
    val selected = _state.value.selected.toMutableSet()
    if (!selected.remove(tagId)) {
      selected.add(tagId)
    }
    _state.value = _state.value.copy(selected = selected)
    viewModelScope.launch { applyFilter(selected) }
  }

  fun clear() {
    _state.value = _state.value.copy(selected = emptySet(), songs = emptyList())
  }

  private suspend fun applyFilter(selected: Set<String>) {
    if (selected.isEmpty()) {
      _state.value = _state.value.copy(songs = emptyList())
      return
    }
    val tagCategories = _state.value.groups.flatMap { it.second }.associate { it.tagId to it.category }
    val perCategory = HashMap<String, MutableSet<String>>()
    selected.forEach { tagId ->
      val category = tagCategories[tagId] ?: return@forEach
      val ids = withContext(Dispatchers.IO) { songTagRepository.songsOf(tagId) }
      perCategory.getOrPut(category) { HashSet() }.addAll(ids)
    }
    var matched: Set<String>? = null
    perCategory.values.forEach { set ->
      matched = if (matched == null) set else matched!!.intersect(set)
    }
    val allowed = matched ?: emptySet()
    val songs = allSongs.filter { canonicalIds[it.id] in allowed }
    _state.value = _state.value.copy(songs = songs)
  }
}
