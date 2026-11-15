package ru.uust.schedule.update

/**
 * A release published on GitHub that can update the desktop application.
 */
data class UpdateInfo(
    val version: String,
    val releaseName: String,
    val changelog: String,
    val releaseUrl: String,
    val assetName: String,
    val downloadUrl: String,
    val assetSize: Long,
    val checksumUrl: String?,
    val installKind: UpdateInstallKind,
    val supportsOneClick: Boolean,
)

enum class UpdateInstallKind {
    AppImage,
    WindowsInstaller,
    WindowsPortable,
    Manual,
}

class UpdateException(message: String) : Exception(message)

/**
 * Platform updater. Android has no implementation because updates are only
 * distributed for desktop platforms.
 */
interface AppUpdater {
    suspend fun check(currentVersion: String): UpdateInfo?
    suspend fun downloadAndInstall(update: UpdateInfo, onProgress: (Float?) -> Unit)
    fun openReleasePage(update: UpdateInfo)
}

expect fun createAppUpdater(): AppUpdater?

expect fun currentAppVersion(): String

/** Drops a leading `v` and pre-release/build suffixes before comparing. */
fun normalizeVersion(version: String): String = version
    .trim()
    .removePrefix("v")
    .removePrefix("V")
    .substringBefore('-')
    .substringBefore('+')

/** Returns a positive number when [left] is newer than [right]. */
fun compareVersions(left: String, right: String): Int {
    val leftParts = normalizeVersion(left).split('.')
    val rightParts = normalizeVersion(right).split('.')
    val size = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until size) {
        val leftPart = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
        val rightPart = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }
    return 0
}
