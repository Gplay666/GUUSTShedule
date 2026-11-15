package ru.uust.schedule.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GroupDto(
    @SerialName("group_id") val groupId: Int,
    @SerialName("group_title") val groupTitle: String = "",
    val faculty: String? = null,
    val course: Int? = null,
)

@Immutable
@Serializable
data class Group(
    val id: Int,
    val title: String,
    val faculty: String,
    val course: Int?,
) {
    /** Precomputed because it is read by every suggestion and selected-group card. */
    @Transient
    val subtitle: String = listOfNotNull(
        faculty.takeIf { it.isNotBlank() },
        course?.let { "$it курс" },
    ).joinToString(" · ")
}

@Serializable
data class TeacherUnitDto(
    val code: Int? = null,
    val title: String? = null,
)

@Serializable
data class TeacherDto(
    @SerialName("isu_person_id") val id: Int,
    val surname: String = "",
    val name: String = "",
    val patronymic: String = "",
    val unit: List<TeacherUnitDto> = emptyList(),
)

@Immutable
@Serializable
data class Teacher(
    val id: Int,
    val title: String,
    /** Departments joined with " · "; may be blank when the API has no unit. */
    val department: String,
) {
    @Transient
    val subtitle: String = department
}

enum class TargetKind(val scheduleType: Int, val storageKey: String) {
    Group(0, "group"),
    Teacher(1, "teacher"),
}

/** Stable identity of the currently selected schedule owner. */
@Immutable
data class SelectedTarget(
    val kind: TargetKind,
    val id: Int,
)

/** A group or a teacher shown in the shared search list. */
@Immutable
sealed interface ScheduleTarget {
    val id: Int
    val title: String
    val subtitle: String
    val kind: TargetKind

    val selection: SelectedTarget get() = SelectedTarget(kind, id)
}

@Immutable
data class GroupTarget(val group: Group) : ScheduleTarget {
    override val id: Int get() = group.id
    override val title: String get() = group.title
    override val subtitle: String get() = group.subtitle
    override val kind: TargetKind get() = TargetKind.Group
}

@Immutable
data class TeacherTarget(val teacher: Teacher) : ScheduleTarget {
    override val id: Int get() = teacher.id
    override val title: String get() = teacher.title
    override val subtitle: String get() = teacher.department
    override val kind: TargetKind get() = TargetKind.Teacher
}

@Serializable
data class LessonDto(
    val index: Int? = null,
    @SerialName("schedule_id") val scheduleId: Long? = null,
    @SerialName("schedule_weeks") val scheduleWeeks: List<String> = emptyList(),
    @SerialName("schedule_subject_title") val scheduleSubjectTitle: String = "",
    @SerialName("schedule_weekday_id") val scheduleWeekdayId: Int = 0,
    @SerialName("building_short_title") val buildingShortTitle: String? = null,
    @SerialName("building_title") val buildingTitle: String? = null,
    @SerialName("room_title") val roomTitle: String? = null,
    @SerialName("room_title_short") val roomTitleShort: String? = null,
    val type: String? = null,
    val teacher: String? = null,
    @SerialName("teacher_fullname") val teacherFullname: String? = null,
    @SerialName("student_group_number_title") val studentGroupTitle: String? = null,
    @SerialName("schedule_time_new_id") val scheduleTimeNewId: Int? = null,
    @SerialName("schedule_time_title") val scheduleTimeTitle: String? = null,
    @SerialName("schedule_time_num") val scheduleTimeNum: Int? = null,
    val comment: String? = null,
)

@Immutable
@Serializable
data class Lesson(
    val id: String,
    val subject: String,
    val weekday: Int,
    val pair: Int?,
    val time: String,
    val type: String,
    val teacher: String,
    val room: String,
    val building: String,
    val weeks: List<Int>,
    val comment: String,
    /** Filled only for teacher schedules; group schedules leave it blank. */
    val group: String = "",
)

fun GroupDto.toGroup(): Group = Group(
    id = groupId,
    title = groupTitle.trim().ifBlank { "Группа $groupId" },
    faculty = faculty?.trim().orEmpty(),
    course = course,
)

fun TeacherDto.toTeacher(): Teacher {
    val fullName = listOf(surname, name, patronymic)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
    val departments = unit
        .mapNotNull { it.title?.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    return Teacher(
        id = id,
        title = fullName.ifBlank { "Преподаватель $id" },
        department = departments.joinToString(" · "),
    )
}

fun LessonDto.toLesson(sequence: Int, includeGroup: Boolean = false): Lesson? {
    if (scheduleSubjectTitle.isBlank() || scheduleWeekdayId <= 0) return null

    val weekNumbers = scheduleWeeks
        .mapNotNull { it.toIntOrNull() }
        .distinct()
        .sorted()

    val shortRoom = roomTitleShort
        ?.trim()
        ?.takeIf { room -> room.any { it.isLetterOrDigit() } }
        .orEmpty()
    val buildingName = sequenceOf(buildingShortTitle, buildingTitle)
        .mapNotNull { it?.trim() }
        .firstOrNull { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
        .orEmpty()

    val roomName = shortRoom.ifBlank {
        listOf(buildingName, roomTitle?.trim().orEmpty())
            .filter { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
            .joinToString(", ")
    }

    val teacherName = sequenceOf(teacherFullname, teacher)
        .mapNotNull { it?.trim() }
        .firstOrNull { name -> name.any { ch -> ch.isLetter() } }
        .orEmpty()

    return Lesson(
        id = listOf(
            scheduleId ?: 0L,
            index ?: sequence,
            sequence,
            scheduleWeekdayId,
            scheduleTimeNum ?: 0,
        ).joinToString("-"),
        subject = scheduleSubjectTitle.trim(),
        weekday = scheduleWeekdayId,
        pair = scheduleTimeNum,
        time = scheduleTimeTitle?.trim().orEmpty().ifBlank { "Время не указано" },
        type = type?.trim().orEmpty().ifBlank { "Занятие" },
        teacher = teacherName.ifBlank { "Преподаватель не указан" },
        room = roomName.ifBlank { "Аудитория не указана" },
        building = buildingName,
        weeks = weekNumbers,
        comment = comment?.trim().orEmpty(),
        group = if (includeGroup) studentGroupTitle?.trim().orEmpty() else "",
    )
}
