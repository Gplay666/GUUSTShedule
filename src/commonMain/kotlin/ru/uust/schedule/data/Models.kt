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
)

fun GroupDto.toGroup(): Group = Group(
    id = groupId,
    title = groupTitle.trim().ifBlank { "Группа $groupId" },
    faculty = faculty?.trim().orEmpty(),
    course = course,
)

fun LessonDto.toLesson(sequence: Int): Lesson? {
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
    )
}
