package ru.uust.schedule.update

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.uust.schedule.data.createHttpClient
import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.system.exitProcess

private const val REPOSITORY = "Gplay666/GUUSTShedule"
private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/$REPOSITORY/releases/latest"

private val isWindows = System.getProperty("os.name").orEmpty()
    .startsWith("Windows", ignoreCase = true)
private val isLinux = System.getProperty("os.name").orEmpty()
    .startsWith("Linux", ignoreCase = true)

actual fun createAppUpdater(): AppUpdater? = DesktopAppUpdater()

actual fun currentAppVersion(): String =
    System.getProperty("uust.app.version")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: System.getProperty("jpackage.app-version")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: "0.0.0"

private class DesktopAppUpdater : AppUpdater {
    private val ktor = createHttpClient()
    private val http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

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

        val selection = selectAsset(release.assets) ?: return null
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
            assetName = selection.asset.name,
            downloadUrl = selection.asset.downloadUrl,
            assetSize = selection.asset.size,
            checksumUrl = checksum?.downloadUrl,
            installKind = selection.kind,
            supportsOneClick = selection.supportsOneClick,
        )
    }

    override suspend fun downloadAndInstall(update: UpdateInfo, onProgress: (Float?) -> Unit) {
        when (update.installKind) {
            UpdateInstallKind.AppImage -> installAppImage(update, onProgress)
            UpdateInstallKind.WindowsInstaller -> installWindows(update, portable = false, onProgress)
            UpdateInstallKind.WindowsPortable -> installWindows(update, portable = true, onProgress)
            UpdateInstallKind.Manual -> throw UpdateException(
                "Для этой системы доступно только ручное обновление.",
            )
        }
    }

    override fun openReleasePage(update: UpdateInfo) {
        openUrl(update.releaseUrl)
    }

    private fun selectAsset(assets: List<GitHubAsset>): SelectedAsset? {
        if (isLinux) {
            val asset = assets.firstOrNull {
                it.name.endsWith(".AppImage", ignoreCase = true)
            } ?: return null
            return SelectedAsset(
                asset = asset,
                kind = UpdateInstallKind.AppImage,
                supportsOneClick = !System.getenv("APPIMAGE").isNullOrBlank(),
            )
        }

        if (isWindows) {
            val launcher = launcherPath()
            val installed = launcher?.parent?.let {
                Files.exists(it.resolve("uninstall.exe"))
            } == true

            return if (installed || launcher == null) {
                val asset = assets.firstOrNull {
                    it.name.endsWith("-setup.exe", ignoreCase = true)
                } ?: return null
                SelectedAsset(
                    asset = asset,
                    kind = UpdateInstallKind.WindowsInstaller,
                    supportsOneClick = launcher != null,
                )
            } else {
                val asset = assets.firstOrNull {
                    it.name.endsWith("-windows-x64.zip", ignoreCase = true)
                } ?: return null
                SelectedAsset(
                    asset = asset,
                    kind = UpdateInstallKind.WindowsPortable,
                    supportsOneClick = true,
                )
            }
        }

        return null
    }

    private suspend fun installAppImage(update: UpdateInfo, onProgress: (Float?) -> Unit) {
        val appImage = System.getenv("APPIMAGE")
            ?.takeIf { it.isNotBlank() }
            ?: throw UpdateException(
                "Автообновление доступно только для версии AppImage.",
            )
        val target = Path.of(appImage).toAbsolutePath()
        val directory = target.parent
            ?: throw UpdateException("Не удалось определить папку приложения.")

        val temporary = try {
            Files.createTempFile(directory, ".uust-update-", ".AppImage")
        } catch (throwable: Throwable) {
            throw UpdateException(
                "Не удалось создать файл рядом с приложением. Проверьте права доступа.",
            )
        }

        var helperStarted = false
        try {
            downloadVerified(update, temporary, onProgress)
            if (!temporary.toFile().setExecutable(true, false)) {
                throw UpdateException("Не удалось подготовить файл обновления.")
            }

            val helper = Files.createTempFile("uust-update-", ".sh")
            Files.writeString(
                helper,
                """
                #!/bin/sh
                while kill -0 "$1" 2>/dev/null; do sleep 0.2; done
                mv -f -- "$2" "$3" && chmod +x -- "$3"
                unset APPIMAGE APPDIR
                rm -f -- "$0"
                exec "$3"
                """.trimIndent() + "\n",
            )
            if (!helper.toFile().setExecutable(true, false)) {
                Files.deleteIfExists(helper)
                throw UpdateException("Не удалось запустить установку обновления.")
            }

            ProcessBuilder(
                "sh",
                helper.toString(),
                ProcessHandle.current().pid().toString(),
                temporary.toString(),
                target.toString(),
            ).start()
            helperStarted = true
            exitProcess(0)
        } finally {
            if (!helperStarted) Files.deleteIfExists(temporary)
        }
    }

    private suspend fun installWindows(
        update: UpdateInfo,
        portable: Boolean,
        onProgress: (Float?) -> Unit,
    ) {
        val launcher = launcherPath()
            ?: throw UpdateException("Не удалось определить путь к приложению.")
        val applicationDirectory = launcher.parent
            ?: throw UpdateException("Не удалось определить папку приложения.")

        val directory = try {
            Files.createTempDirectory("uust-schedule-update-")
        } catch (throwable: Throwable) {
            throw UpdateException("Не удалось подготовить папку для обновления.")
        }
        val archive = directory.resolve(update.assetName)

        var helperStarted = false
        try {
            downloadVerified(update, archive, onProgress)
            val script = directory.resolve("update.ps1")
            Files.writeString(
                script,
                if (portable) {
                    portableUpdateScript(launcher, applicationDirectory, archive)
                } else {
                    installerUpdateScript(launcher, archive)
                },
            )
            ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-WindowStyle",
                "Hidden",
                "-File",
                script.toString(),
            ).start()
            helperStarted = true
            exitProcess(0)
        } finally {
            if (!helperStarted) {
                runCatching { directory.toFile().deleteRecursively() }
            }
        }
    }

    private fun installerUpdateScript(launcher: Path, setup: Path): String = buildString {
        appendLine("\$ErrorActionPreference = 'SilentlyContinue'")
        appendLine("while (Get-Process -Id ${ProcessHandle.current().pid()} -ErrorAction SilentlyContinue) {")
        appendLine("    Start-Sleep -Milliseconds 250")
        appendLine("}")
        appendLine("try {")
        appendLine("    Start-Process -FilePath ${psQuote(setup)} -ArgumentList '/S' -Wait | Out-Null")
        appendLine("} catch { }")
        appendLine("Start-Process -FilePath ${psQuote(launcher)}")
        appendLine("Remove-Item -LiteralPath ${psQuote(setup)} -Force")
        appendLine("Remove-Item -LiteralPath \$MyInvocation.MyCommand.Path -Force")
    }

    private fun portableUpdateScript(
        launcher: Path,
        applicationDirectory: Path,
        archive: Path,
    ): String = buildString {
        appendLine("\$ErrorActionPreference = 'SilentlyContinue'")
        appendLine("while (Get-Process -Id ${ProcessHandle.current().pid()} -ErrorAction SilentlyContinue) {")
        appendLine("    Start-Sleep -Milliseconds 250")
        appendLine("}")
        appendLine("\$target = Join-Path \$env:TEMP ('uust-schedule-' + [guid]::NewGuid().ToString('N'))")
        appendLine("try {")
        appendLine("    New-Item -ItemType Directory -Path \$target -Force | Out-Null")
        appendLine("    Expand-Archive -LiteralPath ${psQuote(archive)} -DestinationPath \$target -Force")
        appendLine("    Copy-Item -Path (Join-Path \$target 'uust-schedule\\*') -Destination ${psQuote(applicationDirectory)} -Recurse -Force")
        appendLine("} catch { }")
        appendLine("Start-Process -FilePath ${psQuote(launcher)}")
        appendLine("Remove-Item -LiteralPath \$target -Recurse -Force")
        appendLine("Remove-Item -LiteralPath ${psQuote(archive)} -Force")
        appendLine("Remove-Item -LiteralPath \$MyInvocation.MyCommand.Path -Force")
    }

    private suspend fun downloadVerified(
        update: UpdateInfo,
        target: Path,
        onProgress: (Float?) -> Unit,
    ) {
        val expectedChecksum = try {
            fetchChecksum(update)
        } catch (throwable: Throwable) {
            println("UUST: failed to load checksum: ${throwable.message}")
            null
        }

        val actualChecksum = withContext(Dispatchers.IO) {
            download(update, target, onProgress)
        }

        if (expectedChecksum != null && !expectedChecksum.equals(actualChecksum, ignoreCase = true)) {
            Files.deleteIfExists(target)
            throw UpdateException("Файл обновления повреждён. Попробуйте скачать ещё раз.")
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

    private fun download(
        update: UpdateInfo,
        target: Path,
        onProgress: (Float?) -> Unit,
    ): String {
        val request = HttpRequest.newBuilder(URI(update.downloadUrl))
            .header("User-Agent", "UUSTSchedule-Updater")
            .header("Accept", "application/octet-stream")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            throw UpdateException("GitHub вернул ошибку ${response.statusCode()}.")
        }

        val total = response.headers()
            .firstValueAsLong("Content-Length")
            .orElse(update.assetSize)
            .takeIf { it > 0 }
            ?: update.assetSize

        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        var lastPercent = -1

        response.body().use { input ->
            Files.newOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
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
    }

    private fun launcherPath(): Path? {
        val fromJpackage = System.getProperty("jpackage.app-path")
            ?.takeIf { it.isNotBlank() }
        if (fromJpackage != null) return Path.of(fromJpackage)

        val command = ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { it.isNotBlank() }
        return command?.let(Path::of)
    }

    private fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
            ) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (throwable: Throwable) {
            println("UUST: failed to open browser: ${throwable.message}")
        }

        try {
            if (isWindows) {
                ProcessBuilder("cmd", "/c", "start", "", url).start()
            } else {
                ProcessBuilder("xdg-open", url).start()
            }
        } catch (throwable: Throwable) {
            println("UUST: failed to open release page: ${throwable.message}")
        }
    }

    private fun psQuote(path: Path): String =
        "'" + path.toString().replace("'", "''") + "'"

    private data class SelectedAsset(
        val asset: GitHubAsset,
        val kind: UpdateInstallKind,
        val supportsOneClick: Boolean,
    )
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)
