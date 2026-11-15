package ru.uust.schedule.update

import ru.uust.schedule.util.AndroidAppContext

actual fun createAppUpdater(): AppUpdater? = null

actual fun currentAppVersion(): String = try {
    val context = AndroidAppContext.context
    context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        ?.takeIf { it.isNotBlank() }
        ?: "0.0.0"
} catch (throwable: Throwable) {
    println("UUST: failed to read app version: ${throwable.message}")
    "0.0.0"
}
