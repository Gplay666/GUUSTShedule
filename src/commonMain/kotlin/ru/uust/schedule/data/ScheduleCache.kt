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

    fun loadTeachers(): List<Teacher>? = read<TeachersCache>(TEACHERS_FILE)?.teachers

    fun saveTeachers(teachers: List<Teacher>) {
        write(TEACHERS_FILE, TeachersCache(teachers))
    }

    /** Group schedules keep the old filename, teacher schedules use their own. */
    fun loadSchedule(target: SelectedTarget): List<Lesson>? =
        read<ScheduleFile>(scheduleFile(target))?.lessons

    fun saveSchedule(target: SelectedTarget, lessons: List<Lesson>) {
        write(scheduleFile(target), ScheduleFile(lessons))
    }

    fun loadSelectedTarget(): SelectedTarget? {
        val file = read<SelectionFile>(SELECTION_FILE) ?: return null
        val kind = file.targetType?.let { type ->
            TargetKind.entries.firstOrNull { it.storageKey == type }
        }
        if (kind != null && file.targetId != null) {
            return SelectedTarget(kind, file.targetId)
        }
        // selection.json written by <= 0.4.1 contains only groupId.
        return file.groupId?.let { SelectedTarget(TargetKind.Group, it) }
    }

    fun saveSelectedTarget(target: SelectedTarget?) {
        write(
            SELECTION_FILE,
            SelectionFile(
                groupId = target?.takeIf { it.kind == TargetKind.Group }?.id,
                targetType = target?.kind?.storageKey,
                targetId = target?.id,
            ),
        )
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

    private fun scheduleFile(target: SelectedTarget): String = when (target.kind) {
        TargetKind.Group -> "schedule_${target.id}.json"
        TargetKind.Teacher -> "teacher_schedule_${target.id}.json"
    }

    private companion object {
        const val GROUPS_FILE = "groups.json"
        const val TEACHERS_FILE = "teachers.json"
        const val SELECTION_FILE = "selection.json"
    }
}

@Serializable
private data class GroupsCache(
    val groups: List<Group>,
)

@Serializable
private data class TeachersCache(
    val teachers: List<Teacher>,
)

@Serializable
private data class ScheduleFile(
    val lessons: List<Lesson>,
)

@Serializable
private data class SelectionFile(
    /** Kept for compatibility with selection files written by 0.4.1 and older. */
    val groupId: Int? = null,
    val targetType: String? = null,
    val targetId: Int? = null,
)
