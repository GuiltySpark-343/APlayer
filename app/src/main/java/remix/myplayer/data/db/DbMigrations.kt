package remix.myplayer.data.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.tag.TagSeeds
import java.util.Calendar

internal object DbMigrations {

  val migration3to4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `data` TEXT NOT NULL DEFAULT ''")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `account` TEXT")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `pwd` TEXT")

      db.execSQL("CREATE TABLE IF NOT EXISTS `WebDav` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alias` TEXT NOT NULL, `account` TEXT, `pwd` TEXT, `server` TEXT NOT NULL, `lastPath` TEXT, `createAt` INTEGER NOT NULL)")
    }
  }

  val migration4to5 = object : Migration(4, 5) {
    @SuppressLint("Range")
    override fun migrate(db: SupportSQLiteDatabase) {
      val temp = ArrayList<WebDav>()
      val cursor = db.query("select * from `Webdav`")
      while (cursor.moveToNext()) {
        temp.add(
          WebDav(
            cursor.getString(cursor.getColumnIndex("alias")),
            cursor.getString(cursor.getColumnIndex("account")),
            cursor.getString(cursor.getColumnIndex("pwd")),
            cursor.getString(cursor.getColumnIndex("server")),
            cursor.getString(cursor.getColumnIndex("server")),
            cursor.getLong(cursor.getColumnIndex("createAt"))
          ).apply {
            id = cursor.getInt(cursor.getColumnIndex("id"))
          })
      }
      print(temp)
      db.execSQL("DROP TABLE `WebDav`")
      db.execSQL("CREATE TABLE IF NOT EXISTS `WebDav` (`alias` TEXT NOT NULL, `account` TEXT NOT NULL, `pwd` TEXT NOT NULL, `server` TEXT NOT NULL, `lastUrl` TEXT NOT NULL, `createAt` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
      temp.forEach { webDav ->
        db.insert("WebDav", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
          put("alias", webDav.alias)
          put("account", webDav.account)
          put("pwd", webDav.pwd)
          put("server", webDav.server)
          put("lastUrl", webDav.lastUrl)
          put("createAt", webDav.createAt)
          put("id", webDav.id)
        })
      }
    }
  }

  val migration5to6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `MetaDataCache` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `fileSize` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `year` TEXT NOT NULL, `genre` TEXT NOT NULL, `track` TEXT NOT NULL, `updateTime` INTEGER NOT NULL, PRIMARY KEY (`url`))")
    }
  }

  val migration6to7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `Smb` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alias` TEXT NOT NULL, `domain` TEXT, `account` TEXT NOT NULL, `pwd` TEXT NOT NULL, `server` TEXT NOT NULL, `share` TEXT NOT NULL, `lastUrl` TEXT NOT NULL, `createAt` INTEGER NOT NULL)")
    }
  }

  val migration7to8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `play_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `schemaVersion` INTEGER NOT NULL, `eventId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `eventType` TEXT NOT NULL, `canonicalId` TEXT NOT NULL, `audioId` INTEGER, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `listenedMs` INTEGER NOT NULL, `listenRatio` REAL NOT NULL, `playScore` REAL NOT NULL, `completed` INTEGER NOT NULL, `source` TEXT NOT NULL, `endReason` TEXT NOT NULL, `titleSnapshot` TEXT NOT NULL, `artistSnapshot` TEXT NOT NULL, `albumSnapshot` TEXT NOT NULL, `genreSnapshot` TEXT, `sourceUri` TEXT, `contentHash` TEXT, `pathHint` TEXT, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `hour` INTEGER NOT NULL, `weekday` INTEGER NOT NULL)"
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_year` ON `play_events` (`year`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_canonicalId` ON `play_events` (`canonicalId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_audioId` ON `play_events` (`audioId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_source` ON `play_events` (`source`)")

      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_startedAt` ON `play_events` (`startedAt`)")
    }
  }

  val migration8to9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `songId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `artistId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `albumId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `genreId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `playlistId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `mediaType` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `sessionId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `gapBeforeMs` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `gapAfterMs` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `loopCount` INTEGER NOT NULL DEFAULT 0")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `outputDevice` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `isForeground` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `decoder` TEXT")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_eventType` ON `play_events` (`eventType`)")
    }
  }

  val migration9to10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
      // 先去掉可能存在的重复 eventId，再建立唯一索引（用于导入幂等）
      db.execSQL("DELETE FROM `play_events` WHERE `id` NOT IN (SELECT MIN(`id`) FROM `play_events` GROUP BY `eventId`)")
      db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_play_events_eventId` ON `play_events` (`eventId`)")
    }
  }

  val migration10to11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `album_colors` (`albumId` INTEGER NOT NULL, `color` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
      )
    }
  }

  val migration11to12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `report_overrides` (`year` INTEGER NOT NULL, `slot` TEXT NOT NULL, `canonicalId` TEXT NOT NULL, `audioId` INTEGER, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`year`, `slot`))"
      )
    }
  }

  /**
   * 歌曲标签系统（T0）：纯新增两张表，不触碰既有表。
   *
   * DDL 必须与 Room 由实体生成的 schema 完全一致（见 app/schemas/.../13.json），
   * 否则 Room 的 schema 校验会在运行时抛异常。
   */
  val migration12to13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tagId` TEXT NOT NULL, `name` TEXT NOT NULL, `alias` TEXT, `category` TEXT NOT NULL, `kind` TEXT NOT NULL, `colorArgb` INTEGER, `sortOrder` INTEGER NOT NULL, `isPreset` INTEGER NOT NULL, `aliasOf` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
      )
      db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_tagId` ON `tags` (`tagId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_category_sortOrder` ON `tags` (`category`, `sortOrder`)")

      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `song_tags` (`canonicalId` TEXT NOT NULL, `tagId` TEXT NOT NULL, `score` REAL, `source` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, PRIMARY KEY(`canonicalId`, `tagId`))"
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_tags_tagId` ON `song_tags` (`tagId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_tags_canonicalId` ON `song_tags` (`canonicalId`)")

      // 预设词表（幂等：INSERT OR IGNORE，重复执行不产生重复行）
      TagSeeds.PRESET_TAGS.forEach { seed ->
        db.execSQL(
          "INSERT OR IGNORE INTO `tags` (`tagId`, `name`, `alias`, `category`, `kind`, `colorArgb`, `sortOrder`, `isPreset`, `aliasOf`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?, ?, 1, NULL, ?, ?)",
          arrayOf(
            seed.tagId,
            seed.name,
            seed.alias,
            seed.category,
            seed.kind,
            seed.colorArgb,
            seed.sortOrder,
            TagSeeds.CREATED_AT,
            TagSeeds.CREATED_AT
          )
        )
      }
    }
  }

  /**
   * 把历史数据的派生列从 UTC 改成设备本地时区。
   *
   * 用 JVM 的 `Calendar` 逐行重算，而不用 SQL 的 `strftime(..., 'localtime')`：实测在真机上
   * 迁移里的那段 SQL 一行都没改（派生列仍是 UTC 值），而同样的 SQL 在设备的 sqlite3 CLI 上却是对的，
   * 说明依赖平台 SQLite 的时区处理不可靠。逐行重算与 PlayEventRecorder 用同一套 API，
   * 结果确定、与平台无关，并且可以重复执行（幂等，始终从 `startedAt` 重算）。
   */
  val migration13to14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
      val rows = ArrayList<Pair<Long, Long>>()
      db.query("SELECT `id`, `startedAt` FROM `play_events`").use { cursor ->
        while (cursor.moveToNext()) {
          rows.add(cursor.getLong(0) to cursor.getLong(1))
        }
      }
      if (rows.isEmpty()) return

      val calendar = Calendar.getInstance()
      val statement = db.compileStatement(
        "UPDATE `play_events` SET `year` = ?, `month` = ?, `day` = ?, `hour` = ?, `weekday` = ? WHERE `id` = ?"
      )
      rows.forEach { (id, startedAt) ->
        calendar.timeInMillis = startedAt
        statement.clearBindings()
        statement.bindLong(1, calendar.get(Calendar.YEAR).toLong())
        statement.bindLong(2, (calendar.get(Calendar.MONTH) + 1).toLong())
        statement.bindLong(3, calendar.get(Calendar.DAY_OF_MONTH).toLong())
        statement.bindLong(4, calendar.get(Calendar.HOUR_OF_DAY).toLong())
        statement.bindLong(5, calendar.get(Calendar.DAY_OF_WEEK).toLong())
        statement.bindLong(6, id)
        statement.executeUpdateDelete()
      }
    }
  }
}
