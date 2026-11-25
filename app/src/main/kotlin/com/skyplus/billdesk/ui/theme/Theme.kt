package com.skyplus.billdesk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** SKYPLUS colours: indigo, teal and a yellow accent. */
private val Indigo = Color(0xFF303F9F)
private val Teal = Color(0xFF00796B)
private val Yellow = Color(0xFFFFC107)

private val Light = lightColorScheme(
    primary = Indigo,
    secondary = Teal,
    tertiary = Yellow
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF4DB6AC),
    tertiary = Yellow
)

@Composable
fun SkyPlusBillDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        content = content
    )
}
