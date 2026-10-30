package ru.uust.schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.uust.schedule.data.Group
import ru.uust.schedule.data.Lesson
import ru.uust.schedule.util.formatWeekRange

@Composable
fun MenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Открыть меню",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun BrandMark(
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) {
            leading()
        } else {
            Box(
                modifier = Modifier
                    .size(if (compact) 44.dp else 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = UustLogoMark,
                    contentDescription = "Логотип УУНиТ",
                    modifier = Modifier.fillMaxSize(0.62f),
                )
            }
        }
        Column {
            Text(
                "УУНиТ",
                style = if (compact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Black,
            )
            Text(
                "расписание",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun GroupSearch(
    state: ScheduleState,
    modifier: Modifier = Modifier,
    showHint: Boolean = true,
    trailingActions: (@Composable () -> Unit)? = null,
    onGroupSelected: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val showSuggestions = state.query.isNotBlank() &&
        state.query.trim() != state.selectedGroup?.title

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = state::onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text("Найти группу", maxLines = 1) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { state.onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        state.filteredGroups.firstOrNull()?.let {
                            state.selectGroup(it)
                            focusManager.clearFocus()
                            onGroupSelected?.invoke()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            trailingActions?.invoke()
        }

        if (showHint && !showSuggestions && state.groups.isNotEmpty()) {
            Text(
                "${state.groups.size} групп · начните вводить название",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        AnimatedVisibility(
            visible = showSuggestions,
            enter = fadeIn(tween(160)) + expandVertically(animationSpec = AppMotion.spatial()),
            exit = fadeOut(tween(100)) + shrinkVertically(animationSpec = AppMotion.spatial()),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 3.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                ),
            ) {
                if (state.filteredGroups.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Попробуйте название группы или факультет.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(state.filteredGroups, key = { it.id }) { group ->
                            GroupSuggestion(
                                group = group,
                                selected = state.selectedGroup?.id == group.id,
                                onClick = {
                                    state.selectGroup(group)
                                    focusManager.clearFocus()
                                    onGroupSelected?.invoke()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSuggestion(
    group: Group,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = AppMotion.effects(),
        label = "suggestionBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    group.title.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                group.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (group.subtitle.isNotBlank()) {
                Text(
                    group.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun SelectedGroupCard(
    state: ScheduleState,
    modifier: Modifier = Modifier,
) {
    val group = state.selectedGroup
    val shape = MaterialTheme.shapes.extraLarge
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = AppMotion.spatial())
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    color = primary.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width * 1.02f, -size.height * 0.1f),
                )
                drawCircle(
                    color = tertiary.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.7f, size.height * 1.1f),
                )
            }
            .padding(22.dp),
    ) {
        if (group == null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ГРУППА",
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp,
                )
                Text(
                    "Ещё не выбрана",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                )
                Text(
                    "Найдите свою группу в поиске выше.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.8f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ГРУППА",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = CircleShape,
                        color = onContainer.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = onContainer,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = group,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 6 }) togetherWith
                            fadeOut(tween(120))
                    },
                    label = "selectedGroup",
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = onContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            current.subtitle.ifBlank { "УУНиТ" },
                            style = MaterialTheme.typography.labelLarge,
                            color = onContainer.copy(alpha = 0.78f),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaPill(
                        lessonCountText(state.visibleLessons.size),
                        container = onContainer.copy(alpha = 0.10f),
                        content = onContainer,
                    )
                    if (state.selectedWeek != null) {
                        MetaPill(
                            "нед. ${state.selectedWeek}",
                            container = onContainer.copy(alpha = 0.10f),
                            content = onContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekSelector(
    state: ScheduleState,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val selectedWeek = state.selectedWeek
    val canGoPrevious = state.canMoveWeek(-1)
    val canGoNext = state.canMoveWeek(1)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "УЧЕБНАЯ НЕДЕЛЯ",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(
                onClick = { state.moveWeek(-1) },
                enabled = canGoPrevious,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = "Предыдущая неделя",
                )
            }

            Box(Modifier.weight(1f)) {
                WeekDropdownTrigger(
                    selectedWeek = selectedWeek,
                    expanded = expanded,
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(min = 280.dp, max = 400.dp),
                    scrollState = rememberScrollState(),
                ) {
                    WeekMenuItem(
                        title = "Все недели",
                        subtitle = "весь семестр",
                        selected = selectedWeek == null,
                        onClick = {
                            state.selectWeek(null)
                            expanded = false
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = scheme.outlineVariant.copy(alpha = 0.35f),
                    )
                    state.weeks.forEach { week ->
                        WeekMenuItem(
                            title = "Неделя $week",
                            subtitle = formatWeekRange(week),
                            selected = selectedWeek == week,
                            onClick = {
                                state.selectWeek(week)
                                expanded = false
                            },
                        )
                    }
                }
            }

            FilledTonalIconButton(
                onClick = { state.moveWeek(1) },
                enabled = canGoNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "Следующая неделя",
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = state::selectCurrentWeek,
                enabled = state.currentWeekAvailable && selectedWeek != state.academicWeek,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "К текущей неделе",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.currentWeekAvailable) {
                Text(
                    "сейчас · нед. ${state.academicWeek}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeekDropdownTrigger(
    selectedWeek: Int?,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    selectedWeek?.let { "Неделя $it" } ?: "Все недели",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    selectedWeek?.let(::formatWeekRange) ?: "весь семестр",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun WeekMenuItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun ThemeButton(state: ScheduleState) {
    FilledTonalIconButton(
        onClick = state::toggleTheme,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (state.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = if (state.isDark) "Светлая тема" else "Тёмная тема",
        )
    }
}

@Composable
fun RefreshButton(state: ScheduleState) {
    FilledTonalIconButton(
        onClick = { state.loadSchedule(forceRefresh = true) },
        enabled = !state.isLoadingSchedule && state.selectedGroup != null,
        modifier = Modifier.size(48.dp),
    ) {
        if (state.isLoadingSchedule) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.Rounded.Refresh, contentDescription = "Обновить расписание")
        }
    }
}

@Composable
fun SettingsButton(
    state: ScheduleState,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier) {
        if (showLabel) {
            FilledTonalButton(
                onClick = { showSettings = true },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Настройки")
            }
        } else {
            FilledTonalIconButton(
                onClick = { showSettings = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
            }
        }
    }

    if (showSettings) {
        LaunchedEffect(Unit) {
            state.refreshSystemTheme()
        }

        Dialog(onDismissRequest = { showSettings = false }) {
            UustScheduleTheme(
                darkTheme = state.isDark,
                palette = state.palette,
                systemAccent = state.systemAccent,
            ) {
                Surface(
                    modifier = Modifier.widthIn(min = 420.dp, max = 540.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Настройки",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = { showSettings = false }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Закрыть",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        SettingsSectionLabel("Тема")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeModeChip(
                                label = "Светлая",
                                icon = Icons.Rounded.LightMode,
                                selected = state.themeMode == ThemeMode.Light,
                                onClick = { state.chooseThemeMode(ThemeMode.Light) },
                            )
                            ThemeModeChip(
                                label = "Тёмная",
                                icon = Icons.Rounded.DarkMode,
                                selected = state.themeMode == ThemeMode.Dark,
                                onClick = { state.chooseThemeMode(ThemeMode.Dark) },
                            )
                            ThemeModeChip(
                                label = "Автоматически",
                                icon = Icons.Rounded.BrightnessAuto,
                                selected = state.themeMode == ThemeMode.System,
                                onClick = { state.chooseThemeMode(ThemeMode.System) },
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )

                        SettingsSectionLabel("Цветовая палитра")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ThemePalette.entries.forEach { palette ->
                                PaletteSwatch(
                                    palette = palette,
                                    selected = state.palette == palette,
                                    systemAccent = state.systemAccent,
                                    onClick = { state.choosePalette(palette) },
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
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun PaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    systemAccent: Color?,
    onClick: () -> Unit,
) {
    val colors = if (palette == ThemePalette.System) {
        listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            systemAccent ?: MaterialTheme.colorScheme.primary,
        )
    } else {
        palette.swatches
    }
    val ringColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = palette.displayName }
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = ringColor,
                shape = CircleShape,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.first()),
            contentAlignment = Alignment.Center,
        ) {
            if (colors.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.64f)
                        .clip(CircleShape)
                        .background(colors[1]),
                    contentAlignment = Alignment.Center,
                ) {
                    if (colors.size > 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.48f)
                                .clip(CircleShape)
                                .background(colors[2]),
                        )
                    }
                    if (palette == ThemePalette.System) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Devices,
                                contentDescription = null,
                                tint = Color(0xFF4D19CC),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ExpressiveLoader()
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpressiveLoader() {
    val transition = rememberInfiniteTransition(label = "loader")
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEachIndexed { index, color ->
            val scale by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.6f at 0
                        1.2f at 300
                        0.6f at 600
                        0.6f at 900
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 150),
                ),
                label = "loaderDot$index",
            )
            Box(
                Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            ),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(
                Modifier.padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Text(
                    "Не получилось загрузить",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                FilledTonalButton(
                    onClick = onRetry,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Попробовать снова")
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Повторить")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun OfflineBanner(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Нет подключения",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Показаны сохранённые данные. Проверьте сеть и обновите вручную.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                )
            }
            TextButton(
                onClick = onRefresh,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text("Обновить")
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(92.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(onClick = onAction, shape = CircleShape) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun DaySection(
    day: Int,
    lessons: List<Lesson>,
    compact: Boolean,
    dateLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = weekdayAccent(day, scheme)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
                modifier = Modifier.size(if (compact) 48.dp else 56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        weekdayShort(day),
                        style = if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    weekdayName(day),
                    style = if (compact) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    listOfNotNull(dateLabel, lessonCountText(lessons.size)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                lessons.size.toString().padStart(2, '0'),
                style = if (compact) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                fontWeight = FontWeight.Black,
                color = accent.copy(alpha = 0.35f),
            )
        }

        lessons.forEach { lesson ->
            LessonCard(lesson, compact)
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = lessonAccent(lesson.type)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = if (compact) RoundedCornerShape(24.dp) else RoundedCornerShape(28.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interaction),
        shape = shape,
        color = accent.copy(alpha = 0.10f).compositeOver(scheme.surfaceContainerLow),
        contentColor = scheme.onSurface,
        tonalElevation = if (hovered) 2.dp else 0.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.22f),
        ),
    ) {
        Row(
            Modifier.padding(if (compact) 14.dp else 18.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
        ) {
            Column(
                Modifier.width(if (compact) 68.dp else 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = accent.copy(alpha = 0.13f),
                    contentColor = accent,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            lesson.pair?.toString() ?: "—",
                            style = if (compact) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.headlineSmall
                            },
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "пара",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.8f),
                        )
                    }
                }
                Text(
                    lesson.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TypeBadge(shortLessonType(lesson.type), accent)
                    if (lesson.weeks.isNotEmpty()) {
                        Text(
                            "нед. ${formatWeeks(lesson.weeks)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    lesson.subject,
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfoItem(Icons.Rounded.Person, lesson.teacher, compact)
                    InfoItem(Icons.Rounded.MeetingRoom, lesson.room, compact)
                }
                if (lesson.comment.isNotBlank()) {
                    Text(
                        lesson.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.13f),
        contentColor = color,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.widthIn(max = if (compact) 230.dp else 340.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaPill(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

internal fun weekdayAccent(day: Int, scheme: ColorScheme): Color = when (day) {
    1, 4 -> scheme.primary
    2, 5 -> scheme.secondary
    3, 6 -> scheme.tertiary
    else -> scheme.primary
}

internal fun shortLessonType(type: String): String = when {
    type.contains("Лекция + практика + лабораторная", ignoreCase = true) ->
        "Лекция + практика + лабораторная"
    type.contains("Лекция + практика", ignoreCase = true) -> "Лекция + практика"
    type.contains("Лабораторная", ignoreCase = true) -> "Лабораторная"
    type.contains("Практика", ignoreCase = true) -> "Практика"
    type.contains("Семинар", ignoreCase = true) -> "Семинар"
    type.contains("Лекция", ignoreCase = true) -> "Лекция"
    else -> type
}

fun weekdayName(day: Int): String = when (day) {
    1 -> "Понедельник"
    2 -> "Вторник"
    3 -> "Среда"
    4 -> "Четверг"
    5 -> "Пятница"
    6 -> "Суббота"
    7 -> "Воскресенье"
    else -> "День $day"
}

fun weekdayShort(day: Int): String = when (day) {
    1 -> "ПН"
    2 -> "ВТ"
    3 -> "СР"
    4 -> "ЧТ"
    5 -> "ПТ"
    6 -> "СБ"
    7 -> "ВС"
    else -> "—"
}

fun lessonCountText(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "занятие"
        mod10 in 2..4 && mod100 !in 12..14 -> "занятия"
        else -> "занятий"
    }
    return "$count $word"
}

fun formatWeeks(weeks: List<Int>): String {
    if (weeks.isEmpty()) return "—"
    val numbers = weeks.distinct().sorted()
    val parts = mutableListOf<String>()
    var start = numbers.first()
    var previous = start

    for (number in numbers.drop(1)) {
        if (number == previous + 1) {
            previous = number
            continue
        }
        parts += if (start == previous) "$start" else "$start–$previous"
        start = number
        previous = number
    }
    parts += if (start == previous) "$start" else "$start–$previous"
    return parts.joinToString(", ")
}
