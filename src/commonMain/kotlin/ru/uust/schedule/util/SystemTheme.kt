package ru.uust.schedule.util

import androidx.compose.ui.graphics.Color

/**
 * Best-effort information about the desktop theme.
 *
 * Values are nullable because not every Linux desktop exposes its colour
 * preferences through the same mechanism. The UI falls back to the standard
 * UUST theme when a value is unavailable.
 */
data class SystemThemeInfo(
    val isDark: Boolean?,
    val accentColor: Color?,
)

expect fun readSystemTheme(): SystemThemeInfo
