package remix.myplayer.repo

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.AlbumColorDao
import remix.myplayer.data.db.room.entity.AlbumColor
import remix.myplayer.util.ColorUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专辑封面主色：audioId → albumId → 封面 → Palette 主色。
 *
 * 结果缓存进 album_colors，取色失败也写 fallback，避免每次都重新解码封面。
 */
@Singleton
class AlbumColorRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val dao: AlbumColorDao
) {

  /** 返回 albumId → ARGB。命中缓存直接返回，未命中才读封面取色。 */
  suspend fun colorsForAudioIds(
    audioIds: List<Long>,
    fallbackArgb: Int,
    limit: Int = 100
  ): Map<Long, Int> = withContext(Dispatchers.IO) {
    val pairs = audioIds.mapNotNull { audioId -> resolveAlbumId(audioId)?.let { audioId to it } }
      .take(limit)
    if (pairs.isEmpty()) return@withContext emptyMap()

    val albumIds = pairs.map { it.second }.distinct()
    val colors = ensureColors(albumIds, fallbackArgb)

    val result = HashMap<Long, Int>(colors)
    pairs.forEach { (_, albumId) -> colors[albumId]?.let { result[albumId] = it } }
    result
  }

  /**
   * 与入参**一一对应**的颜色（解析不出封面的项被跳过），用于 S2 色板按听歌顺序展示。
   */
  suspend fun colorsInOrder(
    audioIds: List<Long>,
    fallbackArgb: Int,
    limit: Int = 100
  ): List<Int> = withContext(Dispatchers.IO) {
    val albumIds = audioIds.mapNotNull { resolveAlbumId(it) }.take(limit)
    if (albumIds.isEmpty()) return@withContext emptyList()

    val colors = ensureColors(albumIds.distinct(), fallbackArgb)
    albumIds.mapNotNull { colors[it] }
  }

  /** 取色（含缓存与回填），返回 albumId → ARGB 的可变表。 */
  private suspend fun ensureColors(albumIds: List<Long>, fallbackArgb: Int): MutableMap<Long, Int> {
    val colors = dao.byIds(albumIds).associate { it.albumId to it.color }.toMutableMap()
    val missing = albumIds.filterNot { colors.containsKey(it) }
    val fresh = ArrayList<AlbumColor>(missing.size)
    missing.forEach { albumId ->
      val color = extractColor(albumId) ?: fallbackArgb
      fresh.add(AlbumColor(albumId, color, System.currentTimeMillis()))
      colors[albumId] = color
    }
    if (fresh.isNotEmpty()) dao.upsert(fresh)
    return colors
  }

  private fun resolveAlbumId(audioId: Long): Long? = runCatching {
    context.contentResolver.query(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      arrayOf(MediaStore.Audio.Media.ALBUM_ID),
      "_id = ?",
      arrayOf(audioId.toString()),
      null
    )?.use { cursor ->
      if (cursor.moveToFirst()) cursor.getLong(0) else null
    }
  }.getOrNull()

  private fun extractColor(albumId: Long): Int? = runCatching {
    val uri: Uri = ContentUris.withAppendedId(
      Uri.parse("content://media/external/audio/albumart/"),
      albumId
    )
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
      ?: return@runCatching null

    // 先读边界再按目标 160px 降采样，避免整张大图进内存
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 160) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(
      bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return@runCatching null

    val palette = Palette.from(bitmap).generate()
    bitmap.recycle()
    // 复用项目已有的选色策略（ColorUtil.getColor(Palette, int)）
    ColorUtil.getColor(palette, 0)
  }.getOrNull()?.takeIf { it != 0 }
}
