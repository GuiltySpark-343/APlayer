package remix.myplayer.ui.screen.report

import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport

/** P2：年度关键词。 */
enum class ReportKeyword(val titleRes: Int) {
  NIGHT_OWL(R.string.kw_night_owl),
  LOOP_LOVER(R.string.kw_loop_lover),
  EXPLORER(R.string.kw_explorer),
  PICKY_EARS(R.string.kw_picky_ears),
  IMMERSIVE(R.string.kw_immersive),
  CHILL_LISTENER(R.string.kw_chill_listener),
  NEW_CHASER(R.string.kw_new_chaser),
  DEFAULT(R.string.kw_default)
}

/** P2：文案分析结果。 */
data class ReportStoryResult(
  val keywords: List<ReportKeyword>,
  val topMonth: Int,
  val peakHour: Int,
  val lateNightRatio: Double,
  val repeat: Double,
  val explore: Double,
  val skipRate: Double,
  val completeRate: Double,
  val autoRatio: Double
)

/**
 * P2：年度关键词与文案的规则引擎。
 *
 * 纯本地、确定性：同一份数据永远得到同样的结果，不依赖网络。
 */
object ReportStory {

  fun analyze(report: AnnualReport): ReportStoryResult {
    val plays = report.plays.coerceAtLeast(0)

    val latePlays = report.hourDistribution.filter { it.hour in 0..5 }.sumOf { it.plays }
    val lateRatio = if (plays > 0) latePlays.toDouble() / plays else 0.0

    val repeat = if (report.distinctSongs > 0) {
      plays.toDouble() / report.distinctSongs
    } else {
      0.0
    }
    val explore = if (report.distinctSongs > 0) {
      report.firstListenedSongs.toDouble() / report.distinctSongs
    } else {
      0.0
    }
    val skipRate = if (plays > 0) report.skippedPlays.toDouble() / plays else 0.0
    val completeRate = if (plays > 0) report.completedPlays.toDouble() / plays else 0.0

    val autoPlays = report.sourceBreakdown.firstOrNull { it.source == "QUEUE_AUTO" }?.plays ?: 0
    val autoRatio = if (plays > 0) autoPlays.toDouble() / plays else 0.0

    val topMonth = report.monthDistribution.maxByOrNull { it.listenedMs }?.month ?: 0
    val peakHour = report.hourDistribution.maxByOrNull { it.plays }?.hour ?: 0

    val scored = ArrayList<Pair<ReportKeyword, Double>>()
    if (lateRatio >= 0.25) scored.add(ReportKeyword.NIGHT_OWL to lateRatio)
    if (repeat >= 3.0) scored.add(ReportKeyword.LOOP_LOVER to repeat / 10.0)
    if (explore >= 0.6) scored.add(ReportKeyword.EXPLORER to explore)
    if (skipRate >= 0.4) scored.add(ReportKeyword.PICKY_EARS to skipRate)
    if (completeRate >= 0.7) scored.add(ReportKeyword.IMMERSIVE to completeRate)
    if (autoRatio >= 0.6) scored.add(ReportKeyword.CHILL_LISTENER to autoRatio)
    if (report.addedSongs >= 100) scored.add(ReportKeyword.NEW_CHASER to report.addedSongs / 500.0)

    val keywords = scored.sortedByDescending { it.second }.take(2).map { it.first }

    return ReportStoryResult(
      keywords = keywords.ifEmpty { listOf(ReportKeyword.DEFAULT) },
      topMonth = topMonth,
      peakHour = peakHour,
      lateNightRatio = lateRatio,
      repeat = repeat,
      explore = explore,
      skipRate = skipRate,
      completeRate = completeRate,
      autoRatio = autoRatio
    )
  }
}
