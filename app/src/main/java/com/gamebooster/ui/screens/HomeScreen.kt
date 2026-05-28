package com.gamebooster.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gamebooster.data.model.AppInfo
import com.gamebooster.data.model.GameProfile
import com.gamebooster.shizuku.ShizukuState
import com.gamebooster.ui.MainUiState
import com.gamebooster.ui.components.GameModeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onAddApp: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    onCloseDialog: () -> Unit,
    onSaveProfile: (GameProfile) -> Unit,
    onRemoveProfile: (GameProfile) -> Unit,
    onRequestShizuku: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game BoosterX Plus", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddApp) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm app")
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start  = 16.dp, end = 16.dp,
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ShizukuStatusBanner(
                    state               = uiState.shizukuState,
                    onRequestPermission = onRequestShizuku,
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text(
                    "App đã thêm (${uiState.profiles.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            if (uiState.profiles.isEmpty()) {
                item { EmptyState(onAddApp) }
            } else {
                items(uiState.profiles, key = { it.packageName }) { profile ->
                    GameProfileCard(
                        profile      = profile,
                        onSave       = onSaveProfile,
                        onRemove     = onRemoveProfile,
                        shizukuReady = uiState.shizukuState == ShizukuState.Ready,
                    )
                }
            }
        }
    }

    // Add App Dialog
    if (uiState.showAddDialog) {
        AddAppDialog(
            query         = uiState.searchQuery,
            results       = uiState.searchResults,
            isSearching   = uiState.isSearching,
            onQueryChange = onSearchQueryChange,
            onAppSelected = onAppSelected,
            onDismiss     = onCloseDialog,
        )
    }
}

// ---------------------------------------------------------------------------
// Add App Dialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppDialog(
    query: String,
    results: List<AppInfo>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    placeholder   = { Text("Tên app...") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )

                if (results.isEmpty() && query.length >= 2 && !isSearching) {
                    Text(
                        "Không tìm thấy app nào",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp),
                    )
                }

                LazyColumn(
                    modifier            = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(results, key = { it.packageName }) { app ->
                        AppSearchItem(app = app, onClick = { onAppSelected(app) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
    )
}

@Composable
private fun AppSearchItem(app: AppInfo, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Surface(
        onClick = onClick,
        shape   = MaterialTheme.shapes.small,
        color   = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = remember(app.packageName) {
                runCatching { ctx.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
            }
            AsyncImage(
                model              = ImageRequest.Builder(ctx).data(icon).crossfade(true).build(),
                contentDescription = null,
                modifier           = Modifier.size(36.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(app.label,       style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(app.packageName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Icon(imageVector = Icons.Default.Add, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------------------
// Shizuku banner
// ---------------------------------------------------------------------------

@Composable
private fun ShizukuStatusBanner(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
) {
    val containerColor = when (state) {
        ShizukuState.Ready        -> MaterialTheme.colorScheme.primaryContainer
        ShizukuState.NoPermission -> MaterialTheme.colorScheme.errorContainer
        else                      -> MaterialTheme.colorScheme.surfaceVariant
    }
    val icon: ImageVector = when (state) {
        ShizukuState.Ready        -> Icons.Default.CheckCircle
        ShizukuState.NoPermission -> Icons.Default.Lock
        else                      -> Icons.Default.Warning
    }
    val title = when (state) {
        ShizukuState.Ready        -> "Shizuku đã kết nối"
        ShizukuState.NoPermission -> "Chưa cấp quyền Shizuku"
        ShizukuState.NotRunning   -> "Shizuku chưa chạy"
        ShizukuState.NotInstalled -> "Shizuku chưa cài"
    }
    val subtitle = when (state) {
        ShizukuState.Ready        -> "GameMode API sẵn sàng áp dụng"
        ShizukuState.NoPermission -> "Nhấn để cấp quyền"
        ShizukuState.NotRunning   -> "Mở app Shizuku và bật service"
        ShizukuState.NotInstalled -> "Tải Shizuku từ Play Store / GitHub"
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (state == ShizukuState.NoPermission) {
                TextButton(onClick = onRequestPermission) { Text("Cấp quyền") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Game profile card
// ---------------------------------------------------------------------------

@Composable
private fun GameProfileCard(
    profile: GameProfile,
    onSave: (GameProfile) -> Unit,
    onRemove: (GameProfile) -> Unit,
    shizukuReady: Boolean,
) {
    var expanded  by remember { mutableStateOf(false) }
    var mode      by remember(profile) { mutableIntStateOf(profile.targetMode) }
    var perfScale by remember(profile) { mutableFloatStateOf(profile.perfDownscale) }
    var battScale by remember(profile) { mutableFloatStateOf(profile.batteryDownscale) }
    var battFps   by remember(profile) { mutableIntStateOf(profile.batteryFpsCap) }

    val isDirty = mode      != profile.targetMode
        || perfScale != profile.perfDownscale
        || battScale != profile.batteryDownscale
        || battFps   != profile.batteryFpsCap

    Card(
        onClick  = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(profile.packageName)
                Column(Modifier.weight(1f)) {
                    Text(profile.appLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(modeLabel(mode),  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (isDirty) Badge { Text("*") }
                Icon(
                    imageVector        = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Chế độ tối ưu", style = MaterialTheme.typography.labelMedium)
                    GameModeSelector(selectedMode = mode, onModeSelected = { mode = it })

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    AnimatedVisibility(visible = mode == 2) {
                        SliderRow(
                            label = "Render scale (Performance)", hint = "Giảm để nhẹ GPU",
                            value = perfScale, range = 0.5f..1.0f,
                            display = { "${(it * 100).toInt()}%" }, onChange = { perfScale = it },
                        )
                    }
                    AnimatedVisibility(visible = mode == 3) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SliderRow(
                                label = "Render scale (Battery)", hint = "Giảm để tiết kiệm GPU",
                                value = battScale, range = 0.5f..1.0f,
                                display = { "${(it * 100).toInt()}%" }, onChange = { battScale = it },
                            )
                            SliderRow(
                                label = "FPS cap (Battery)", hint = "0 = không giới hạn",
                                value = battFps.toFloat(), range = 0f..120f, steps = 11,
                                display = { if (it == 0f) "∞" else "${it.toInt()}" }, onChange = { battFps = it.toInt() },
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onRemove(profile) }, modifier = Modifier.weight(1f),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Xoá")
                        }
                        Button(
                            onClick = { onSave(profile.copy(targetMode = mode, perfDownscale = perfScale, batteryDownscale = battScale, batteryFpsCap = battFps)) },
                            modifier = Modifier.weight(1f), enabled = shizukuReady,
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text(if (isDirty) "Áp dụng *" else "Áp dụng")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun SliderRow(
    label: String, hint: String, value: Float,
    range: ClosedFloatingPointRange<Float>, display: (Float) -> String,
    onChange: (Float) -> Unit, steps: Int = 0,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(hint,  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Text(display(value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val ctx  = LocalContext.current
    val icon = remember(packageName) { runCatching { ctx.packageManager.getApplicationIcon(packageName) }.getOrNull() }
    AsyncImage(
        model = ImageRequest.Builder(ctx).data(icon).crossfade(true).build(),
        contentDescription = null, modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        Text("Chưa có app nào", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Button(onClick = onAdd) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp)); Text("Thêm app")
        }
    }
}

private fun modeLabel(mode: Int) = when (mode) {
    2    -> "⚡ Performance"
    3    -> "🔋 Battery"
    else -> "⚖ Standard"
}