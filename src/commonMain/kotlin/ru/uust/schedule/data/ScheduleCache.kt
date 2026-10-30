package ru.uust.schedule.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.uust.schedule.util.CacheStorage
import ru.uust.schedule.util.createCacheStorage

/**
 * Persistent local copy of the last successfully loaded API data.
 *
 * The cache is intentionally best-effort: a missing or corrupt file simply
 * means the app falls back to the network instead of failing.
 */
class ScheduleCache(
    private val storage: CacheStorage = createCacheStorage(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadGroups(): List<Group>? = read<GroupsCache>(GROUPS_FILE)?.groups

    fun saveGroups(groups: List<Group>) {
        write(GROUPS_FILE, GroupsCache(groups))
    }

    fun loadSchedule(groupId: Int): List<Lesson>? =
        read<ScheduleFile>(scheduleFile(groupId))?.lessons

    fun saveSchedule(groupId: Int, lessons: List<Lesson>) {
        write(scheduleFile(groupId), ScheduleFile(lessons))
    }

    fun loadSelectedGroupId(): Int? = read<SelectionFile>(SELECTION_FILE)?.groupId

    fun saveSelectedGroupId(groupId: Int?) {
        write(SELECTION_FILE, SelectionFile(groupId))
    }

    private inline fun <reified T> read(name: String): T? = try {
        storage.read(name)?.let { json.decodeFromString<T>(it) }
    } catch (throwable: Throwable) {
        println("UUST: failed to parse cache $name: ${throwable.message}")
        null
    }

    private inline fun <reified T> write(name: String, value: T) {
        try {
            storage.write(name, json.encodeToString(value))
        } catch (throwable: Throwable) {
            println("UUST: failed to serialize cache $name: ${throwable.message}")
        }
    }

    private fun scheduleFile(groupId: Int): String = "schedule_$groupId.json"

    private companion object {
        const val GROUPS_FILE = "groups.json"
        const val SELECTION_FILE = "selection.json"
    }
}

@Serializable
private data class GroupsCache(
    val groups: List<Group>,
)

@Serializable
private data class ScheduleFile(
    val lessons: List<Lesson>,
)

@Serializable
private data class SelectionFile(
    val groupId: Int? = null,
)
