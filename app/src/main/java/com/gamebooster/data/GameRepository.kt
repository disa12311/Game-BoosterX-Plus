package com.gamebooster.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.gamebooster.data.db.GameProfileDao
import com.gamebooster.data.model.AppInfo
import com.gamebooster.data.model.GameProfile
import com.gamebooster.gamemode.GameModeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GameRepository"

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: GameProfileDao,
    private val gameModeManager: GameModeManager,
) {
    val allProfiles: Flow<List<GameProfile>> = dao.observeAll()

    /** Tìm kiếm app đã cài theo tên, trả về list chưa có trong DB */
    suspend fun searchInstalledApps(query: String): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm         = context.packageManager
        val savedPkgs  = dao.getAllPackageNames().toSet()

        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { info ->
                val label = pm.getApplicationLabel(info).toString()
                label.contains(query, ignoreCase = true) && info.packageName !in savedPkgs
            }
            .map { info ->
                AppInfo(
                    packageName = info.packageName,
                    label       = pm.getApplicationLabel(info).toString(),
                )
            }
            .sortedBy { it.label }
            .take(30)
    }

    suspend fun getProfile(packageName: String): GameProfile? = dao.getByPackage(packageName)

    suspend fun saveAndApply(profile: GameProfile) {
        dao.upsert(profile)
        gameModeManager.applyProfile(profile)
    }

    suspend fun removeProfile(profile: GameProfile) {
        dao.delete(profile)
        gameModeManager.resetProfile(profile.packageName)
    }

    suspend fun addCustomApp(packageName: String): GameProfile? {
        return try {
            val pm    = context.packageManager
            val info  = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(info).toString()
            val profile = GameProfile(packageName = packageName, appLabel = label)
            dao.upsert(profile)
            profile
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName")
            null
        }
    }
}