package ru.uust.schedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.uust.schedule.data.Lesson
import ru.uust.schedule.util.formatShortDate

private val TimeColumnWidth = 84.dp
private val MinDayColumnWidth = 125.dp
private val MinCellHeight = 88.dp

private val StandardPairTimes = mapOf(
    1 to "08:00-09:20",
    2 to "09:35-10:55",
    3 to "11:35-12:55",
    4 to "13:10-14:30",
    5 to "15:10-16:30",
    6 to "16:45-18:05",
    7 to "18:20-19:40",
)

/**
 * Desktop timetable: weekdays are columns, pair numbers are rows.
 *
 * Empty cells are rendered intentionally and all cells in a row share the same height,
 * so the result reads as a real timetable rather than a list of lessons.
 */
@Composable
fun Timetable(
    state: ScheduleState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val days = state.availableWeekdays
    val pairs = state.pairs
    val lessonsByCell = state.lessonsByCell
    val allLessons = state.visibleLessons
    val dateLabels = days.associateWith { day ->
        state.dateForWeekday(day)?.let(::formatShortDate)
    }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val minWidth = TimeColumnWidth + MinDayColumnWidth * days.size.toFloat()
        val tableWidth = maxOf(maxWidth, minWidth)

        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll),
        ) {
            Box(
                Modifier.horizontalScroll(horizontalScroll),
            ) {
                Column(
                    Modifier
                        .width(tableWidth)
                        .padding(bottom = 24.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = scheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.38f)),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            TimetableHeader(
                                days = days,
                                selectedWeek = state.selectedWeek,
                                dateLabels = dateLabels,
                                lessons = allLessons,
                                today = state.todayWeekday,
                            )
                            pairs.forEach { pair ->
                                TimetableRow(
                                    pair = pair,
                                    days = days,
                                    lessonsByCell = lessonsByCell,
                                    allLessons = allLessons,
                                    today = state.todayWeekday,
                                    showWeeks = state.selectedWeek == null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableHeader(
    days: List<Int>,
    selectedWeek: Int?,
    dateLabels: Map<Int, String?>,
    lessons: List<Lesson>,
    today: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val lineColor = scheme.outlineVariant.copy(alpha = 0.32f)

    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp),
    ) {
        Box(
            Modifier
                .width(TimeColumnWidth)
                .fillMaxHeight()
                .background(scheme.surfaceContainerHigh)
                .gridBorder(lineColor)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "ПАРА",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    selectedWeek?.let { "нед. $it" } ?: "все",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        days.forEach { day ->
            val accent = weekdayAccent(day, scheme)
            val count = lessons.count { it.weekday == day }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (day == today) {
                            accent.copy(alpha = 0.09f)
                        } else {
                            scheme.surfaceContainerHigh
                        }
                    )
                    .gridBorder(lineColor)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    weekdayShort(day),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                )
                            }
                        }
                        Text(
                            lessonCountText(count),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Text(
                        weekdayName(day),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = listOfNotNull(
                        dateLabels[day],
                        "сегодня".takeIf { day == today },
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day == today) accent else scheme.onSurfaceVariant,
                            fontWeight = if (day == today) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableRow(
    pair: Int,
    days: List<Int>,
    lessonsByCell: Map<Pair<Int, Int>, List<Lesson>>,
    allLessons: List<Lesson>,
    today: Int,
    showWeeks: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val lineColor = scheme.outlineVariant.copy(alpha = 0.32f)
    val time = pairTime(pair, allLessons)

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        Box(
            Modifier
                .width(TimeColumnWidth)
                .fillMaxHeight()
                .defaultMinSize(minHeight = MinCellHeight)
                .background(scheme.surfaceContainer)
                .gridBorder(lineColor)
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    pair.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = scheme.primary,
                )
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        days.forEach { day ->
            TimetableCell(
                day = day,
                lessons = lessonsByCell[day to pair].orEmpty(),
                isToday = day == today,
                showWeeks = showWeeks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = MinCellHeight),
            )
        }
    }
}

@Composable
private fun TimetableCell(
    day: Int,
    lessons: List<Lesson>,
    isToday: Boolean,
    showWeeks: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = weekdayAccent(day, scheme)
    val lineColor = scheme.outlineVariant.copy(alpha = 0.32f)

    Box(
        modifier = modifier
            .background(
                if (isToday) {
                    accent.copy(alpha = 0.035f)
                } else {
                    scheme.surfaceContainerLowest
                }
            )
            .gridBorder(lineColor)
            .padding(6.dp),
    ) {
        if (lessons.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                )
            }
        } else if (lessons.size == 1) {
            TimetableLessonCard(
                lesson = lessons.first(),
                accent = lessonAccent(lessons.first().type),
                showWeeks = showWeeks,
                fillHeight = true,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lessons.forEachIndexed { index, lesson ->
                    TimetableLessonCard(
                        lesson = lesson,
                        accent = lessonAccent(lesson.type),
                        showWeeks = showWeeks,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (index < lessons.lastIndex) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableLessonCard(
    lesson: Lesson,
    accent: Color,
    showWeeks: Boolean,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f).compositeOver(scheme.surfaceContainerLowest),
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        if (fillHeight) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            ) {
                LessonTypeRow(
                    lesson = lesson,
                    accent = accent,
                    showWeeks = showWeeks,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        lesson.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LessonMetaRow(lesson = lesson)
            }
        } else {
            Column(
                Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LessonTypeRow(
                    lesson = lesson,
                    accent = accent,
                    showWeeks = showWeeks,
                )
                Text(
                    lesson.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                LessonMetaRow(lesson = lesson)
            }
        }
    }
}

@Composable
private fun LessonTypeRow(
    lesson: Lesson,
    accent: Color,
    showWeeks: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            shortLessonType(lesson.type),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showWeeks && lesson.weeks.isNotEmpty()) {
            Text(
                "нед. ${formatWeeks(lesson.weeks)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LessonMetaRow(lesson: Lesson) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Rounded.MeetingRoom,
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = scheme.onSurfaceVariant,
        )
        Text(
            lesson.room,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (lesson.teacher.isNotBlank()) {
            Text(
                "·",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Text(
                lesson.teacher,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun pairTime(pair: Int, lessons: List<Lesson>): String =
    lessons.firstOrNull {
        it.pair == pair && it.time.isNotBlank() && it.time != "Время не указано"
    }?.time ?: StandardPairTimes[pair].orEmpty()

private fun Modifier.gridBorder(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawLine(
        color = color,
        start = Offset(size.width - stroke / 2f, 0f),
        end = Offset(size.width - stroke / 2f, size.height),
        strokeWidth = stroke,
    )
    drawLine(
        color = color,
        start = Offset(0f, size.height - stroke / 2f),
        end = Offset(size.width, size.height - stroke / 2f),
        strokeWidth = stroke,
    )
}
