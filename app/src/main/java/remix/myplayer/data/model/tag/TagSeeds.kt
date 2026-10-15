package remix.myplayer.data.model.tag

import remix.myplayer.data.db.room.entity.TagDef

/**
 * 预设标签词表（随版本发布）。
 *
 * 由 `migration12to13` 写入数据库：保证任何设备、任何时间升级后的预设 tagId 集合一致，
 * 且重复执行幂等（INSERT OR IGNORE）。
 *
 * 情绪词自带 (valence, arousal) 预设坐标 —— 打标面板「点一下即打分」就写这组值，
 * 需要精确的用户可在面板里展开 5 档滑条覆盖。见 docs/song-tag-system-design.md §4.6。
 */
object TagSeeds {

  /** 确定性时间戳：避免每次迁移写入不同值，便于断言与跨端比对。 */
  const val CREATED_AT = 0L

  private const val COLOR_CONTEXT = 0xFF6FA8DC.toInt()
  private const val COLOR_MOOD = 0xFFE06666.toInt()
  private const val COLOR_OBJECTIVE = 0xFF9AA0A6.toInt()

  data class Seed(
    val tagId: String,
    val name: String,
    val alias: String?,
    val category: String,
    val kind: String,
    val colorArgb: Int?,
    val sortOrder: Int
  )

  private fun context(tagId: String, name: String, alias: String, order: Int) = Seed(
    tagId = tagId,
    name = name,
    alias = alias,
    category = TagDef.CATEGORY_CONTEXT,
    kind = TagDef.KIND_BOOLEAN,
    colorArgb = COLOR_CONTEXT,
    sortOrder = order
  )

  private fun mood(tagId: String, name: String, alias: String, order: Int) = Seed(
    tagId = tagId,
    name = name,
    alias = alias,
    category = TagDef.CATEGORY_MOOD,
    kind = TagDef.KIND_BOOLEAN,
    colorArgb = COLOR_MOOD,
    sortOrder = order
  )

  private fun objective(tagId: String, name: String, alias: String, order: Int) = Seed(
    tagId = tagId,
    name = name,
    alias = alias,
    category = TagDef.CATEGORY_OBJECTIVE,
    kind = TagDef.KIND_BOOLEAN,
    colorArgb = COLOR_OBJECTIVE,
    sortOrder = order
  )

  val PRESET_TAGS: List<Seed> = buildList {
    // ---- 情境（12）----
    add(context("ctx.home", "回家", "home", 0))
    add(context("ctx.commute", "通勤", "commute", 1))
    add(context("ctx.outdoor", "户外运动", "outdoor", 2))
    add(context("ctx.workout", "健身", "workout", 3))
    add(context("ctx.focus", "专注工作", "focus", 4))
    add(context("ctx.study", "学习", "study", 5))
    add(context("ctx.sleep", "睡前", "sleep", 6))
    add(context("ctx.latenight", "深夜", "late-night", 7))
    add(context("ctx.rain", "雨天", "rain", 8))
    add(context("ctx.party", "聚会", "party", 9))
    add(context("ctx.drive", "开车", "drive", 10))
    add(context("ctx.cafe", "咖啡馆", "cafe", 11))

    // ---- 情绪词（12），坐标见 MOOD_PRESETS ----
    add(mood("mood.happy", "开心", "happy", 100))
    add(mood("mood.energetic", "亢奋", "energetic", 101))
    add(mood("mood.passionate", "热烈", "passionate", 102))
    add(mood("mood.burn", "燃", "burn", 103))
    add(mood("mood.healing", "治愈", "healing", 104))
    add(mood("mood.gentle", "温柔", "gentle", 105))
    add(mood("mood.romantic", "浪漫", "romantic", 106))
    add(mood("mood.thoughtful", "引人思考", "thoughtful", 107))
    add(mood("mood.nostalgic", "怀旧", "nostalgic", 108))
    add(mood("mood.depressive", "致郁", "depressive", 109))
    add(mood("mood.low", "低沉", "low", 110))
    add(mood("mood.lonely", "孤独", "lonely", 111))

    // ---- 情绪维度（2，SCALE）----
    add(Seed("mood.valence", "情绪正负", "valence", TagDef.CATEGORY_MOOD, TagDef.KIND_SCALE, COLOR_MOOD, 200))
    add(Seed("mood.arousal", "唤醒强度", "arousal", TagDef.CATEGORY_MOOD, TagDef.KIND_SCALE, COLOR_MOOD, 201))

    // ---- 客观（8）----
    add(objective("obj.instrumental", "器乐", "instrumental", 300))
    add(objective("obj.vocal", "人声", "vocal", 301))
    add(objective("obj.bpm.slow", "慢速", "slow", 310))
    add(objective("obj.bpm.mid", "中速", "medium", 311))
    add(objective("obj.bpm.fast", "快速", "fast", 312))
    add(objective("obj.energy.low", "低能量", "low-energy", 320))
    add(objective("obj.energy.mid", "中能量", "mid-energy", 321))
    add(objective("obj.energy.high", "高能量", "high-energy", 322))
  }

  /** 情绪词 → (valence, arousal) 预设坐标；点一下 chip 即按此写分。 */
  val MOOD_PRESETS: Map<String, Pair<Int, Int>> = mapOf(
    "mood.happy" to (5 to 4),
    "mood.energetic" to (4 to 5),
    "mood.passionate" to (4 to 5),
    "mood.burn" to (3 to 5),
    "mood.healing" to (4 to 2),
    "mood.gentle" to (4 to 2),
    "mood.romantic" to (4 to 3),
    "mood.thoughtful" to (3 to 2),
    "mood.nostalgic" to (3 to 2),
    "mood.depressive" to (2 to 2),
    "mood.low" to (2 to 2),
    "mood.lonely" to (2 to 2)
  )

  const val TAG_VALENCE = "mood.valence"
  const val TAG_AROUSAL = "mood.arousal"
}
