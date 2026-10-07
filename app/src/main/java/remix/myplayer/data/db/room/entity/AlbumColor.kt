package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专辑封面主色缓存：MediaStore 的 albumId → ARGB。
 *
 * 用途：S2「音乐颜色」页面，以及由用户主色派生整套报告配色。
 * 取色失败时写入 fallback 色，避免每次都重试解码封面。
 */
@Entity(tableName = "album_colors")
data class AlbumColor(
  @PrimaryKey val albumId: Long,
  val color: Int,
  val updatedAt: Long
)
