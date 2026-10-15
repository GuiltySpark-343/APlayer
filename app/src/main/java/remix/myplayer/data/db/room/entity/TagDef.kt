package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签定义（歌曲语义标签的「词表」）。
 *
 * 注意：与音频文件的 ID3 标签无关 —— 这是 App 自己的语义标签体系（客观/情绪/情境三层）。
 * [tagId] 是稳定字符串 ID（如 ctx.home / mood.valence），跨端导出与合并只认它，不认自增 id。
 */
@Entity(
  tableName = "tags",
  indices = [
    Index(value = ["tagId"], unique = true),
    Index(value = ["category", "sortOrder"])
  ]
)
data class TagDef(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val tagId: String,
  val name: String,
  val alias: String? = null,
  val category: String,
  val kind: String,
  val colorArgb: Int? = null,
  val sortOrder: Int = 0,
  val isPreset: Boolean = false,
  val aliasOf: String? = null,
  val createdAt: Long = 0,
  val updatedAt: Long = 0
) {
  companion object {
    const val CATEGORY_OBJECTIVE = "OBJECTIVE"
    const val CATEGORY_MOOD = "MOOD"
    const val CATEGORY_CONTEXT = "CONTEXT"
    const val KIND_BOOLEAN = "BOOLEAN"
    const val KIND_SCALE = "SCALE"
  }
}
