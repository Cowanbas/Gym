package com.cowanbas.gym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppTheme {
    val bg = Color(0xFF161616)
    val card = Color(0xFF161616)
    val border = Color(0xFF2B2B2B)
    val hover = Color(0xFF222222)
    val text = Color(0xFFFFFFFF)
    val muted = Color(0xFF9E9E9E)
    val primary = Color(0xFF757575)
}

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppTheme.bg,
            surface = AppTheme.card,
            primary = AppTheme.primary,
            onBackground = AppTheme.text,
            onSurface = AppTheme.text
        ),
        content = content
    )
}