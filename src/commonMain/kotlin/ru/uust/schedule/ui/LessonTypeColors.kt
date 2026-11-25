package ru.uust.schedule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * Night City's lesson colours are a curated subset of the NCDI accents.
 *
 * The palette intentionally does not reuse the full university rainbow: on a
 * near-black canvas each hue is used as a small signal (a type label and a
 * thin rule), never as a card fill. This keeps the timetable reading as one
 * composition while still making lesson types recognisable at a glance.
 */
internal object NightCityLessonColors {
    val Attention = Color(0xFFFCEE0A)
    val Information = Color(0xFF00E5FF)
    val Laboratory = Color(0xFFFF2E9C)
    val Alert = Color(0xFFFF3B4E)
    val Support = Color(0xFF9B5DE5)
    val Growth = Color(0xFF55E58A)
    val Warm = Color(0xFFFFB020)
    val Muted = Color(0xFFA7ABB8)
    val Unknown = Color(0xFF6D7280)
}

@Composable
internal fun rememberLessonAccent(type: String): Color {
    val palette = LocalThemePalette.current
    return remember(type, palette) {
        when (palette) {
            ThemePalette.NightCity -> nightCityLessonAccent(type)
            else -> lessonAccent(type)
        }
    }
}

internal fun nightCityLessonAccent(type: String): Color {
    val normalized = type.trim().lowercase()
    return when {
        normalized.contains("лекция + практика + лабораторная") ->
            NightCityLessonColors.Support
        normalized.contains("лекция + практика") ->
            NightCityLessonColors.Warm
        normalized.contains("защита") ->
            NightCityLessonColors.Alert
        normalized.contains("консультац") ->
            NightCityLessonColors.Support
        normalized.contains("лаборатор") ->
            NightCityLessonColors.Laboratory
        normalized.contains("экзамен") ->
            NightCityLessonColors.Alert
        normalized.contains("зачёт с оценкой") ||
            normalized.contains("зачет с оценкой") ->
            NightCityLessonColors.Alert
        normalized.contains("зачёт") ||
            normalized.contains("зачет") ->
            NightCityLessonColors.Warm
        normalized.contains("производственная практика") ->
            NightCityLessonColors.Information
        normalized.contains("практика") ||
            normalized.contains("семинар") ->
            NightCityLessonColors.Information
        normalized.contains("лекция") ||
            normalized.contains("лек") ->
            NightCityLessonColors.Attention
        normalized.contains("физвоспитание") ->
            NightCityLessonColors.Growth
        normalized.contains("военная подготовка") ->
            NightCityLessonColors.Growth
        normalized.contains("иностранный язык") ->
            NightCityLessonColors.Warm
        normalized.contains("мероприятие") ->
            NightCityLessonColors.Support
        normalized.contains("ликвидация") ->
            NightCityLessonColors.Muted
        normalized.contains("кураторский час") ->
            NightCityLessonColors.Growth
        normalized.contains("прочее") ->
            NightCityLessonColors.Muted
        else -> NightCityLessonColors.Unknown
    }
}
