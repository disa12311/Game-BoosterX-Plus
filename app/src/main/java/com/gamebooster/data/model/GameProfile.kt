package com.gamebooster.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Setting tối ưu per-game.
 *
 * targetMode:
 *   1 = STANDARD    – mặc định, cân bằng
 *   2 = PERFORMANCE – ưu tiên FPS, render scale giảm GPU load
 *   3 = BATTERY     – tiết kiệm pin, giới hạn FPS + downscale
 */
@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey
    val packageName: String,

    val appLabel: String,

    /** GameMode áp dụng khi mở game (1/2/3) */
    val targetMode: Int = 1,

    /** Render downscale khi Performance mode (0.5–1.0, 1.0 = native) */
    val perfDownscale: Float = 1.0f,

    /** Render downscale khi Battery mode */
    val batteryDownscale: Float = 0.75f,

    /** FPS cap khi Battery mode (0 = không giới hạn) */
    val batteryFpsCap: Int = 0,

    val lastPlayed: Long = 0L,
    val totalPlayTime: Long = 0L,
)
