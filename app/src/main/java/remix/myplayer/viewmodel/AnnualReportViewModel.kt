package remix.myplayer.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import remix.myplayer.data.db.room.entity.PlayEvent
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.data.model.report.PlayEventExport
import remix.myplayer.R
import remix.myplayer.data.model.report.TrackExport
import remix.myplayer.repo.PlayEventRepository
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.ui.nav.MessageNotifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AnnualReportViewModel @Inject constructor(
  private val playEventRepository: PlayEventRepository,
  private val playListRepository: PlayListRepository,
  @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _state = MutableStateFlow(ReportUiState())
  val state = _state.asStateFlow()

  fun load() {
    viewModelScope.launch {
      val years = playEventRepository.availableYears()
      val year = _state.value.year.takeIf { it in years } ?: (years.firstOrNull() ?: currentYear())
      _state.value = ReportUiState(
        years = years,
        year = year,
        report = playEventRepository.annualReport(year),
        previousReport = previousReportOf(years, year),
        loading = false
      )
    }
  }

  fun selectYear(year: Int) {
    if (_state.value.year == year) return
    _state.value = _state.value.copy(year = year, loading = true)
    viewModelScope.launch {
      val years = _state.value.years
      _state.value = _state.value.copy(
        report = playEventRepository.annualReport(year),
        previousReport = previousReportOf(years, year),
        loading = false
      )
    }
  }

  /** P1：取比该年小的最近一年报告，用于“多年对比”。 */
  private suspend fun previousReportOf(years: List<Int>, year: Int): AnnualReport? {
    val prev = years.filter { it < year }.maxOrNull() ?: return null
    return playEventRepository.annualReport(prev)
  }

  fun clear() {
    viewModelScope.launch {
      playEventRepository.clear()
      load()
    }
  }

  fun exportJsonl() {
    viewModelScope.launch {
      val year = _state.value.year ?: return@launch
      val events = playEventRepository.eventsOf(year)
      if (events.isEmpty()) return@launch
      val intent = withContext(Dispatchers.IO) {
        writeAndShare(events)
      }
      _state.value = _state.value.copy(
        exportIntent = intent,
        exportRequestId = _state.value.exportRequestId + 1
      )
    }
  }

  fun consumeExportIntent() {
    _state.value = _state.value.copy(exportIntent = null)
  }

  /** M3：从 JSONL 导入播放事件（按 eventId 幂等合并）。 */
  fun importJsonl(uri: Uri) {
    viewModelScope.launch {
      val imported = withContext(Dispatchers.IO) {
        val events = runCatching { parseJsonl(uri) }.getOrElse { emptyList() }
        if (events.isEmpty()) 0 else playEventRepository.importEvents(events)
      }
      if (imported > 0) {
        MessageNotifier.show(R.string.import_events_success, imported)
        load()
      } else {
        MessageNotifier.show(R.string.import_events_failed)
      }
    }
  }

  private fun parseJsonl(uri: Uri): List<PlayEvent> {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val result = ArrayList<PlayEvent>()
    val stream = context.contentResolver.openInputStream(uri) ?: return result
    stream.bufferedReader().useLines { lines ->
      lines.forEach { line ->
        if (line.isBlank()) return@forEach
        val export = runCatching {
          json.decodeFromString(PlayEventExport.serializer(), line)
        }.getOrNull() ?: return@forEach
        val startedAt = parseIso(export.startedAt) ?: return@forEach
        val endedAt = parseIso(export.endedAt) ?: startedAt
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startedAt }
        result.add(
          PlayEvent(
            schemaVersion = export.schemaVersion,
            eventId = export.eventId,
            deviceId = export.deviceId,
            eventType = export.eventType,
            canonicalId = export.track.canonicalId,
            audioId = null,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMs = export.durationMs,
            listenedMs = export.listenedMs,
            listenRatio = export.listenRatio,
            playScore = export.playScore,
            completed = export.completed,
            source = export.source,
            endReason = export.endReason,
            titleSnapshot = export.track.title,
            artistSnapshot = export.track.artist,
            albumSnapshot = export.track.album,
            genreSnapshot = null,
            sourceUri = null,
            contentHash = null,
            pathHint = null,
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            weekday = cal.get(Calendar.DAY_OF_WEEK),
            songId = export.songId,
            artistId = export.artistId,
            albumId = export.albumId,
            genreId = export.genreId,
            playlistId = export.playlistId,
            mediaType = export.mediaType,
            sessionId = export.sessionId,
            gapBeforeMs = export.gapBeforeMs,
            gapAfterMs = export.gapAfterMs,
            loopCount = export.loopCount,
            outputDevice = export.outputDevice,
            isForeground = export.isForeground,
            decoder = export.decoder
          )
        )
      }
    }
    return result
  }

  private fun parseIso(value: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }.parse(value)?.time
  }.getOrNull()

  /** R10：把年度 TOP 歌曲生成为一个本地歌单。 */
  fun generatePlaylist() {
    viewModelScope.launch {
      val year = _state.value.year ?: return@launch
      val report = _state.value.report ?: return@launch
      val audioIds = report.topSongs.mapNotNull { it.audioId }.distinct()
      if (audioIds.isEmpty()) {
        MessageNotifier.show(R.string.no_play_stat_data)
        return@launch
      }
      val name = context.getString(R.string.annual_playlist_name, year)
      withContext(Dispatchers.IO) {
        if (!playListRepository.checkPlayListExist(name)) {
          playListRepository.insertPlayList(name)
        }
        playListRepository.addSongsToPlayList(audioIds, name)
      }
      MessageNotifier.show(R.string.playlist_generated, name)
    }
  }

  private suspend fun writeAndShare(events: List<PlayEvent>): Intent? = withContext(Dispatchers.IO) {
    val year = _state.value.year ?: return@withContext null
    val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "play_event")
    dir.mkdirs()
    val file = File(dir, "play-events-%d.jsonl".format(year))
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    file.bufferedWriter().use { writer ->
      events.forEach { event ->
        writer.write(json.encodeToString(PlayEventExport.serializer(), event.toExport()))
        writer.newLine()
      }
    }
    try {
      val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
      Intent(Intent.ACTION_SEND)
        .setType("application/jsonl")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: Exception) {
      null
    }
  }

  private fun PlayEvent.toExport(): PlayEventExport {
    return PlayEventExport(
      schemaVersion = schemaVersion,
      eventId = eventId,
      deviceId = deviceId,
      eventType = eventType,
      startedAt = formatIso(startedAt),
      endedAt = formatIso(endedAt),
      durationMs = durationMs,
      listenedMs = listenedMs,
      listenRatio = listenRatio,
      playScore = playScore,
      completed = completed,
      source = source,
      endReason = endReason,
      track = TrackExport(
        canonicalId = canonicalId,
        title = titleSnapshot,
        artist = artistSnapshot,
        album = albumSnapshot,
        durationMs = durationMs
      ),
      songId = songId,
      artistId = artistId,
      albumId = albumId,
      genreId = genreId,
      playlistId = playlistId,
      mediaType = mediaType,
      sessionId = sessionId,
      gapBeforeMs = gapBeforeMs,
      gapAfterMs = gapAfterMs,
      loopCount = loopCount,
      outputDevice = outputDevice,
      isForeground = isForeground,
      decoder = decoder
    )
  }

  private fun formatIso(epochMs: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(epochMs)
  }

  private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}

data class ReportUiState(
  val years: List<Int> = emptyList(),
  val year: Int? = null,
  val report: AnnualReport? = null,
  val previousReport: AnnualReport? = null,
  val loading: Boolean = true,
  val exportIntent: Intent? = null,
  val exportRequestId: Int = 0
)
