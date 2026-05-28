package com.gamebooster.ui

import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamebooster.data.GameRepository
import com.gamebooster.data.model.AppInfo
import com.gamebooster.data.model.GameProfile
import com.gamebooster.shizuku.ShizukuManager
import com.gamebooster.shizuku.ShizukuState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

data class MainUiState(
    val shizukuState: ShizukuState       = ShizukuState.NotRunning,
    val profiles: List<GameProfile>      = emptyList(),
    val searchQuery: String              = "",
    val searchResults: List<AppInfo>     = emptyList(),
    val isSearching: Boolean             = false,
    val showAddDialog: Boolean           = false,
    val snackbar: String?                = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: GameRepository,
    private val shizuku: ShizukuManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        val s = if (result == PackageManager.PERMISSION_GRANTED)
            ShizukuState.Ready else ShizukuState.NoPermission
        _state.update { it.copy(shizukuState = s) }
    }

    init {
        Shizuku.addRequestPermissionResultListener(permListener)
        viewModelScope.launch {
            repo.allProfiles.collect { list ->
                _state.update { it.copy(profiles = list) }
            }
        }
        refreshShizukuState()
    }

    fun refreshShizukuState() {
        _state.update { it.copy(shizukuState = shizuku.getState()) }
    }

    fun requestShizukuPermission() = shizuku.requestPermission(1001)

    // --- Add app dialog ---

    fun openAddDialog() {
        _state.update { it.copy(showAddDialog = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeAddDialog() {
        _state.update { it.copy(showAddDialog = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.length >= 2) searchApps(query) else
            _state.update { it.copy(searchResults = emptyList()) }
    }

    private fun searchApps(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = repo.searchInstalledApps(query)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun addApp(appInfo: AppInfo) {
        viewModelScope.launch {
            repo.addCustomApp(appInfo.packageName)
            _state.update { it.copy(
                snackbar      = "✓ Đã thêm: ${appInfo.label}",
                showAddDialog = false,
                searchQuery   = "",
                searchResults = emptyList(),
            )}
        }
    }

    // --- Profile actions ---

    fun saveProfile(profile: GameProfile) {
        viewModelScope.launch {
            repo.saveAndApply(profile)
            _state.update { it.copy(snackbar = "✓ Đã áp dụng: ${profile.appLabel}") }
        }
    }

    fun removeProfile(profile: GameProfile) {
        viewModelScope.launch {
            repo.removeProfile(profile)
            _state.update { it.copy(snackbar = "Đã xoá: ${profile.appLabel}") }
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeRequestPermissionResultListener(permListener)
    }
}