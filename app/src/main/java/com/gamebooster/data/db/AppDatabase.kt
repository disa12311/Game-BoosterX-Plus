package com.gamebooster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gamebooster.data.model.GameProfile

@Database(
    entities = [GameProfile::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
}
