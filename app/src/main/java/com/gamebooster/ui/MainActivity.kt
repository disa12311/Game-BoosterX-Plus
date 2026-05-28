package com.gamebooster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.gamebooster.ui.screens.HomeScreen
import com.gamebooster.ui.theme.GameBoosterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GameBoosterTheme {
                val uiState           = viewModel.state.collectAsState().value
                val snackbarHostState = remember { SnackbarHostState() }
                val scope             = rememberCoroutineScope()
                val lifecycle         = LocalLifecycleOwner.current.lifecycle

                LaunchedEffect(lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        viewModel.refreshShizukuState()
                    }
                }

                LaunchedEffect(uiState.snackbar) {
                    uiState.snackbar?.let { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                        viewModel.clearSnackbar()
                    }
                }

                HomeScreen(
                    uiState             = uiState,
                    onAddApp            = viewModel::openAddDialog,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onAppSelected       = viewModel::addApp,
                    onCloseDialog       = viewModel::closeAddDialog,
                    onSaveProfile       = viewModel::saveProfile,
                    onRemoveProfile     = viewModel::removeProfile,
                    onRequestShizuku    = viewModel::requestShizukuPermission,
                )
            }
        }
    }
}