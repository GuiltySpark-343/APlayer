package remix.myplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.tag.TagSeeds
import remix.myplayer.repo.SongTagRepository
import remix.myplayer.util.CanonicalIdProvider
import javax.inject.Inject

/**
 * T0.5：播放中一键打标面板。
 *
 * 情绪词「点一下即打分」：选中情绪词时同时写入该词与其 (valence, arousal) 预设坐标（见 TagSeeds.MOOD_PRESETS）；
 * 两个情绪维度以 SCALE 标签暴露，面板上可 5 档微调覆盖。
 */
@HiltViewModel
class TagPanelViewModel @Inject constructor(
  private val songTagRepository: SongTagRepository,
  private val canonicalIdProvider: CanonicalIdProvider,
) : ViewModel() {

  data class UiState(
    val visible: Boolean = false,
    val loading: Boolean = true,
    val canonicalId: String = "",
    val songTitle: String = "",
    val groups: List<Pair<String, List<TagDef>>> = emptyList(),
    val selected: Set<String> = emptySet(),
    val scores: Map<String, Double> = emptyMap(),
    val usage: Map<String, Int> = emptyMap(),
    val lastTagName: String? = null,
    val lastActionTagIds: List<String> = emptyList()
  )

  private val _state = MutableStateFlow(UiState())
  val state = _state.asStateFlow()

  fun open(song: Song) {
    viewModelScope.launch {
      val canonicalId = canonicalIdProvider.canonicalId(song)
      _state.value = _state.value.copy(
        visible = true,
        loading = true,
        canonicalId = canonicalId,
        songTitle = song.title
      )
      load(canonicalId)
    }
  }

  fun dismiss() {
    _state.value = _state.value.copy(visible = false, lastTagName = null, lastActionTagIds = emptyList())
  }

  fun toggle(def: TagDef) {
    viewModelScope.launch {
      val canonicalId = _state.value.canonicalId
      if (canonicalId.isEmpty()) return@launch
      if (_state.value.selected.contains(def.tagId)) {
        songTagRepository.untagSong(canonicalId, def.tagId)
        _state.value = _state.value.copy(lastTagName = null, lastActionTagIds = emptyList())
      } else {
        val written = arrayListOf(def.tagId)
        if (TagSeeds.MOOD_PRESETS.containsKey(def.tagId)) {
          songTagRepository.tagMood(canonicalId, def.tagId)
          written.add(TagSeeds.TAG_VALENCE)
          written.add(TagSeeds.TAG_AROUSAL)
        } else {
          songTagRepository.tagSong(canonicalId, def.tagId)
        }
        _state.value = _state.value.copy(lastTagName = def.name, lastActionTagIds = written)
      }
      load(canonicalId)
    }
  }

  /** 微调情绪维度（1..5）。 */
  fun setScore(tagId: String, score: Int) {
    viewModelScope.launch {
      val canonicalId = _state.value.canonicalId
      if (canonicalId.isEmpty()) return@launch
      songTagRepository.tagSong(canonicalId, tagId, score.toDouble())
      load(canonicalId)
    }
  }

  fun undoLast() {
    viewModelScope.launch {
      val canonicalId = _state.value.canonicalId
      if (canonicalId.isEmpty()) return@launch
      _state.value.lastActionTagIds.forEach { songTagRepository.untagSong(canonicalId, it) }
      _state.value = _state.value.copy(lastTagName = null, lastActionTagIds = emptyList())
      load(canonicalId)
    }
  }

  fun clearUndo() {
    _state.value = _state.value.copy(lastTagName = null, lastActionTagIds = emptyList())
  }

  private suspend fun load(canonicalId: String) {
    val tags = songTagRepository.allTags()
    val usage = songTagRepository.usageCounts()
    val rows = songTagRepository.tagsOf(canonicalId)
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
      selected = rows.map { it.tagId }.toSet(),
      scores = rows.mapNotNull { row -> row.score?.let { row.tagId to it } }.toMap()
    )
  }
}
