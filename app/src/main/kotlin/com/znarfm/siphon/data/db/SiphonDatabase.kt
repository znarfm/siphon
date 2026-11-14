package com.znarfm.siphon.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class, ChunkEntity::class, DownloadFileEntity::class, BookmarkEntity::class, HistoryEntity::class],
    version = 5,
    exportSchema = false
)
abstract class SiphonDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun browserDao(): BrowserDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_url` ON `bookmarks` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_url` ON `history` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_visitedAt` ON `history` (`visitedAt`)")
            }
        }

        @Volatile
        private var instance: SiphonDatabase? = null

        fun get(context: Context): SiphonDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SiphonDatabase::class.java,
                    "siphon.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
