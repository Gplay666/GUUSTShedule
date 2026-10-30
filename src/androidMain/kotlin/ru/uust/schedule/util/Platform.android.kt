package ru.uust.schedule.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

actual fun currentWeekday(): Int = currentDate().dayOfWeek.ordinal + 1

actual fun currentDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

actual fun currentAcademicWeek(): Int =
    AcademicCalendar.weekNumber(currentDate())
