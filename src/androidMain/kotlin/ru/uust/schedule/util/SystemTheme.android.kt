package ru.uust.schedule.util

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.graphics.Color

actual fun readSystemTheme(): SystemThemeInfo {
    val context = AndroidAppContext.context
    val nightMode = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK
    val isDark = when (nightMode) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> null
    }

    val accentColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            Color(context.getColor(android.R.color.system_accent1_500))
        }.getOrNull()
    } else {
        null
    }

    return SystemThemeInfo(
        isDark = isDark,
        accentColor = accentColor,
    )
}
