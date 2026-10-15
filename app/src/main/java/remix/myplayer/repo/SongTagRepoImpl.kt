package remix.myplayer.repo

import remix.myplayer.data.db.room.dao.TagDao
import remix.myplayer.data.db.room.entity.SongTag
import remix.myplayer.data.db.room.entity.TagDef
import remix.myplayer.data.model.tag.TagSeeds
import java.security.MessageDigest
import javax.inject.Inject

class SongTagRepoImpl @Inject constructor(
  private val tagDao: TagDao,
) : SongTagRepository {

  override suspend fun allTags(): List<TagDef> = tagDao.allTags()

  override suspend fun tagsOf(canonicalId: String): List<SongTag> = tagDao.tagsOf(canonicalId)

  override suspend fun tagSong(
    canonicalId: String,
    tagId: String,
    score: Double?,
    source: String
  ) {
    tagDao.upsertSongTag(
      SongTag(
        canonicalId = canonicalId,
        tagId = tagId,
        score = score,
        source = source,
        updatedAt = now(),
        deleted = false
      )
    )
  }

  override suspend fun untagSong(canonicalId: String, tagId: String) {
    tagDao.markDeleted(canonicalId, tagId, now())
  }

  override suspend fun songsOf(tagId: String): List<String> = tagDao.songsOf(tagId)

  override suspend fun usageCounts(): Map<String, Int> =
    tagDao.usageCounts().associate { it.tagId to it.count }

  override suspend fun countActiveOfTag(tagId: String): Int = tagDao.countActiveOfTag(tagId)

  override suspend fun createTag(
    name: String,
    category: String,
    kind: String,
    colorArgb: Int?
  ): String {
    val tagId = USER_TAG_PREFIX + hashId(name)
    if (tagDao.tagById(tagId) == null) {
      val ts = now()
      tagDao.insertTag(
        TagDef(
          tagId = tagId,
          name = name,
          alias = null,
          category = category,
          kind = kind,
          colorArgb = colorArgb,
          sortOrder = USER_SORT_ORDER,
          isPreset = false,
          aliasOf = null,
          createdAt = ts,
          updatedAt = ts
        )
      )
    }
    return tagId
  }

  override suspend fun renameTag(tagId: String, newName: String) {
    tagDao.renameTag(tagId, newName, now())
  }

  override suspend fun updateColor(tagId: String, colorArgb: Int?) {
    tagDao.updateColor(tagId, colorArgb, now())
  }

  override suspend fun updateSortOrder(tagId: String, sortOrder: Int) {
    tagDao.updateSortOrder(tagId, sortOrder, now())
  }

  override suspend fun mergeTag(fromTagId: String, intoTagId: String) {
    if (fromTagId == intoTagId) return
    val ts = now()
    // 冲突行（into 已有同一首歌）直接以 into 为准，先删掉 from 侧避免主键冲突
    tagDao.deleteConflictingRelations(fromTagId, intoTagId)
    tagDao.rewriteTagId(fromTagId, intoTagId, ts)
    tagDao.deleteTag(fromTagId)
  }

  override suspend fun deleteTag(tagId: String) {
    val ts = now()
    tagDao.markAllDeletedOfTag(tagId, ts)
    tagDao.deleteTag(tagId)
  }

  override suspend fun tagMood(canonicalId: String, moodTagId: String) {
    tagSong(canonicalId, moodTagId, null, SongTag.SOURCE_USER)
    val preset = TagSeeds.MOOD_PRESETS[moodTagId] ?: return
    setMoodScore(canonicalId, preset.first, preset.second)
  }

  override suspend fun setMoodScore(canonicalId: String, valence: Int?, arousal: Int?) {
    valence?.let { tagSong(canonicalId, TagSeeds.TAG_VALENCE, it.toDouble()) }
    arousal?.let { tagSong(canonicalId, TagSeeds.TAG_AROUSAL, it.toDouble()) }
  }

  private fun now(): Long = System.currentTimeMillis()

  private fun hashId(name: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(name.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(12)
  }

  private companion object {
    const val USER_TAG_PREFIX = "user."
    const val USER_SORT_ORDER = 900
  }
}
