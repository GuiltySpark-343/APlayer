package remix.myplayer.ui.screen.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import remix.myplayer.R
import remix.myplayer.data.model.report.AnnualReport

/**
 * P2：年度报告海报渲染器。
 *
 * 用 Canvas 离屏绘制固定尺寸的分享图（不依赖 Compose 生命周期）。
 * 风格固定（不跟随 App 明暗主题），保证任何主题下导出效果一致。
 */
object ReportPoster {

  const val WIDTH = 1080
  const val HEIGHT = 1620

  private const val PAD = 80f

  private const val COLOR_BG_TOP = "#141628"
  private const val COLOR_BG_BOTTOM = "#2A2450"
  private const val COLOR_TEXT = "#FFFFFF"
  private const val COLOR_TEXT_SUB = "#B9BCDA"
  private const val COLOR_ACCENT = "#8C9BFF"
  private const val COLOR_CARD = "#22FFFFFF"
  private const val COLOR_FOOTER = "#7A7E9E"

  fun render(context: Context, report: AnnualReport, story: ReportStoryResult): Bitmap {
    val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawBackground(canvas)

    canvas.drawText(report.year.toString(), PAD, 200f, paint(COLOR_TEXT, 132f, true))
    canvas.drawText(
      context.getString(R.string.poster_subtitle),
      PAD,
      272f,
      paint(COLOR_SUB(), 42f)
    )

    val keywordText = story.keywords.joinToString(" · ") { context.getString(it.titleRes) }
    canvas.drawText(keywordText, PAD, 380f, paint(COLOR_ACCENT, 60f, true))

    val storyPaint = paint("#E6E7F2", 34f)
    var y = 452f
    wrap(buildStory(context, report, story), storyPaint, WIDTH - PAD * 2).forEach { line ->
      canvas.drawText(line, PAD, y, storyPaint)
      y += 48f
    }

    drawMetrics(context, canvas, report, 640f)
    drawList(
      context,
      canvas,
      context.getString(R.string.poster_top_songs),
      report.topSongs.take(3).map { it.title to it.artist },
      880f
    )
    drawList(
      context,
      canvas,
      context.getString(R.string.poster_top_artists),
      report.topArtists.take(3).map { it.name to "" },
      1120f
    )
    drawHeatmap(canvas, report, 1340f)

    canvas.drawText(
      context.getString(R.string.poster_footer),
      PAD,
      HEIGHT - 50f,
      paint(COLOR_FOOTER, 26f)
    )

    return bitmap
  }

  private fun COLOR_SUB(): String = COLOR_TEXT_SUB

  private fun drawBackground(canvas: Canvas) {
    val paint = Paint().apply {
      shader = LinearGradient(
        0f, 0f, 0f, HEIGHT.toFloat(),
        Color.parseColor(COLOR_BG_TOP),
        Color.parseColor(COLOR_BG_BOTTOM),
        Shader.TileMode.CLAMP
      )
    }
    canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
  }

  private fun drawMetrics(context: Context, canvas: Canvas, report: AnnualReport, top: Float) {
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(COLOR_CARD) }
    val valuePaint = paint(COLOR_TEXT, 50f, true)
    val labelPaint = paint(COLOR_TEXT_SUB, 26f)

    val boxWidth = 215f
    val gap = 20f
    val height = 150f

    val plays = report.plays.toString()
    val time = formatDuration(context, report.listenMs)
    val days = report.listenedDays.toString()
    val complete = String.format("%.0f%%", if (report.plays > 0) {
      report.completedPlays * 100.0 / report.plays
    } else {
      0.0
    })

    val items = listOf(
      plays to context.getString(R.string.poster_metric_plays),
      time to context.getString(R.string.poster_metric_time),
      days to context.getString(R.string.poster_metric_days),
      complete to context.getString(R.string.poster_metric_complete)
    )

    items.forEachIndexed { index, item ->
      val left = PAD + index * (boxWidth + gap)
      canvas.drawRoundRect(
        RectF(left, top, left + boxWidth, top + height),
        24f, 24f, cardPaint
      )
      val centerX = left + boxWidth / 2f
      drawCentered(canvas, item.first, centerX, top + 72f, valuePaint)
      drawCentered(canvas, item.second, centerX, top + 118f, labelPaint)
    }
  }

  private fun drawList(
    context: Context,
    canvas: Canvas,
    title: String,
    items: List<Pair<String, String>>,
    top: Float
  ) {
    canvas.drawText(title, PAD, top, paint(COLOR_ACCENT, 36f, true))

    val rankPaint = paint(COLOR_ACCENT, 34f, true)
    val titlePaint = paint(COLOR_TEXT, 36f)
    val subPaint = paint(COLOR_TEXT_SUB, 28f)

    items.forEachIndexed { index, item ->
      val baseline = top + 62f + index * 62f
      canvas.drawText((index + 1).toString(), PAD, baseline, rankPaint)
      canvas.drawText(ellipsize(item.first, titlePaint, 640f), PAD + 60f, baseline, titlePaint)
      if (item.second.isNotEmpty()) {
        canvas.drawText(ellipsize(item.second, subPaint, 320f), PAD + 720f, baseline, subPaint)
      }
    }
  }

  private fun drawHeatmap(canvas: Canvas, report: AnnualReport, top: Float) {
    val byDay = HashMap<Pair<Int, Int>, Long>()
    report.dailyDistribution.forEach { byDay[it.month to it.day] = it.listenedMs }
    val maxMs = report.dailyDistribution.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L
    val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    val cell = 10f
    val gap = 2f
    val baseColor = Color.parseColor(COLOR_ACCENT)
    val cellPaint = Paint()

    for (month in 1..12) {
      for (day in 1..daysInMonth[month - 1]) {
        val ms = byDay[month to day] ?: 0L
        val alpha = if (ms <= 0) 22 else (60 + 195 * ms / maxMs).toInt().coerceAtMost(255)
        cellPaint.color = Color.argb(
          alpha,
          Color.red(baseColor),
          Color.green(baseColor),
          Color.blue(baseColor)
        )
        val left = PAD + (day - 1) * (cell + gap)
        val cellTop = top + (month - 1) * (cell + gap)
        canvas.drawRect(left, cellTop, left + cell, cellTop + cell, cellPaint)
      }
    }
  }

  private fun buildStory(context: Context, report: AnnualReport, story: ReportStoryResult): String {
    return context.getString(
      R.string.story_template,
      formatDuration(context, report.listenMs),
      report.distinctSongs,
      story.peakHour,
      report.firstListenedSongs
    )
  }

  private fun formatDuration(context: Context, ms: Long): String {
    val hours = ms / 3600000.0
    return if (hours >= 1) {
      context.getString(R.string.poster_hours, hours)
    } else {
      context.getString(R.string.poster_minutes, (ms / 60000L).toInt())
    }
  }

  private fun paint(colorHex: String, size: Float, bold: Boolean = false): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor(colorHex)
      textSize = size
      typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

  private fun drawCentered(canvas: Canvas, text: String, centerX: Float, baseline: Float, paint: Paint) {
    canvas.drawText(text, centerX - paint.measureText(text) / 2f, baseline, paint)
  }

  private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
      end--
    }
    return text.substring(0, end) + "…"
  }

  private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = ArrayList<String>()
    val sb = StringBuilder()
    for (ch in text) {
      val candidate = sb.toString() + ch
      if (sb.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
        lines.add(sb.toString())
        sb.setLength(0)
      }
      sb.append(ch)
    }
    if (sb.isNotEmpty()) lines.add(sb.toString())
    return lines
  }
}
