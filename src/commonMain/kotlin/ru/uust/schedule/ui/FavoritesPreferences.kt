package ru.uust.schedule.ui

import ru.uust.schedule.data.SelectedTarget
import ru.uust.schedule.data.TargetKind
import ru.uust.schedule.util.SettingsStorage
import ru.uust.schedule.util.createSettingsStorage

/**
 * Persists the ordered list of favorite groups and teachers independently of
 * the schedule cache, so favorites survive restarts and cache cleanup.
 */
internal class FavoritesPreferences(
    private val storage: SettingsStorage = createSettingsStorage(),
) {
    fun load(): List<SelectedTarget> {
        val stored = storage.getString(KEY_FAVORITE_TARGETS)
        if (stored != null) return parse(stored)

        // Older builds only stored group ids. Migrate them into the shared list
        // so existing favorites are not lost.
        val legacy = storage.getString(KEY_LEGACY_FAVORITE_GROUPS)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.distinct()
            ?.map { SelectedTarget(TargetKind.Group, it) }
            ?: emptyList()
        if (legacy.isNotEmpty()) save(legacy)
        return legacy
    }

    fun save(targets: List<SelectedTarget>) {
        storage.putString(
            KEY_FAVORITE_TARGETS,
            targets.joinToString(",") { "${it.kind.storageKey}:${it.id}" },
        )
    }

    private fun parse(raw: String): List<SelectedTarget> =
        raw.split(',')
            .mapNotNull { entry ->
                val separator = entry.indexOf(':')
                if (separator <= 0) return@mapNotNull null
                val kind = TargetKind.entries.firstOrNull {
                    it.storageKey == entry.substring(0, separator)
                } ?: return@mapNotNull null
                val id = entry.substring(separator + 1).trim().toIntOrNull()
                    ?: return@mapNotNull null
                SelectedTarget(kind, id)
            }
            .distinct()

    private companion object {
        const val KEY_FAVORITE_TARGETS = "favorite_targets"
        const val KEY_LEGACY_FAVORITE_GROUPS = "favorite_groups"
    }
}
