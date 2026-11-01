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
    // Normalize once instead of doing an ignore-case scan for every pattern.
    val normalized = type.trim().lowercase()
    return when {
        normalized.contains("лекция + практика + лабораторная") ->
            LessonTypeColors.LecturePracticeLab
        normalized.contains("лекция + практика") ->
            LessonTypeColors.LecturePractice
        normalized.contains("защита") ->
            LessonTypeColors.Defense
        normalized.contains("консультац") ->
            LessonTypeColors.Consultation
        normalized.contains("лаборатор") ->
            LessonTypeColors.Lab
        normalized.contains("экзамен") ->
            LessonTypeColors.Exam
        normalized.contains("зачёт с оценкой") ||
            normalized.contains("зачет с оценкой") ->
            LessonTypeColors.TestWithGrade
        normalized.contains("зачёт") ||
            normalized.contains("зачет") ->
            LessonTypeColors.Test
        normalized.contains("производственная практика") ->
            LessonTypeColors.ProductionPractice
        normalized.contains("практика") ||
            normalized.contains("семинар") ->
            LessonTypeColors.Practice
        normalized.contains("лекция") ||
            normalized.contains("лек") ->
            LessonTypeColors.Lecture
        normalized.contains("физвоспитание") ->
            LessonTypeColors.PhysicalEducation
        normalized.contains("военная подготовка") ->
            LessonTypeColors.Military
        normalized.contains("иностранный язык") ->
            LessonTypeColors.ForeignLanguage
        normalized.contains("мероприятие") ->
            LessonTypeColors.Event
        normalized.contains("ликвидация") ->
            LessonTypeColors.Liquidation
        normalized.contains("кураторский час") ->
            LessonTypeColors.CuratorHour
        normalized.contains("прочее") ->
            LessonTypeColors.Other
        else -> LessonTypeColors.Unknown
    }
}
