package com.siphon.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class, ChunkEntity::class, BookmarkEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SiphonDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var instance: SiphonDatabase? = null

        fun get(context: Context): SiphonDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SiphonDatabase::class.java,
                    "siphon.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
    }
}
