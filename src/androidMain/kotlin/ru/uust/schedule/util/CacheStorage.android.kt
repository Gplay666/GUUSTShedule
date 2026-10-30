package ru.uust.schedule.util

import java.io.File

actual fun createCacheStorage(): CacheStorage = AndroidCacheStorage()

private class AndroidCacheStorage : CacheStorage {
    private val directory: File by lazy {
        File(AndroidAppContext.context.filesDir, "uust-schedule").also { it.mkdirs() }
    }

    override fun read(name: String): String? = try {
        File(directory, name).takeIf { it.isFile }?.readText()
    } catch (throwable: Throwable) {
        println("UUST: failed to read cache $name: ${throwable.message}")
        null
    }

    override fun write(name: String, content: String) {
        try {
            directory.mkdirs()
            val target = File(directory, name)
            val temporary = File(directory, "$name.tmp")
            temporary.writeText(content)
            if (!temporary.renameTo(target)) {
                target.writeText(content)
                temporary.delete()
            }
        } catch (throwable: Throwable) {
            println("UUST: failed to write cache $name: ${throwable.message}")
        }
    }
}
