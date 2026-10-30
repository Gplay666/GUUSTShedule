package ru.uust.schedule.util

import kotlinx.datetime.LocalDate

/** ISO weekday number: Monday = 1, Sunday = 7. */
expect fun currentWeekday(): Int

/** Current local date. */
expect fun currentDate(): LocalDate

/** Current academic week number, 1-based. */
expect fun currentAcademicWeek(): Int
