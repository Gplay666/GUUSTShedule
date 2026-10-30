package ru.uust.schedule.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

actual fun createCacheStorage(): CacheStorage = DesktopCacheStorage()

private class DesktopCacheStorage : CacheStorage {
    private val directory: Path by lazy {
        val base = System.getenv("XDG_CACHE_HOME")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
            ?: Path.of(System.getProperty("user.home"), ".cache")
        base.resolve("uust-schedule").also { Files.createDirectories(it) }
    }

    override fun read(name: String): String? = try {
        val path = directory.resolve(name)
        if (Files.exists(path)) Files.readString(path) else null
    } catch (throwable: Throwable) {
        println("UUST: failed to read cache $name: ${throwable.message}")
        null
    }

    override fun write(name: String, content: String) {
        try {
            val target = directory.resolve(name)
            val temporary = directory.resolve("$name.tmp")
            Files.writeString(
                temporary,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (throwable: Throwable) {
            println("UUST: failed to write cache $name: ${throwable.message}")
        }
    }
}
