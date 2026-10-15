package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 歌曲 × 标签关系。
 *
 * 以 [canonicalId]（M1 的跨端稳定 ID）为主键的一部分，因此标签天然可跨端合并。
 * 删除写墓碑（[deleted] = true）而不是物理删除，供跨端按 updatedAt 做 LWW 合并。
 */
@Entity(
  tableName = "song_tags",
  primaryKeys = ["canonicalId", "tagId"],
  indices = [
    Index(value = ["tagId"]),
    Index(value = ["canonicalId"])
  ]
)
data class SongTag(
  val canonicalId: String,
  val tagId: String,
  val score: Double? = null,
  val source: String = "user",
  val updatedAt: Long = 0,
  val deleted: Boolean = false
) {
  companion object {
    const val SOURCE_USER = "user"
    const val SOURCE_DERIVED = "derived"
    const val SOURCE_IMPORT = "import"
    const val SOURCE_PRESET = "preset"
  }
}
