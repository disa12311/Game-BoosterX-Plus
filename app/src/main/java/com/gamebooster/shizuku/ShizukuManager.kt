package com.gamebooster.shizuku

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShizukuManager"

sealed class ShizukuState {
    object NotInstalled : ShizukuState()
    object NotRunning   : ShizukuState()
    object NoPermission : ShizukuState()
    object Ready        : ShizukuState()
}

@Singleton
class ShizukuManager @Inject constructor() {

    fun getState(): ShizukuState = try {
        when {
            !Shizuku.pingBinder() -> ShizukuState.NotRunning
            !hasPermission()      -> ShizukuState.NoPermission
            else                  -> ShizukuState.Ready
        }
    } catch (e: IllegalStateException) {
        ShizukuState.NotInstalled
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(requestCode: Int) = Shizuku.requestPermission(requestCode)

    suspend fun exec(command: String): String? = withContext(Dispatchers.IO) {
        if (getState() != ShizukuState.Ready) {
            Log.w(TAG, "Shizuku not ready, skip: $command")
            return@withContext null
        }
        try {
            val process: ShizukuRemoteProcess = Shizuku.newProcess(
                arrayOf("sh", "-c", command), null, null
            )
            val output = process.inputStream.bufferedReader().readText().trim()
            val error  = process.errorStream.bufferedReader().readText().trim()
            val code   = process.waitFor()
            if (error.isNotEmpty()) Log.w(TAG, "stderr[$command]: $error")
            Log.d(TAG, "exec [$command] exit=$code")
            if (code == 0) output else null
        } catch (e: Exception) {
            Log.e(TAG, "exec failed [$command]: ${e.message}")
            null
        }
    }
}
