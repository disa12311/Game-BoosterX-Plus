package com.gamebooster.data.db

import androidx.room.*
import com.gamebooster.data.model.GameProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {

    @Query("SELECT * FROM game_profiles ORDER BY lastPlayed DESC")
    fun observeAll(): Flow<List<GameProfile>>

    @Query("SELECT packageName FROM game_profiles")
    suspend fun getAllPackageNames(): List<String>

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: GameProfile)

    @Delete
    suspend fun delete(profile: GameProfile)
}