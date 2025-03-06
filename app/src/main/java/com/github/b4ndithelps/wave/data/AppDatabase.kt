package com.github.b4ndithelps.wave.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BookData::class, PinnedPlaylist::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun pinnedPlaylistDao(): PinnedPlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the two new columns
                database.execSQL("ALTER TABLE books ADD COLUMN current_spine_index INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN current_page_index INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the pinned_playlists table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pinned_playlists` (" +
                    "`playlistId` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`imageUrl` TEXT NOT NULL, " +
                    "`trackCount` INTEGER NOT NULL, " +
                    "`creator` TEXT NOT NULL, " +
                    "`pinnedAt` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        // Singleton pattern ensures only one instance of the database exists
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}