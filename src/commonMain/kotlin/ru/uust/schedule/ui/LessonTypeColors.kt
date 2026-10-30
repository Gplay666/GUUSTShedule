package ru.uust.schedule.ui

import androidx.compose.ui.graphics.Color

/**
 * Fixed lesson-type palette used by the university's schedule service.
 *
 * These values are intentionally independent of MaterialTheme/ColorScheme:
 * the same lesson type must have the same colour in light, dark, desktop and
 * mobile layouts. The hex values match the classes .type0-.type18 used by
 * schedule.uust.ru.
 */
internal object LessonTypeColors {
    val Lecture = Color(0xFFF75BB9)
    val Practice = Color(0xFFFE9835)
    val Lab = Color(0xFF2466F3)
    val PhysicalEducation = Color(0xFF9DD514)
    val Military = Color(0xFF0F7E05)
    val LecturePractice = Color(0xFFFB7A77)
    val ForeignLanguage = Color(0xFFCC1855)
    val Consultation = Color(0xFF8456DD)
    val Exam = Color(0xFFE91111)
    val Event = Color(0xFF1ABC9C)
    val Test = Color(0xFF3498DB)
    val TestWithGrade = Color(0xFF9B59B6)
    val Defense = Color(0xFFF1C40F)
    val LecturePracticeLab = Color(0xFFE67E22)
    val Other = Color(0xFFE74C3C)
    val Liquidation = Color(0xFF95A5A6)
    val CuratorHour = Color(0xFF2ECC71)
    val ProductionPractice = Color(0xFF16A085)
    val Unknown = Color(0xFF6B7280)
}

internal fun lessonAccent(type: String): Color {
    val normalized = type.trim()
    return when {
        normalized.contains("Лекция + практика + лабораторная", ignoreCase = true) ->
            LessonTypeColors.LecturePracticeLab
        normalized.contains("Лекция + практика", ignoreCase = true) ->
            LessonTypeColors.LecturePractice
        normalized.contains("Защита", ignoreCase = true) ->
            LessonTypeColors.Defense
        normalized.contains("Консультац", ignoreCase = true) ->
            LessonTypeColors.Consultation
        normalized.contains("Лаборатор", ignoreCase = true) ->
            LessonTypeColors.Lab
        normalized.contains("Экзамен", ignoreCase = true) ->
            LessonTypeColors.Exam
        normalized.contains("Зачёт с оценкой", ignoreCase = true) ||
            normalized.contains("Зачет с оценкой", ignoreCase = true) ->
            LessonTypeColors.TestWithGrade
        normalized.contains("Зачёт", ignoreCase = true) ||
            normalized.contains("Зачет", ignoreCase = true) ->
            LessonTypeColors.Test
        normalized.contains("Производственная практика", ignoreCase = true) ->
            LessonTypeColors.ProductionPractice
        normalized.contains("Практика", ignoreCase = true) ||
            normalized.contains("Семинар", ignoreCase = true) ->
            LessonTypeColors.Practice
        normalized.contains("Лекция", ignoreCase = true) ||
            normalized.contains("Лек", ignoreCase = true) ->
            LessonTypeColors.Lecture
        normalized.contains("Физвоспитание", ignoreCase = true) ->
            LessonTypeColors.PhysicalEducation
        normalized.contains("Военная подготовка", ignoreCase = true) ->
            LessonTypeColors.Military
        normalized.contains("Иностранный язык", ignoreCase = true) ->
            LessonTypeColors.ForeignLanguage
        normalized.contains("Мероприятие", ignoreCase = true) ->
            LessonTypeColors.Event
        normalized.contains("Ликвидация", ignoreCase = true) ->
            LessonTypeColors.Liquidation
        normalized.contains("Кураторский час", ignoreCase = true) ->
            LessonTypeColors.CuratorHour
        normalized.contains("Прочее", ignoreCase = true) ->
            LessonTypeColors.Other
        else -> LessonTypeColors.Unknown
    }
}
