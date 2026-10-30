package ru.uust.schedule.util

import kotlinx.datetime.LocalDate

actual fun currentWeekday(): Int = java.time.LocalDate.now().dayOfWeek.value

actual fun currentDate(): LocalDate {
    val today = java.time.LocalDate.now()
    return LocalDate(today.year, today.monthValue, today.dayOfMonth)
}

actual fun currentAcademicWeek(): Int =
    AcademicCalendar.weekNumber(currentDate())
