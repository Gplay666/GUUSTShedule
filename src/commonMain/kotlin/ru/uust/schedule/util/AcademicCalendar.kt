package ru.uust.schedule.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Academic calendar for semester 241 (2026/2027).
 *
 * Week 1 starts on Monday 2026-08-31. Keep this date in sync with the semester
 * configuration described in API_docs.md.
 */
object AcademicCalendar {
    val SemesterStart: LocalDate = LocalDate(2026, 8, 31)

    fun weekStart(week: Int): LocalDate =
        SemesterStart.plus((week - 1) * 7, DateTimeUnit.DAY)

    fun weekEnd(week: Int): LocalDate =
        weekStart(week).plus(6, DateTimeUnit.DAY)

    fun weekdayDate(week: Int, weekday: Int): LocalDate =
        weekStart(week).plus((weekday - 1).coerceIn(0, 6), DateTimeUnit.DAY)

    fun weekNumber(date: LocalDate): Int {
        val days = date.toEpochDays() - SemesterStart.toEpochDays()
        return (days / 7 + 1).toInt().coerceIn(1, 40)
    }
}

fun formatShortDate(date: LocalDate): String {
    val iso = date.toString()
    return "${iso.substring(8, 10)}.${iso.substring(5, 7)}"
}

fun formatFullDate(date: LocalDate): String {
    val iso = date.toString()
    return "${iso.substring(8, 10)}.${iso.substring(5, 7)}.${iso.substring(0, 4)}"
}

fun formatWeekRange(week: Int): String {
    val start = AcademicCalendar.weekStart(week)
    val end = AcademicCalendar.weekEnd(week)
    return if (start.toString().take(4) == end.toString().take(4)) {
        "${formatShortDate(start)}–${formatShortDate(end)}"
    } else {
        "${formatFullDate(start)}–${formatFullDate(end)}"
    }
}
