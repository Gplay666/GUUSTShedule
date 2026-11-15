package ru.uust.schedule.update

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.uust.schedule.util.SettingsStorage
import ru.uust.schedule.util.createSettingsStorage

@Stable
class UpdateState(
    private val updater: AppUpdater?,
    private val scope: CoroutineScope,
    private val settings: SettingsStorage = createSettingsStorage(),
) {
    val currentVersion: String = currentAppVersion()

    var available by mutableStateOf<UpdateInfo?>(null)
        private set

    var visible by mutableStateOf(false)
        private set

    var isDownloading by mutableStateOf(false)
        private set

    var progress by mutableStateOf<Float?>(null)
        private set

    var statusMessage by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var checked = false
    private var installJob: Job? = null

    fun checkForUpdates() {
        val updater = updater ?: return
        if (checked) return
        checked = true
        scope.launch {
            val update = try {
                updater.check(currentVersion)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                println("UUST: update check failed: ${throwable.message}")
                null
            } ?: return@launch

            val skippedVersion = settings.getString(SKIPPED_VERSION_KEY)
            if (skippedVersion != null && compareVersions(update.version, skippedVersion) <= 0) {
                return@launch
            }

            available = update
            visible = true
        }
    }

    /**
     * Hides the dialog. Closing while a download is running cancels it, which
     * is the least surprising behaviour for back/Escape and the close button.
     */
    fun dismiss() {
        installJob?.cancel()
        installJob = null
        isDownloading = false
        progress = null
        error = null
        visible = false
    }

    fun skipCurrentVersion() {
        val version = available?.version ?: return
        settings.putString(SKIPPED_VERSION_KEY, version)
        dismiss()
        available = null
    }

    fun install() {
        val update = available ?: return
        val updater = updater ?: return

        if (!update.supportsOneClick) {
            updater.openReleasePage(update)
            return
        }

        if (isDownloading) return
        installJob = scope.launch {
            isDownloading = true
            error = null
            progress = 0f
            statusMessage = "Скачиваем обновление…"
            try {
                updater.downloadAndInstall(update) { value ->
                    scope.launch {
                        if (!isDownloading) return@launch
                        progress = value
                        if ((value ?: 0f) >= 1f) {
                            statusMessage = "Проверяем и устанавливаем…"
                        }
                    }
                }

                // Desktop exits the process after starting the update helper.
                // Android returns after opening the system installer, so close
                // the dialog and let the installer take over.
                if (isDownloading) {
                    isDownloading = false
                    progress = null
                    statusMessage = ""
                    visible = false
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                println("UUST: update install failed: ${throwable.message}")
                isDownloading = false
                progress = null
                error = if (throwable is UpdateException) {
                    throwable.message
                } else {
                    "Не удалось установить обновление. Попробуйте ещё раз."
                }
            }
        }
    }

    fun clearError() {
        error = null
    }

    private companion object {
        const val SKIPPED_VERSION_KEY = "update_skipped_version"
    }
}
