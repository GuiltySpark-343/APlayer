@file:OptIn(ExperimentalMaterial3Api::class)

package remix.myplayer.ui.screen.report

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.dao.DayCount
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.data.model.report.SongMoment
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.viewmodel.annualReportViewModel
import java.util.Calendar

@Composable
fun AnnualReportScreen() {
  val viewModel = annualReportViewModel
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.load()
  }

  LaunchedEffect(state.exportIntent) {
    val intent = state.exportIntent
    if (intent != null) {
      context.startActivity(Intent.createChooser(intent, null))
      viewModel.consumeExportIntent()
    }
  }

  BackHandler { viewModel.consumeExportIntent() }

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.annual_report), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    val report = state.report
    if (report == null) {
      Column(
        modifier = Modifier
          .padding(contentPadding)
          .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        TextSecondary(text = stringResource(R.string.no_play_stat_data), fontSize = 16.sp)
      }
      return@Scaffold
    }

    LazyColumn(modifier = Modifier.padding(contentPadding)) {
      item {
        if (state.years.size > 1) {
          YearSelector(
            years = state.years,
            selected = state.year ?: report.year,
            onSelect = viewModel::selectYear
          )
        }
        Spacer(Modifier.height(8.dp))
        MetricCard(report)
        Spacer(Modifier.height(8.dp))
        ExtraMetricCard(report)
        state.previousReport?.let {
          Spacer(Modifier.height(8.dp))
          CompareCard(report, it)
        }
        Spacer(Modifier.height(8.dp))
        MomentsCard(report)
        Spacer(Modifier.height(8.dp))
        TopSongsCard(report)
        Spacer(Modifier.height(8.dp))
        TopArtistsCard(report)
        Spacer(Modifier.height(8.dp))
        TopAlbumsCard(report)
        Spacer(Modifier.height(8.dp))
        TrendCard(report)
        Spacer(Modifier.height(8.dp))
        HourCard(report)
        Spacer(Modifier.height(8.dp))
        WeekdayCard(report)
        Spacer(Modifier.height(8.dp))
        HeatmapCard(report)
        Spacer(Modifier.height(8.dp))
        LateNightCard(report)
        Spacer(Modifier.height(8.dp))
        LoopCard(report)
        Spacer(Modifier.height(8.dp))
        GenreCard(report)
        Spacer(Modifier.height(8.dp))
        SourceCard(report)
        Spacer(Modifier.height(8.dp))
        ActionRow(
          onGeneratePlaylist = viewModel::generatePlaylist,
          onExport = viewModel::exportJsonl,
          onClear = viewModel::clear
        )
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun YearSelector(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
  LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
    items(years) { year ->
      val isSelected = year == selected
      TextPrimary(
        text = year.toString(),
        fontSize = 16.sp,
        color = if (isSelected) LocalTheme.current.primary else LocalTheme.current.textSecondary,
        modifier = Modifier
          .padding(8.dp)
          .clickable { onSelect(year) }
      )
    }
  }
}

@Composable
private fun MetricCard(report: AnnualReport) {
  SectionCard {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      MetricItem(stringResource(R.string.stat_plays), report.plays.toString())
      MetricItem(stringResource(R.string.stat_listen_score), "%.1f".format(report.listenScore))
      MetricItem(stringResource(R.string.stat_completed), report.completedPlays.toString())
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_listen_ms), formatTime(report.listenMs))
      MetricItem(stringResource(R.string.stat_days), report.listenedDays.toString())
      MetricItem(stringResource(R.string.stat_first_listened), report.firstListenedSongs.toString())
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_songs), report.distinctSongs.toString())
      MetricItem(stringResource(R.string.stat_artists), report.distinctArtists.toString())
      MetricItem(stringResource(R.string.stat_albums), report.distinctAlbums.toString())
    }
  }
}

