package com.gamebooster.gamemode

import android.app.GameManager
import android.content.Context
import android.util.Log
import com.gamebooster.data.model.GameProfile
import com.gamebooster.shizuku.ShizukuManager
import com.gamebooster.shizuku.ShizukuState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GameModeManager"

@Singleton
class GameModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizuku: ShizukuManager,
) {
    private val gameManager: GameManager? by lazy {
        context.getSystemService(Context.GAME_SERVICE) as? GameManager
    }

    /**
     * Đọc GameMode hiện tại của package (yêu cầu Android 12+).
     * Trả về 0 nếu không hỗ trợ.
     */
    fun getCurrentMode(packageName: String): Int {
        return try {
            // GameManager.getGameMode(packageName) là @hide API trên Android 12
            // Dùng reflection vì không có public overload theo package
            val method = GameManager::class.java.getMethod("getGameMode")
            method.invoke(gameManager) as? Int ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "getGameMode: ${e.message}")
            0
        }
    }

    /**
     * Áp dụng toàn bộ setting của [profile] lên hệ thống.
     *
     * Luồng:
     * 1. Set GameMode qua `cmd game mode set`
     * 2. Set device_config game_overlay cho interventions (downscale, fps)
     */
    suspend fun applyProfile(profile: GameProfile) {
        if (shizuku.getState() != ShizukuState.Ready) {
            Log.w(TAG, "Shizuku not ready – skipping applyProfile for ${profile.packageName}")
            return
        }

        // Bước 1: Set game mode
        val modeResult = shizuku.exec("cmd game mode set ${profile.targetMode} ${profile.packageName}")
        Log.d(TAG, "setMode result: $modeResult")

        // Bước 2: Build intervention string
        // Format: "mode=2,downscaleFactor=0.8:mode=3,downscaleFactor=0.7:fps=30"
        val interventions = buildInterventionConfig(profile)
        if (interventions.isNotEmpty()) {
            val configResult = shizuku.exec(
                "device_config put game_overlay ${profile.packageName} \"$interventions\""
            )
            Log.d(TAG, "setIntervention result: $configResult")
        }
    }

    /**
     * Reset về standard mode và xoá intervention config.
     */
    suspend fun resetProfile(packageName: String) {
        shizuku.exec("cmd game mode set 1 $packageName")
        shizuku.exec("device_config delete game_overlay $packageName")
    }

    /**
     * Đọc intervention config hiện tại từ device_config.
     */
    suspend fun getInterventionConfig(packageName: String): String? {
        return shizuku.exec("device_config get game_overlay $packageName")
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------

    private fun buildInterventionConfig(profile: GameProfile): String {
        val parts = mutableListOf<String>()

        // Performance mode (mode=2)
        if (profile.perfDownscale < 1.0f) {
            val factor = "%.2f".format(profile.perfDownscale)
            parts.add("mode=2,downscaleFactor=$factor")
        }

        // Battery mode (mode=3)
        val batteryParts = mutableListOf<String>()
        if (profile.batteryDownscale < 1.0f) {
            val factor = "%.2f".format(profile.batteryDownscale)
            batteryParts.add("downscaleFactor=$factor")
        }
        if (profile.batteryFpsCap > 0) {
            batteryParts.add("fps=${profile.batteryFpsCap}")
        }
        if (batteryParts.isNotEmpty()) {
            parts.add("mode=3,${batteryParts.joinToString(",")}")
        }

        return parts.joinToString(":")
    }
}
