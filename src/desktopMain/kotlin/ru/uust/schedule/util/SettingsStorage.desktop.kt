package ru.uust.schedule.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Properties

actual fun createSettingsStorage(): SettingsStorage = DesktopSettingsStorage()

private class DesktopSettingsStorage : SettingsStorage {
    private val file: Path by lazy {
        val base = System.getenv("XDG_CONFIG_HOME")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
            ?: Path.of(System.getProperty("user.home"), ".config")
        base.resolve("uust-schedule").resolve("settings.properties").also {
            Files.createDirectories(it.parent)
        }
    }

    private val properties: Properties by lazy {
        Properties().apply {
            if (Files.exists(file)) {
                try {
                    Files.newInputStream(file).use(::load)
                } catch (throwable: Throwable) {
                    println("UUST: failed to read settings: ${throwable.message}")
                }
            }
        }
    }

    @Synchronized
    override fun getString(key: String): String? = try {
        properties.getProperty(key)
    } catch (throwable: Throwable) {
        println("UUST: failed to read settings: ${throwable.message}")
        null
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        try {
            val temporary = file.resolveSibling("${file.fileName}.tmp")
            Files.newOutputStream(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                properties.store(output, null)
            }
            try {
                Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (throwable: Throwable) {
            println("UUST: failed to write settings: ${throwable.message}")
        }
    }
}