@Composable
private fun ExtraMetricCard(report: AnnualReport) {
  val plays = report.plays
  val skipRate = if (plays > 0) report.skippedPlays.toDouble() / plays else 0.0
  val completeRate = if (plays > 0) report.completedPlays.toDouble() / plays else 0.0
  val repeat = if (report.distinctSongs > 0) plays.toDouble() / report.distinctSongs else 0.0
  val explore =
    if (report.distinctSongs > 0) report.firstListenedSongs.toDouble() / report.distinctSongs else 0.0

  SectionCard {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      MetricItem(stringResource(R.string.stat_added_songs), report.addedSongs.toString())
      MetricItem(stringResource(R.string.stat_skip_rate), formatPercent(skipRate))
      MetricItem(stringResource(R.string.stat_complete_rate), formatPercent(completeRate))
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_repeat), "%.2f".format(repeat))
      MetricItem(stringResource(R.string.stat_explore), formatPercent(explore))
      MetricItem(
        stringResource(R.string.stat_streak),
        longestStreak(report.year, report.dailyDistribution).toString()
      )
    }
  }
}

@Composable
private fun CompareCard(current: AnnualReport, previous: AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_compare) + " (" + previous.year + ")") {
    CompareRow(stringResource(R.string.stat_plays), previous.plays.toLong(), current.plays.toLong())
    CompareRow(
      stringResource(R.string.stat_listen_ms),
      previous.listenMs,
      current.listenMs
    ) { formatTime(it) }
    CompareRow(
      stringResource(R.string.stat_completed),
      previous.completedPlays.toLong(),
      current.completedPlays.toLong()
    )
    CompareRow(
      stringResource(R.string.stat_songs),
      previous.distinctSongs.toLong(),
      current.distinctSongs.toLong()
    )
  }
}

@Composable
private fun CompareRow(
  label: String,
  lastValue: Long,
  thisValue: Long,
  format: (Long) -> String = { it.toString() }
) {
  val delta = thisValue - lastValue
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextSecondary(text = label, fontSize = 13.sp, modifier = Modifier.weight(1f))
    TextPrimary(text = format(lastValue), fontSize = 13.sp)
    TextSecondary(text = " -> ", fontSize = 13.sp)
    TextPrimary(text = format(thisValue), fontSize = 13.sp)
    TextSecondary(
      text = if (delta >= 0) "  +" + format(delta) else "  " + format(delta),
      fontSize = 12.sp
    )
  }
}

@Composable
private fun MomentsCard(report: AnnualReport) {
  if (report.firstPlay == null && report.lastPlay == null) return
  SectionCard {
    report.firstPlay?.let { MomentRow(stringResource(R.string.stat_first_song), it) }
    if (report.firstPlay != null && report.lastPlay != null) {
      Spacer(Modifier.height(8.dp))
    }
    report.lastPlay?.let { MomentRow(stringResource(R.string.stat_last_song), it) }
  }
}

@Composable
private fun MomentRow(label: String, moment: SongMoment) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TextSecondary(text = label, fontSize = 12.sp)
    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
      TextPrimary(text = moment.title, fontSize = 15.sp)
      if (moment.artist.isNotBlank()) {
        TextSecondary(text = moment.artist, fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    TextPrimary(text = value, fontSize = 20.sp)
    TextSecondary(text = label, fontSize = 12.sp)
  }
}

