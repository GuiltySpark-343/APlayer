package remix.myplayer.ui.screen.report

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.ui.screen.report.pages.PageCalendar
import remix.myplayer.ui.screen.report.pages.PageColors
import remix.myplayer.ui.screen.report.pages.PageCover
import remix.myplayer.ui.screen.report.pages.PageGenreEvolution
import remix.myplayer.ui.screen.report.pages.PageKeywords
import remix.myplayer.ui.screen.report.pages.PageOverview
import remix.myplayer.ui.screen.report.pages.PagePeakHour
import remix.myplayer.ui.screen.report.pages.PagePlaceholder
import remix.myplayer.ui.screen.report.pages.PageRepeatExplore
import remix.myplayer.ui.screen.report.pages.PageShare
import remix.myplayer.ui.screen.report.pages.PageSources
import remix.myplayer.ui.screen.report.pages.PageYearBest
import remix.myplayer.viewmodel.AnnualReportViewModel

/**
 * 页面注册表。新增页面只改这里。
 *
 * 顺序即叙事顺序；`show` 为 false 的页整页不出现（对应"数据不足则不展示"）。
 * 不要放永不展示的占位条目——未实现的页等到对应 Task 再加。
 */
object StoryPages {

  data class Entry(val id: String, val show: (AnnualReport) -> Boolean)

  val entries: List<Entry> = listOf(
    Entry("cover", { true }),
    Entry("overview", { it.plays > 0 }),
    Entry("colors", { it.topSongs.isNotEmpty() }),
    Entry("genre_evolution", { it.genreByQuarter.isNotEmpty() }),
    Entry(
      "year_best",
      { it.topSongs.isNotEmpty() || it.topAlbums.isNotEmpty() || it.topArtists.isNotEmpty() }
    ),
    Entry("peak_hour", { it.hourDistribution.isNotEmpty() }),
    Entry("night", { lateNightRatio(it) >= 0.02 }),
    Entry("calendar", { it.dailyDistribution.isNotEmpty() }),
    Entry("loop_king", { it.loopTop.isNotEmpty() }),
    Entry("repeat_explore", { it.plays > 0 }),
    Entry("sources", { it.sourceBreakdown.isNotEmpty() }),
    Entry("keywords", { true }),
    Entry("share", { true })
  )

  /** 当前数据下实际展示的页面。渲染与计数都必须走这里，避免两处条件不一致。 */
  fun visible(report: AnnualReport): List<Entry> = entries.filter { it.show(report) }

  private fun lateNightRatio(report: AnnualReport): Double {
    val plays = report.plays
    if (plays <= 0) return 0.0
    return report.hourDistribution.filter { it.hour in 0..5 }.sumOf { it.plays }.toDouble() / plays
  }
}

/** 页面 index → 组件。索引必须基于 [StoryPages.visible]，与 StoryPager 的 pageCount 同源。 */
@Composable
fun renderStoryPage(
  index: Int,
  report: AnnualReport,
  story: ReportStoryResult,
  context: Context,
  nav: NavController,
  viewModel: AnnualReportViewModel,
  paletteColors: List<Int>,
  bestOverrides: Map<String, String>
) {
  val id = StoryPages.visible(report).getOrNull(index)?.id
  when (id) {
    "cover" -> PageCover(report, story)
    "overview" -> PageOverview(report, context)
    "colors" -> PageColors(paletteColors)
    "peak_hour" -> PagePeakHour(report)
    "calendar" -> PageCalendar(report)
    "repeat_explore" -> PageRepeatExplore(story)
    "sources" -> PageSources(report)
    "genre_evolution" -> PageGenreEvolution(report, context)
    "year_best" -> PageYearBest(report, bestOverrides) { slot -> viewModel.swapBest(slot) }
    "keywords" -> PageKeywords(report, story, context)
    "share" -> PageShare(viewModel, nav)
    else -> PagePlaceholder(id ?: "")
  }
}
