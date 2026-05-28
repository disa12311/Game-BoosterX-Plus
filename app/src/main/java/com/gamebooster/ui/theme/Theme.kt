package com.gamebooster.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bảng màu gaming – dark theme với accent xanh lá neon
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF00E676),   // neon green
    onPrimary        = Color(0xFF003316),
    primaryContainer = Color(0xFF00522A),
    secondary        = Color(0xFF03DAC6),
    background       = Color(0xFF0A0A0A),
    surface          = Color(0xFF141414),
    surfaceVariant   = Color(0xFF1E1E1E),
    onBackground     = Color(0xFFE0E0E0),
    onSurface        = Color(0xFFE0E0E0),
    error            = Color(0xFFCF6679),
)

@Composable
fun GameBoosterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content,
    )
}
