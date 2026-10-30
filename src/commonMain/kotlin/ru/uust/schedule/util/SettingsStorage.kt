package ru.uust.schedule.util

/**
 * Small persistent key-value storage for user preferences.
 *
 * Unlike [CacheStorage], this is meant for data that must survive app
 * restarts and cache cleanup (for example, the selected theme).
 */
interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

expect fun createSettingsStorage(): SettingsStorage
