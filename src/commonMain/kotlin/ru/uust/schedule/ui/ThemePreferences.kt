package ru.uust.schedule.ui

import ru.uust.schedule.util.SettingsStorage
import ru.uust.schedule.util.createSettingsStorage

/**
 * Reads and writes the theme selection independently of the schedule cache.
 */
internal class ThemePreferences(
    private val storage: SettingsStorage = createSettingsStorage(),
) {
    fun loadThemeMode(): ThemeMode =
        storage.getString(KEY_THEME_MODE)
            ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
            ?: ThemeMode.System

    fun saveThemeMode(mode: ThemeMode) {
        storage.putString(KEY_THEME_MODE, mode.name)
    }

    fun loadPalette(): ThemePalette =
        storage.getString(KEY_PALETTE)
            ?.let { value -> ThemePalette.entries.firstOrNull { it.name == value } }
            ?: ThemePalette.Current

    fun savePalette(palette: ThemePalette) {
        storage.putString(KEY_PALETTE, palette.name)
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PALETTE = "theme_palette"
    }
}
