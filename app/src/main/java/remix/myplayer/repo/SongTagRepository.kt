package remix.myplayer.repo

import remix.myplayer.data.db.room.entity.SongTag
import remix.myplayer.data.db.room.entity.TagDef

/**
 * 歌曲语义标签仓库（T0 范围）。
 *
 * 与音频文件的 ID3 标签无关：数据全部存在本地 DB，以 canonicalId 关联歌曲。
 * 聚合类只读接口（情绪分布、场景 TOP 等）属于 T2，不在本接口内。
 */
interface SongTagRepository {

  suspend fun allTags(): List<TagDef>

  suspend fun tagsOf(canonicalId: String): List<SongTag>

  suspend fun tagSong(
    canonicalId: String,
    tagId: String,
    score: Double? = null,
    source: String = SongTag.SOURCE_USER
  )

  /** 取消打标：写墓碑（不物理删除），保留跨端合并语义。 */
  suspend fun untagSong(canonicalId: String, tagId: String)

  suspend fun songsOf(tagId: String): List<String>

  suspend fun usageCounts(): Map<String, Int>

  suspend fun countActiveOfTag(tagId: String): Int

  /** 新建用户标签，返回生成的稳定 tagId。同名重复调用返回同一个 tagId。 */
  suspend fun createTag(
    name: String,
    category: String,
    kind: String = TagDef.KIND_BOOLEAN,
    colorArgb: Int? = null
  ): String

  suspend fun renameTag(tagId: String, newName: String)

  suspend fun updateColor(tagId: String, colorArgb: Int?)

  /** 合并同义词：把 from 的所有关系迁移到 into，然后删除 from 定义。 */
  suspend fun mergeTag(fromTagId: String, intoTagId: String)

  /** 删除标签：先给它名下的关系写墓碑，再删除定义。 */
  suspend fun deleteTag(tagId: String)

  /**
   * 打「情绪词」：同时写入该词本身与其 (valence, arousal) 预设坐标 —— 即设计文档 §4.6 的
   * 「点一下即打分」。非情绪词（不在 MOOD_PRESETS 内）只写该标签本身。
   */
  suspend fun tagMood(canonicalId: String, moodTagId: String)

  /** 精确设置情绪维度分（面板微调用）；null 表示该维度不改动。 */
  suspend fun setMoodScore(canonicalId: String, valence: Int?, arousal: Int?)
}