@Composable
private fun TopSongsCard(report: AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_songs)) {
    report.topSongs.forEachIndexed { index, item ->
      RankRow(index + 1, item.title, item.artist, item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun TopArtistsCard(report: AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_artists)) {
    report.topArtists.forEachIndexed { index, item ->
      RankRow(index + 1, item.name, "", item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun TopAlbumsCard(report: AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_albums)) {
    report.topAlbums.forEachIndexed { index, item ->
      RankRow(index + 1, item.name, "", item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, listenMs: Long, plays: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextPrimary(text = rank.toString(), fontSize = 16.sp, color = LocalTheme.current.secondary)
    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
      TextPrimary(text = title, fontSize = 15.sp)
      if (subtitle.isNotBlank()) {
        TextSecondary(text = subtitle, fontSize = 12.sp)
      }
    }
    Column(horizontalAlignment = Alignment.End) {
      TextPrimary(text = formatTime(listenMs), fontSize = 13.sp)
      TextSecondary(text = plays.toString() + " x", fontSize = 12.sp)
    }
  }
}

@Composable
private fun TrendCard(report: AnnualReport) {
  if (report.monthDistribution.isEmpty()) return
  val maxMs = report.monthDistribution.maxOf { it.listenedMs }.coerceAtLeast(1L)
  val byMonth = HashMap<Int, Long>()
  report.monthDistribution.forEach { byMonth[it.month] = it.listenedMs }
  SectionCard(title = stringResource(R.string.stat_trend)) {
    Row(
      modifier = Modifier.fillMaxWidth().height(64.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      (1..12).forEach { month ->
        val ms = byMonth[month] ?: 0L
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .width(8.dp)
              .height((44f * ms / maxMs).coerceAtLeast(if (ms > 0) 2f else 1f).dp)
              .background(
                if (ms > 0) LocalTheme.current.primary else LocalTheme.current.textSecondary
              )
          )
          TextSecondary(text = month.toString(), fontSize = 8.sp)
        }
      }
    }
  }
}

@Composable
private fun HourCard(report: AnnualReport) {
  if (report.hourDistribution.isEmpty()) return
  val counts = IntArray(24)
  report.hourDistribution.forEach { if (it.hour in 0..23) counts[it.hour] = it.plays }
  val maxPlays = counts.max().coerceAtLeast(1)

  SectionCard(title = stringResource(R.string.stat_hours)) {
    Row(
      modifier = Modifier.fillMaxWidth().height(64.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      counts.forEachIndexed { hour, count ->
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .width(5.dp)
              .height((44f * count / maxPlays).coerceAtLeast(if (count > 0) 2f else 1f).dp)
              .background(
                if (count > 0) LocalTheme.current.primary else LocalTheme.current.textSecondary
              )
          )
          TextSecondary(text = if (hour % 6 == 0) hour.toString() else "", fontSize = 9.sp)
        }
      }
    }
  }
}

@Composable
private fun WeekdayCard(report: AnnualReport) {
  if (report.weekdayDistribution.isEmpty()) return
  val labels = listOf(
    stringResource(R.string.weekday_sun),
    stringResource(R.string.weekday_mon),
    stringResource(R.string.weekday_tue),
    stringResource(R.string.weekday_wed),
    stringResource(R.string.weekday_thu),
    stringResource(R.string.weekday_fri),
    stringResource(R.string.weekday_sat)
  )
  val byWeekday = HashMap<Int, Int>()
  report.weekdayDistribution.forEach { byWeekday[it.weekday] = it.plays }
  val maxPlays = (1..7).maxOf { byWeekday[it] ?: 0 }.coerceAtLeast(1)

  SectionCard(title = stringResource(R.string.stat_weekday)) {
    Row(
      modifier = Modifier.fillMaxWidth().height(56.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      (1..7).forEach { weekday ->
        val count = byWeekday[weekday] ?: 0
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .width(14.dp)
              .height((36f * count / maxPlays).coerceAtLeast(if (count > 0) 2f else 1f).dp)
              .background(
                if (count > 0) LocalTheme.current.primary else LocalTheme.current.textSecondary
              )
          )
          TextSecondary(text = labels[weekday - 1], fontSize = 10.sp)
        }
      }
    }
  }
}

@Composable
private fun HeatmapCard(report: AnnualReport) {
  if (report.dailyDistribution.isEmpty()) return
  val byDay = HashMap<Pair<Int, Int>, Long>()
  report.dailyDistribution.forEach { byDay[it.month to it.day] = it.listenedMs }
  val maxMs = report.dailyDistribution.maxOf { it.listenedMs }.coerceAtLeast(1L)
  val daysInMonth = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

  SectionCard(title = stringResource(R.string.stat_heatmap)) {
    for (month in 1..12) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextSecondary(
          text = "%02d".format(month),
          fontSize = 9.sp,
          modifier = Modifier.width(20.dp)
        )
        for (day in 1..daysInMonth[month - 1]) {
          val ms = byDay[month to day] ?: 0L
          val alpha = if (ms <= 0) 0.08f else (0.25f + 0.75f * ms / maxMs).toFloat()
          Box(
            modifier = Modifier
              .padding(0.5.dp)
              .size(7.dp)
              .background(LocalTheme.current.primary.copy(alpha = alpha))
          )
        }
      }
    }
  }
}

@Composable
private fun LateNightCard(report: AnnualReport) {
  val late = report.hourDistribution.filter { it.hour in 0..5 }
  val latePlays = late.sumOf { it.plays }
  val lateMs = late.sumOf { it.listenedMs }
  if (latePlays == 0) return
  val ratio = if (report.plays > 0) latePlays.toDouble() / report.plays else 0.0

  SectionCard(title = stringResource(R.string.stat_late_night)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      MetricItem(stringResource(R.string.stat_late_night_ratio), formatPercent(ratio))
      MetricItem(stringResource(R.string.stat_listen_ms), formatTime(lateMs))
      MetricItem(stringResource(R.string.stat_plays), latePlays.toString())
    }
    if (report.lateNightTopSongs.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      TextSecondary(text = stringResource(R.string.stat_late_night_top), fontSize = 12.sp)
      report.lateNightTopSongs.take(3).forEachIndexed { index, item ->
        RankRow(index + 1, item.title, item.artist, item.listenedMs, item.plays)
      }
    }
  }
}

