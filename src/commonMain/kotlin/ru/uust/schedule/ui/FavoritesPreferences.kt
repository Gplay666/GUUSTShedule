package ru.uust.schedule.ui

import ru.uust.schedule.util.SettingsStorage
import ru.uust.schedule.util.createSettingsStorage

/**
 * Persists the ordered list of favorite group ids independently of the
 * schedule cache, so favorites survive restarts and cache cleanup.
 */
internal class FavoritesPreferences(
    private val storage: SettingsStorage = createSettingsStorage(),
) {
    fun load(): List<Int> =
        storage.getString(KEY_FAVORITE_GROUPS)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.distinct()
            ?: emptyList()

    fun save(ids: List<Int>) {
        storage.putString(KEY_FAVORITE_GROUPS, ids.joinToString(","))
    }

    private companion object {
        const val KEY_FAVORITE_GROUPS = "favorite_groups"
    }
}
