package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import remix.myplayer.data.db.room.entity.AlbumColor

@Dao
interface AlbumColorDao {

  @Query("SELECT * FROM album_colors WHERE albumId IN (:ids)")
  suspend fun byIds(ids: List<Long>): List<AlbumColor>

  @Query("SELECT * FROM album_colors ORDER BY updatedAt DESC LIMIT :limit")
  suspend fun recent(limit: Int): List<AlbumColor>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(rows: List<AlbumColor>)

  @Query("DELETE FROM album_colors")
  suspend fun clear()
}