@Composable
private fun LoopCard(report: AnnualReport) {
  if (report.loopTop.isEmpty()) return
  SectionCard(title = stringResource(R.string.stat_loop_top)) {
    report.loopTop.take(5).forEachIndexed { index, item ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextPrimary(
          text = (index + 1).toString(),
          fontSize = 16.sp,
          color = LocalTheme.current.secondary
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
          TextPrimary(text = item.title, fontSize = 15.sp)
          if (item.artist.isNotBlank()) {
            TextSecondary(text = item.artist, fontSize = 12.sp)
          }
        }
        TextPrimary(text = item.loops.toString() + " x", fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun GenreCard(report: AnnualReport) {
  if (report.genreBreakdown.isEmpty()) return
  SectionCard(title = stringResource(R.string.stat_genre)) {
    report.genreBreakdown.forEach { g ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TextPrimary(text = g.genre, fontSize = 14.sp)
        TextSecondary(
          text = g.plays.toString() + "  ·  " + formatTime(g.listenedMs),
          fontSize = 12.sp
        )
      }
    }
  }
}

@Composable
private fun SourceCard(report: AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_sources)) {
    report.sourceBreakdown.forEach { s ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TextPrimary(text = sourceLabel(s.source), fontSize = 14.sp)
        TextSecondary(text = s.plays.toString(), fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun sourceLabel(source: String): String = when (source) {
  "SEARCH_CLICK" -> stringResource(R.string.source_search)
  "LIBRARY_CLICK" -> stringResource(R.string.source_library)
  "PLAYLIST_CLICK" -> stringResource(R.string.source_playlist)
  "QUEUE_AUTO" -> stringResource(R.string.source_queue_auto)
  "RESUME" -> stringResource(R.string.source_resume)
  "EXTERNAL_INTENT" -> stringResource(R.string.source_external)
  else -> source
}

@Composable
private fun ActionRow(
  onGeneratePlaylist: () -> Unit,
  onExport: () -> Unit,
  onClear: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
    SectionButton(text = stringResource(R.string.generate_playlist), onClick = onGeneratePlaylist)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      SectionButton(text = stringResource(R.string.export_jsonl), onClick = onExport)
      SectionButton(text = stringResource(R.string.clear_play_stats), onClick = onClear)
    }
  }
}

@Composable
private fun SectionButton(text: String, onClick: () -> Unit) {
  androidx.compose.material3.Text(
    text = text,
    color = LocalTheme.current.primary,
    fontSize = 14.sp,
    modifier = Modifier
      .padding(vertical = 12.dp)
      .clickable { onClick() }
  )
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    if (title != null) {
      TextPrimary(text = title, fontSize = 16.sp)
      Spacer(Modifier.height(6.dp))
    }
    content()
  }
}

private fun formatTime(ms: Long): String {
  val minutes = ms / 60000.0
  return if (minutes < 60) {
    "%.1f min".format(minutes)
  } else {
    "%.1f h".format(minutes / 60.0)
  }
}

private fun formatPercent(value: Double): String = "%.0f%%".format(value * 100)

private fun longestStreak(year: Int, days: List<DayCount>): Int {
  if (days.isEmpty()) return 0
  val set = days.map { it.month to it.day }.toHashSet()
  val cal = Calendar.getInstance().apply {
    clear()
    set(year, Calendar.JANUARY, 1)
  }
  val daysInYear = if (cal.getActualMaximum(Calendar.DAY_OF_YEAR) >= 366) 366 else 365
  var best = 0
  var current = 0
  repeat(daysInYear) {
    val key = (cal.get(Calendar.MONTH) + 1) to cal.get(Calendar.DAY_OF_MONTH)
    if (set.contains(key)) {
      current++
      if (current > best) best = current
    } else {
      current = 0
    }
    cal.add(Calendar.DAY_OF_MONTH, 1)
  }
  return best
}
