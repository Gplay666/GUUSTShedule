package ru.uust.schedule.util

/**
 * Small platform storage abstraction used for the offline schedule cache.
 *
 * Keys are flat file names chosen by [ru.uust.schedule.data.ScheduleCache], so
 * implementations only need to read and write text blobs.
 */
interface CacheStorage {
    fun read(name: String): String?
    fun write(name: String, content: String)
}

expect fun createCacheStorage(): CacheStorage
