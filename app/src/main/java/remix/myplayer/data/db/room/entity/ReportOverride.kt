package remix.myplayer.data.db.room.entity

import androidx.room.Entity

/**
 * 「年度之最」的用户覆盖选择：某年某个槽位被用户手动换成了哪一项。
 *
 * 槽位 slot ∈ {"artist", "album", "song"}；报告读取时优先用覆盖值，
 * 没有覆盖才回落到聚合出来的 Top。
 */
@Entity(tableName = "report_overrides", primaryKeys = ["year", "slot"])
data class ReportOverride(
  val year: Int,
  val slot: String,
  val canonicalId: String,
  val audioId: Long?,
  val title: String,
  val artist: String,
  val updatedAt: Long
)
