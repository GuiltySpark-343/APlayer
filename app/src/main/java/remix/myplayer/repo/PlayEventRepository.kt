package remix.myplayer.repo

import remix.myplayer.data.db.room.dao.PlayEventDao
import remix.myplayer.data.db.room.entity.PlayEvent
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.data.model.report.SongMoment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放事件仓库：负责写入原始事件以及按年聚合。
 */
interface PlayEventRepository {

  suspend fun record(event: PlayEvent)

  /** 若该 canonicalId 尚无 song_added 事件，则登记一条。 */
  suspend fun recordSongAddedIfAbsent(event: PlayEvent)

  /** M3：导入事件（按 eventId 幂等），返回实际写入条数。 */
  suspend fun importEvents(events: List<PlayEvent>): Int

  suspend fun availableYears(): List<Int>

  suspend fun annualReport(year: Int): AnnualReport

  suspend fun eventsOf(year: Int): List<PlayEvent>

  suspend fun clear()
}

@Singleton
class PlayEventRepoImpl @Inject constructor(
  private val playEventDao: PlayEventDao,
) : PlayEventRepository {

  override suspend fun record(event: PlayEvent) {
    playEventDao.insert(event)
  }

  override suspend fun recordSongAddedIfAbsent(event: PlayEvent) {
    if (playEventDao.countSongAdded(event.canonicalId) == 0) {
      playEventDao.insert(event)
    }
  }

  override suspend fun importEvents(events: List<PlayEvent>): Int {
    var imported = 0
    events.forEach { event ->
      if (playEventDao.insertIgnore(event) != -1L) {
        imported++
      }
    }
    return imported
  }

  override suspend fun availableYears(): List<Int> = playEventDao.distinctYears()

  override suspend fun eventsOf(year: Int): List<PlayEvent> = playEventDao.selectByYear(year)

  override suspend fun annualReport(year: Int): AnnualReport {
    return AnnualReport(
      year = year,
      plays = playEventDao.countByYear(year),
      listenScore = playEventDao.sumPlayScore(year),
      completedPlays = playEventDao.countCompleted(year),
      listenMs = playEventDao.sumListenMs(year),
      listenedDays = playEventDao.countListenDays(year),
      distinctSongs = playEventDao.countDistinctSongs(year),
      distinctArtists = playEventDao.countDistinctArtists(year),
      distinctAlbums = playEventDao.countDistinctAlbums(year),
      firstListenedSongs = playEventDao.countFirstListenedSongs(year),
      addedSongs = playEventDao.countAddedSongs(year),
      skippedPlays = playEventDao.countSkipped(year),
      firstPlay = playEventDao.firstEvent(year)?.toMoment(),
      lastPlay = playEventDao.lastEvent(year)?.toMoment(),
      topSongs = playEventDao.topSongs(year, TOP_LIMIT),
      topArtists = playEventDao.topArtists(year, TOP_LIMIT),
      topAlbums = playEventDao.topAlbums(year, TOP_LIMIT),
      monthDistribution = playEventDao.monthDistribution(year),
      hourDistribution = playEventDao.hourDistribution(year),
      sourceBreakdown = playEventDao.sourceBreakdown(year),
      weekdayDistribution = playEventDao.weekdayDistribution(year),
      dailyDistribution = playEventDao.dailyDistribution(year),
      loopTop = playEventDao.loopRanking(year, TOP_LIMIT),
      genreBreakdown = playEventDao.genreBreakdown(year, TOP_LIMIT),
      lateNightTopSongs = playEventDao.lateNightTopSongs(year, TOP_LIMIT)
    )
  }

  override suspend fun clear() {
    playEventDao.clear()
  }

  private fun PlayEvent.toMoment(): SongMoment = SongMoment(
    title = titleSnapshot,
    artist = artistSnapshot,
    album = albumSnapshot,
    at = startedAt
  )

  companion object {
    private const val TOP_LIMIT = 10
  }
}
