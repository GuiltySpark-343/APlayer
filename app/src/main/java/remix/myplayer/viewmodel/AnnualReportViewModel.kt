package remix.myplayer.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.toArgb
import remix.myplayer.data.db.room.dao.ReportOverrideDao
import remix.myplayer.data.db.room.entity.ReportOverride
import remix.myplayer.repo.AlbumColorRepository
import remix.myplayer.repo.PlayEventRepository
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.ui.theme.report.ReportTokenDefaults
import remix.myplayer.ui.theme.report.reportTokensFor
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.report.ReportPoster
import remix.myplayer.ui.screen.report.ReportStory
import remix.myplayer.util.Util
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AnnualReportViewModel @Inject constructor(
  private val playEventRepository: PlayEventRepository,
  private val playListRepository: PlayListRepository,
  private val albumColorRepository: AlbumColorRepository,
  private val reportOverrideDao: ReportOverrideDao,
  @param:ApplicationContext private val context: Context,
) : ViewModel() {

  /** 年度之最的一个候选项。 */
  private data class BestCandidate(
    val key: String,
    val audioId: Long?,
    val title: String,
    val artist: String
  )

  private val _state = MutableStateFlow(ReportUiState())
  val state = _state.asStateFlow()

  fun load() {
    viewModelScope.launch {
      val years = playEventRepository.availableYears()
      val year = _state.value.year.takeIf { it in years } ?: (years.firstOrNull() ?: currentYear())
      val report = playEventRepository.annualReport(year)
      _state.value = ReportUiState(
        years = years,
        year = year,
        report = report,
        previousReport = previousReportOf(years, year),
        paletteColors = paletteColorsOf(report),
        bestOverrides = bestOverridesOf(year),
        loading = false
      )
    }
  }

  /**
   * S2 音乐颜色：按 Top 歌曲顺序取专辑封面主色（去重）。
   * 取不到封面时由仓库回填默认主色，保证页面不会空掉。
   */
  private suspend fun paletteColorsOf(report: AnnualReport): List<Int> {
    val audioIds = report.topSongs.mapNotNull { it.audioId }
    if (audioIds.isEmpty()) return emptyList()
    return albumColorRepository.colorsInOrder(
      audioIds = audioIds,
      fallbackArgb = ReportTokenDefaults.Dark.accent.toArgb()
    ).distinct()
  }

  fun selectYear(year: Int) {
    if (_state.value.year == year) return
    _state.value = _state.value.copy(year = year, loading = true)
    viewModelScope.launch {
      val years = _state.value.years
      val report = playEventRepository.annualReport(year)
      _state.value = _state.value.copy(
        report = report,
        previousReport = previousReportOf(years, year),
        paletteColors = paletteColorsOf(report),
        bestOverrides = bestOverridesOf(year),
        loading = false
      )
    }
  }

  /** S4 年度之最：读取用户这一年的覆盖选择。 */
  private suspend fun bestOverridesOf(year: Int): Map<String, String> =
    reportOverrideDao.byYear(year).associate { it.slot to it.canonicalId }

  /**
   * S4 年度之最：把某个槽位换成候选列表里的下一项，并落库。
   * 候选顺序固定（按播放量降序取前 5），因此"换一个"是可预期的循环。
   */
  fun swapBest(slot: String) {
    viewModelScope.launch {
      val year = _state.value.year ?: return@launch
      val report = _state.value.report ?: return@launch
      val candidates = bestCandidates(report, slot)
      if (candidates.isEmpty()) return@launch

      val currentKey = _state.value.bestOverrides[slot]
      val currentIndex = candidates.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)
      val next = candidates[(currentIndex + 1) % candidates.size]

      reportOverrideDao.upsert(
        ReportOverride(
          year = year,
          slot = slot,
          canonicalId = next.key,
          audioId = next.audioId,
          title = next.title,
          artist = next.artist,
          updatedAt = System.currentTimeMillis()
        )
      )
      _state.value = _state.value.copy(
        bestOverrides = _state.value.bestOverrides + (slot to next.key)
      )
    }
  }

  /** 某个槽位的候选（最多 5 个）。专辑/歌手没有 canonicalId，用名字作为 key。 */
  private fun bestCandidates(report: AnnualReport, slot: String): List<BestCandidate> = when (slot) {
    "song" -> report.topSongs.take(5).map {
      BestCandidate(it.canonicalId, it.audioId, it.title, it.artist)
    }

    "album" -> report.topAlbums.take(5).map {
      BestCandidate(it.name, null, it.name, "")
    }

    "artist" -> report.topArtists.take(5).map {
      BestCandidate(it.name, null, it.name, "")
    }

    else -> emptyList()
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

  /** P2：生成年度报告海报（预览用）。 */
  fun generatePoster() {
    viewModelScope.launch {
      val report = _state.value.report ?: return@launch
      if (report.plays <= 0) {
        MessageNotifier.show(R.string.no_play_stat_data)
        return@launch
      }
      val bitmap = withContext(Dispatchers.IO) {
        ReportPoster.render(
          context,
          report,
          ReportStory.analyze(report),
          reportTokensFor(_state.value.paletteColors)
        )
      }
      _state.value = _state.value.copy(posterBitmap = bitmap)
    }
  }

  fun dismissPoster() {
    _state.value = _state.value.copy(posterBitmap = null)
  }

  /** P2：把海报存盘并返回分享 Intent。 */
  fun sharePoster() {
    viewModelScope.launch {
      val bitmap = _state.value.posterBitmap ?: return@launch
      val year = _state.value.year ?: return@launch
      val intent = withContext(Dispatchers.IO) { savePoster(bitmap, year) }
      if (intent == null) {
        MessageNotifier.show(R.string.import_events_failed)
      } else {
        _state.value = _state.value.copy(sharePosterIntent = intent)
      }
    }
  }

  fun consumeSharePosterIntent() {
    _state.value = _state.value.copy(sharePosterIntent = null)
  }

  private fun savePoster(bitmap: Bitmap, year: Int): Intent? {
    return runCatching {
      val dir = File(context.externalCacheDir ?: context.cacheDir, "share").apply { mkdirs() }
      val file = File(dir, "annual-report-" + year + ".png")
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }
      Util.createShareImageFileIntent(file, context)
    }.getOrNull()
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
  /** S2 音乐颜色：按 Top 歌曲顺序的专辑封面主色（去重）。 */
  val paletteColors: List<Int> = emptyList(),
  /** 年度之最的用户覆盖选择：slot → 选项 key。 */
  val bestOverrides: Map<String, String> = emptyMap(),
  val posterBitmap: Bitmap? = null,
  val sharePosterIntent: Intent? = null,
  val loading: Boolean = true,
  val exportIntent: Intent? = null,
  val exportRequestId: Int = 0
)
