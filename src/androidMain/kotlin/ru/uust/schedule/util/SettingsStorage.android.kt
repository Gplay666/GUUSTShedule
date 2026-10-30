package ru.uust.schedule.util

import android.content.Context

actual fun createSettingsStorage(): SettingsStorage = AndroidSettingsStorage()

private class AndroidSettingsStorage : SettingsStorage {
    private val preferences by lazy {
        AndroidAppContext.context.getSharedPreferences(
            "uust-schedule-settings",
            Context.MODE_PRIVATE,
        )
    }

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
