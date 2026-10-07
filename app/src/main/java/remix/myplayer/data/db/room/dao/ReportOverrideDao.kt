package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import remix.myplayer.data.db.room.entity.ReportOverride

@Dao
interface ReportOverrideDao {

  @Query("SELECT * FROM report_overrides WHERE year = :year")
  suspend fun byYear(year: Int): List<ReportOverride>

  @Query("SELECT * FROM report_overrides WHERE year = :year AND slot = :slot LIMIT 1")
  suspend fun bySlot(year: Int, slot: String): ReportOverride?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(row: ReportOverride)

  @Query("DELETE FROM report_overrides WHERE year = :year AND slot = :slot")
  suspend fun delete(year: Int, slot: String)

  @Query("DELETE FROM report_overrides")
  suspend fun clear()
}
