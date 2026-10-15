package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import remix.myplayer.data.db.room.entity.SongTag
import remix.myplayer.data.db.room.entity.TagDef

@Dao
interface TagDao {

  // ---------- tags ----------

  @Query("SELECT * FROM tags WHERE aliasOf IS NULL ORDER BY category, sortOrder, name")
  suspend fun allTags(): List<TagDef>

  @Query("SELECT * FROM tags WHERE tagId = :tagId LIMIT 1")
  suspend fun tagById(tagId: String): TagDef?

  @Query("SELECT COUNT(*) FROM tags WHERE isPreset = 1")
  suspend fun countPreset(): Int

  @Query("SELECT COUNT(*) FROM tags")
  suspend fun countAll(): Int

  @Query("SELECT COUNT(DISTINCT tagId) FROM tags")
  suspend fun countDistinctTagIds(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTag(tag: TagDef): Long

  @Query("UPDATE tags SET name = :name, updatedAt = :updatedAt WHERE tagId = :tagId")
  suspend fun renameTag(tagId: String, name: String, updatedAt: Long)

  @Query("UPDATE tags SET colorArgb = :color, updatedAt = :updatedAt WHERE tagId = :tagId")
  suspend fun updateColor(tagId: String, color: Int?, updatedAt: Long)

  @Query("DELETE FROM tags WHERE tagId = :tagId")
  suspend fun deleteTag(tagId: String): Int

  // ---------- song_tags ----------

  @Query("SELECT * FROM song_tags WHERE canonicalId = :canonicalId AND deleted = 0")
  suspend fun tagsOf(canonicalId: String): List<SongTag>

  @Query("SELECT canonicalId FROM song_tags WHERE tagId = :tagId AND deleted = 0")
  suspend fun songsOf(tagId: String): List<String>

  @Query("SELECT tagId, COUNT(*) AS count FROM song_tags WHERE deleted = 0 GROUP BY tagId")
  suspend fun usageCounts(): List<TagUsage>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSongTag(row: SongTag)

  @Query("UPDATE song_tags SET deleted = 1, updatedAt = :updatedAt WHERE canonicalId = :canonicalId AND tagId = :tagId")
  suspend fun markDeleted(canonicalId: String, tagId: String, updatedAt: Long): Int

  @Query("SELECT COUNT(*) FROM song_tags WHERE deleted = 0")
  suspend fun countActive(): Int

  @Query("UPDATE song_tags SET tagId = :intoTagId, updatedAt = :updatedAt WHERE tagId = :fromTagId")
  suspend fun rewriteTagId(fromTagId: String, intoTagId: String, updatedAt: Long): Int

  @Query("DELETE FROM song_tags WHERE tagId = :tagId AND deleted = 1")
  suspend fun purgeDeletedOfTag(tagId: String): Int

  @Query("SELECT COUNT(*) FROM song_tags WHERE tagId = :tagId AND deleted = 0")
  suspend fun countActiveOfTag(tagId: String): Int

  @Query("UPDATE song_tags SET deleted = 1, updatedAt = :updatedAt WHERE tagId = :tagId")
  suspend fun markAllDeletedOfTag(tagId: String, updatedAt: Long): Int

  /** 合并前清理冲突：删除 from 中那些 into 已有关系的 canonicalId 行，避免主键冲突。 */
  @Query("DELETE FROM song_tags WHERE tagId = :fromTagId AND canonicalId IN (SELECT canonicalId FROM song_tags WHERE tagId = :intoTagId)")
  suspend fun deleteConflictingRelations(fromTagId: String, intoTagId: String): Int
}

data class TagUsage(
  val tagId: String,
  val count: Int
)
