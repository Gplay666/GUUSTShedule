package ru.uust.schedule.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import ru.uust.schedule.data.createHttpClient
import ru.uust.schedule.util.AndroidAppContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

actual fun createAppUpdater(): AppUpdater? = AndroidAppUpdater()

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

private class AndroidAppUpdater : AppUpdater {
    private val context: Context = AndroidAppContext.context
    private val ktor = createHttpClient()

    override suspend fun check(currentVersion: String): UpdateInfo? {
        val release = withContext(Dispatchers.IO) {
            ktor.get(LATEST_RELEASE_URL) {
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }.body<GitHubRelease>()
        }

        if (release.draft || release.prerelease) return null
        val version = normalizeVersion(release.tagName)
        if (version.isBlank() || compareVersions(version, currentVersion) <= 0) return null

        val asset = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null
        val checksum = release.assets.firstOrNull {
            it.name.startsWith("SHA256SUMS", ignoreCase = true)
        }

        return UpdateInfo(
            version = version,
            releaseName = release.name?.takeIf { it.isNotBlank() } ?: "v$version",
            changelog = release.body
                ?.replace("\r\n", "\n")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Список изменений доступен на странице релиза.",
            releaseUrl = release.htmlUrl,
            assetName = asset.name,
            downloadUrl = asset.downloadUrl,
            assetSize = asset.size,
            checksumUrl = checksum?.downloadUrl,
            installKind = UpdateInstallKind.AndroidApk,
            supportsOneClick = true,
        )
    }

    override suspend fun downloadAndInstall(update: UpdateInfo, onProgress: (Float?) -> Unit) {
        ensureInstallPermission()

        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val fileName = update.assetName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "uust-schedule-${update.version}.apk" }
        val target = File(directory, fileName)

        try {
            val expectedChecksum = fetchChecksum(update)
            val actualChecksum = withContext(Dispatchers.IO) {
                download(update, target, onProgress)
            }
            if (expectedChecksum != null &&
                !expectedChecksum.equals(actualChecksum, ignoreCase = true)
            ) {
                target.delete()
                throw UpdateException("Файл обновления повреждён. Попробуйте скачать ещё раз.")
            }

            launchInstaller(target)
        } catch (throwable: Throwable) {
            if (throwable !is UpdateException) target.delete()
            throw throwable
        }
    }

    override fun openReleasePage(update: UpdateInfo) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (throwable: Throwable) {
            println("UUST: failed to open release page: ${throwable.message}")
        }
    }

    private fun ensureInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (throwable: Throwable) {
                println("UUST: failed to open install settings: ${throwable.message}")
            }
            throw UpdateException(
                "Разрешите установку приложений из этого источника и нажмите «Обновить» ещё раз.",
            )
        }
    }

    private suspend fun fetchChecksum(update: UpdateInfo): String? {
        val url = update.checksumUrl ?: return null
        val text = withContext(Dispatchers.IO) {
            ktor.get(url) {
                header("Accept", "text/plain")
            }.bodyAsText()
        }
        val assetName = update.assetName
        return text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.endsWith("  $assetName") || it.endsWith(" $assetName") }
            ?.substringBefore(' ')
            ?.takeIf { it.length == 64 }
    }

    private suspend fun download(
        update: UpdateInfo,
        target: File,
        onProgress: (Float?) -> Unit,
    ): String {
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "UUSTSchedule-Updater")
            setRequestProperty("Accept", "application/octet-stream")
            connect()
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw UpdateException("GitHub вернул ошибку $status.")
            }

            val total = connection.contentLengthLong
                .takeIf { it > 0 }
                ?: update.assetSize
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                            }
                        } else {
                            onProgress(null)
                        }
                    }
                }
            }

            if (update.assetSize > 0 && downloaded != update.assetSize) {
                throw UpdateException("Размер загруженного файла не совпал. Попробуйте ещё раз.")
            }

            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        } catch (throwable: Throwable) {
            target.delete()
            throw throwable
        } finally {
            connection.disconnect()
        }
    }

    private fun launchInstaller(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
