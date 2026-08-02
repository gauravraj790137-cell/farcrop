package com.example.farcrop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGreen = Color(0xFF1B5E20)
private val LeafGreen = Color(0xFF4CAF50)
private val CreamBackground = Color(0xFFFDFBF7)
private val EarthBrown = Color(0xFF4E342E)
private val SoftWhite = Color(0xFFFFFFFF)
private val WarmGray = Color(0xFFF2F0EB)

private val LightColors = lightColorScheme(
    primary = DarkGreen,
    onPrimary = SoftWhite,
    secondary = LeafGreen,
    onSecondary = SoftWhite,
    background = CreamBackground,
    onBackground = EarthBrown,
    surface = SoftWhite,
    onSurface = EarthBrown,
    surfaceVariant = WarmGray,
    onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFFD7CCC8)
)

@Composable
fun FarCropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

