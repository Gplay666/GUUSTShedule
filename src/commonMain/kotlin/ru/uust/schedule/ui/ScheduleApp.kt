package ru.uust.schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.uust.schedule.data.ScheduleRepository
import ru.uust.schedule.util.formatFullDate
import ru.uust.schedule.util.formatShortDate

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScheduleApp() {
    val repository = remember { ScheduleRepository() }
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val state = remember(repository) {
        ScheduleState(
            repository = repository,
            scope = scope,
            initialSystemDark = systemDark,
        )
    }
    var systemDarkEffectStarted by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        state.initialize()
    }

    LaunchedEffect(systemDark) {
        // The first value is already passed to the state constructor. Later
        // changes come from a live configuration change.
        if (systemDarkEffectStarted) {
            state.updateSystemDark(systemDark)
        } else {
            systemDarkEffectStarted = true
        }
    }

    UustScheduleTheme(
        darkTheme = state.isDark,
        palette = state.palette,
        systemAccent = state.systemAccent,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val portrait = maxHeight > maxWidth
                val landscape = maxWidth > maxHeight
                val showSidebar = landscape && maxWidth >= 1500.dp
                val compact = portrait || !showSidebar
                val drawerState = rememberDrawerState(DrawerValue.Closed)

                BackHandler(enabled = drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                }

                LaunchedEffect(showSidebar) {
                    if (showSidebar) drawerState.close()
                }

                if (showSidebar) {
                    Row(Modifier.fillMaxSize()) {
                        Sidebar(
                            state = state,
                            modifier = Modifier
                                .width(330.dp)
                                .fillMaxHeight(),
                        )
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                        ScheduleContent(
                            state = state,
                            compact = false,
                            showHeader = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = false,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.fillMaxHeight(),
                                windowInsets = WindowInsets(0, 0, 0, 0),
                            ) {
                                Sidebar(
                                    state = state,
                                    modifier = Modifier.fillMaxSize(),
                                    onGroupSelected = {
                                        scope.launch { drawerState.close() }
                                    },
                                    onClose = {
                                        scope.launch { drawerState.close() }
                                    },
                                )
                            }
                        },
                    ) {
                        if (portrait) {
                            PortraitScheduleContent(
                                state = state,
                                onOpenMenu = {
                                    scope.launch { drawerState.open() }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                CompactTopBar(
                                    state = state,
                                    onOpenMenu = {
                                        scope.launch { drawerState.open() }
                                    },
                                )
                                ScheduleContent(
                                    state = state,
                                    compact = compact,
                                    showHeader = false,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
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
private fun Sidebar(
    state: ScheduleState,
    modifier: Modifier = Modifier,
    onGroupSelected: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BrandMark()
                GroupSearch(state, onGroupSelected = onGroupSelected)
                SelectedGroupCard(state)
                Spacer(Modifier.height(8.dp))
                SidebarFooter(state)
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            SettingsButton(
                state = state,
                showLabel = true,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .align(Alignment.Start),
            )
        }
        if (onClose != null) {
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Закрыть меню",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SidebarFooter(state: ScheduleState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeButton(state)
            Column(Modifier.weight(1f)) {
                Text(
                    when (state.themeMode) {
                        ThemeMode.Light -> "Светлая тема"
                        ThemeMode.Dark -> "Тёмная тема"
                        ThemeMode.System -> "Автоматически"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (state.themeMode == ThemeMode.System) {
                        "Тема устройства"
                    } else {
                        "Нажмите, чтобы переключить"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        WeekSelector(state)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        TextButton(
            onClick = { state.loadSchedule(forceRefresh = true) },
            enabled = state.selectedGroup != null && !state.isLoadingSchedule,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Обновить расписание")
        }
        Text(
            "Данные: dev.uust-time.ru\nПрототип 0.1 · Compose Multiplatform",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactTopBar(
    state: ScheduleState,
    onOpenMenu: () -> Unit = {},
) {
    val group = state.selectedGroup

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuButton(onClick = onOpenMenu)
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = group?.title ?: "Расписание",
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically { it / 8 }) togetherWith
                                fadeOut(tween(120))
                        },
                        label = "compactGroupTitle",
                    ) { title ->
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (group != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (group.faculty.isNotBlank()) {
                                HeaderPill(group.faculty)
                            }
                            group.course?.let {
                                HeaderPill("$it курс")
                            }
                            if (state.lessons.isNotEmpty()) {
                                HeaderPill(
                                    lessonCountText(state.visibleLessons.size),
                                    tonal = true,
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun ScheduleContent(
    state: ScheduleState,
    compact: Boolean,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            if (showHeader) {
                ScheduleHeader(state, compact)
            }

            val error = state.error
            when {
                state.isLoadingGroups && state.selectedGroup == null -> {
                    LoadingState(
                        message = "Загружаем группы…",
                        modifier = Modifier.weight(1f),
                    )
                }

                error != null && state.lessons.isEmpty() && !state.isLoadingSchedule -> {
                    ErrorState(
                        message = error,
                        onRetry = state::retry,
                        modifier = Modifier.weight(1f),
                    )
                }

                state.isLoadingSchedule && state.lessons.isEmpty() -> {
                    LoadingState(
                        message = "Загружаем расписание…",
                        modifier = Modifier.weight(1f),
                    )
                }

                state.selectedGroup == null -> {
                    EmptyState(
                        title = "Выберите группу",
                        message = if (showHeader) {
                            "Найдите свою группу в поиске, чтобы увидеть расписание."
                        } else {
                            "Откройте меню и найдите свою группу."
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    if (state.isOffline) {
                        OfflineBanner(
                            onRefresh = { state.loadSchedule(forceRefresh = true) },
                            modifier = Modifier.padding(
                                horizontal = if (compact) 20.dp else 36.dp,
                                vertical = 8.dp,
                            ),
                        )
                    }
                    if (error != null && state.lessons.isNotEmpty() && !state.isOffline) {
                        ErrorBanner(
                            message = error,
                            onRetry = state::retry,
                            onDismiss = state::clearError,
                            modifier = Modifier.padding(
                                horizontal = if (compact) 20.dp else 36.dp,
                                vertical = 8.dp,
                            ),
                        )
                    }
                    DesktopTimetable(
                        state = state,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.isLoadingSchedule && state.lessons.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun ScheduleHeader(
    state: ScheduleState,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val group = state.selectedGroup

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.92f, -size.height * 0.25f),
                )
                drawCircle(
                    color = scheme.tertiary.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.25f,
                    center = Offset(size.width * 0.72f, size.height * 1.15f),
                )
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.08f),
                        Color.Transparent,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 20.dp else 36.dp,
                vertical = if (compact) 16.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "РАСПИСАНИЕ",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = group?.title ?: "Выберите группу",
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically { it / 8 }) togetherWith
                                fadeOut(tween(120))
                        },
                        label = "headerGroup",
                    ) { title ->
                        Text(
                            title,
                            style = if (compact) {
                                MaterialTheme.typography.headlineLarge
                            } else {
                                MaterialTheme.typography.displaySmall
                            },
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        group?.faculty?.takeIf { it.isNotBlank() }?.let {
                            HeaderPill(it)
                        }
                        group?.course?.let {
                            HeaderPill("$it курс")
                        }
                        if (state.lessons.isNotEmpty()) {
                            HeaderPill(
                                lessonCountText(state.visibleLessons.size),
                                tonal = true,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                RefreshButton(state)
            }
        }
    }
}

@Composable
private fun HeaderPill(
    text: String,
    tonal: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = if (tonal) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (tonal) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun DesktopTimetable(
    state: ScheduleState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (compact) 20.dp else 36.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.visibleLessons.isEmpty()) {
            TableNotice(state)
        }
        Timetable(
            state = state,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TableNotice(state: ScheduleState) {
    val message = if (state.lessons.isEmpty()) {
        "Для этой группы пока нет занятий"
    } else {
        "На этой неделе занятий нет"
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PortraitScheduleContent(
    state: ScheduleState,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = state.selectedGroup
    val error = state.error
    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableStateOf(0) }
    val collapseFraction = when {
        group == null -> 0f
        listState.firstVisibleItemIndex > 0 -> 1f
        headerHeightPx <= 0 -> 0f
        else -> (listState.firstVisibleItemScrollOffset.toFloat() / headerHeightPx)
            .coerceIn(0f, 1f)
    }

    Column(modifier.fillMaxSize()) {
        PortraitTopBar(
            title = group?.title ?: "Расписание",
            titleAlpha = collapseFraction,
            onOpenMenu = onOpenMenu,
        )

        if (state.isLoadingSchedule && state.lessons.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
        }

        when {
            state.isLoadingGroups && group == null -> {
                LoadingState(
                    message = "Загружаем группы…",
                    modifier = Modifier.weight(1f),
                )
            }

            error != null && state.lessons.isEmpty() && !state.isLoadingSchedule -> {
                Column(Modifier.weight(1f)) {
                    if (group != null) {
                        PortraitGroupHeader(state)
                    }
                    ErrorState(
                        message = error,
                        onRetry = state::retry,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            group == null -> {
                EmptyState(
                    title = "Выберите группу",
                    message = "Откройте меню и найдите свою группу.",
                    modifier = Modifier.weight(1f),
                )
            }

            state.isLoadingSchedule && state.lessons.isEmpty() -> {
                Column(Modifier.weight(1f)) {
                    PortraitGroupHeader(state)
                    LoadingState(
                        message = "Загружаем расписание…",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            else -> {
                PortraitScheduleList(
                    state = state,
                    listState = listState,
                    collapseFraction = collapseFraction,
                    onHeaderMeasured = { headerHeightPx = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PortraitTopBar(
    title: String,
    titleAlpha: Float,
    onOpenMenu: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuButton(onClick = onOpenMenu)
                Text(
                    title,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            val visible = titleAlpha.coerceIn(0f, 1f)
                            alpha = visible
                            translationY = (1f - visible) * 8.dp.toPx()
                            val scale = 0.92f + 0.08f * visible
                            scaleX = scale
                            scaleY = scale
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun PortraitGroupHeader(
    state: ScheduleState,
    collapseFraction: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val group = state.selectedGroup

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = group?.title ?: "Выберите группу",
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically { it / 8 }) togetherWith
                    fadeOut(tween(120))
            },
            label = "portraitGroupTitle",
        ) { title ->
            Text(
                title,
                modifier = Modifier.graphicsLayer {
                    val scale = 1f - 0.10f * collapseFraction
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            group?.faculty?.takeIf { it.isNotBlank() }?.let {
                HeaderPill(it)
            }
            group?.course?.let {
                HeaderPill("$it курс")
            }
            if (state.lessons.isNotEmpty()) {
                HeaderPill(
                    lessonCountText(state.visibleLessons.size),
                    tonal = true,
                )
            }
        }
    }
}

@Composable
private fun PortraitScheduleList(
    state: ScheduleState,
    listState: LazyListState,
    collapseFraction: Float,
    onHeaderMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = state.availableWeekdays
    val selectedDay = state.selectedDay.takeIf { it in days } ?: days.first()
    val dateLabels = days.associateWith { day ->
        state.dateForWeekday(day)?.let(::formatShortDate)
    }
    val error = state.error
    val horizontalPadding = 20.dp
    var previewDay by remember { mutableStateOf(selectedDay) }

    LaunchedEffect(selectedDay) {
        previewDay = selectedDay
    }

    LaunchedEffect(days) {
        if (state.selectedDay !in days) {
            state.selectDay(days.first())
        }
    }

    LaunchedEffect(state.selectedGroup?.id, state.selectedWeek, selectedDay) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "portrait-group-header") {
            PortraitGroupHeader(
                state = state,
                collapseFraction = collapseFraction,
                modifier = Modifier.onGloballyPositioned {
                    onHeaderMeasured(it.size.height)
                },
            )
        }
        if (state.isOffline) {
            item(key = "portrait-offline") {
                OfflineBanner(
                    onRefresh = { state.loadSchedule(forceRefresh = true) },
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = 8.dp,
                    ),
                )
            }
        }
        if (error != null && state.lessons.isNotEmpty() && !state.isOffline) {
            item(key = "portrait-error") {
                ErrorBanner(
                    message = error,
                    onRetry = state::retry,
                    onDismiss = state::clearError,
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = 8.dp,
                    ),
                )
            }
        }
        stickyHeader(key = "portrait-day-picker") {
            DayPicker(
                days = days,
                selectedDay = previewDay.takeIf { it in days } ?: selectedDay,
                dateLabels = dateLabels,
                today = state.todayWeekday,
                onSelect = { day ->
                    previewDay = day
                    state.selectDay(day)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = 8.dp,
                    ),
            )
        }
        item(key = "portrait-day-content") {
            PortraitDayPager(
                state = state,
                days = days,
                selectedDay = selectedDay,
                horizontalPadding = horizontalPadding,
                onDayPreview = { previewDay = it },
                onDaySettled = state::selectDay,
            )
        }
    }
}

@Composable
private fun PortraitDayPager(
    state: ScheduleState,
    days: List<Int>,
    selectedDay: Int,
    horizontalPadding: Dp,
    onDayPreview: (Int) -> Unit,
    onDaySettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeDay by remember { mutableStateOf(selectedDay) }
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val previewDayCallback by rememberUpdatedState(onDayPreview)
    val settledDayCallback by rememberUpdatedState(onDaySettled)

    LaunchedEffect(selectedDay) {
        if (!isDragging) {
            settleJob?.cancel()
            activeDay = selectedDay
            dragOffset = 0f
        }
    }

    LaunchedEffect(days) {
        if (activeDay !in days) {
            settleJob?.cancel()
            days.firstOrNull()?.let {
                activeDay = it
                dragOffset = 0f
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val pageWidth = constraints.maxWidth.toFloat()
        val currentIndex = days.indexOf(activeDay).takeIf { it >= 0 } ?: 0
        val currentDay = days.getOrNull(currentIndex) ?: activeDay
        val previousDay = days.getOrNull(currentIndex - 1)
        val nextDay = days.getOrNull(currentIndex + 1)

        Layout(
            content = {
                Box(
                    Modifier.graphicsLayer {
                        translationX = dragOffset
                    },
                ) {
                    if (previousDay != null) {
                        PortraitDayPage(
                            state = state,
                            day = previousDay,
                            horizontalPadding = horizontalPadding,
                        )
                    } else {
                        Spacer(Modifier)
                    }
                }
                Box(
                    Modifier.graphicsLayer {
                        translationX = dragOffset
                    },
                ) {
                    PortraitDayPage(
                        state = state,
                        day = currentDay,
                        horizontalPadding = horizontalPadding,
                    )
                }
                Box(
                    Modifier.graphicsLayer {
                        translationX = dragOffset
                    },
                ) {
                    if (nextDay != null) {
                        PortraitDayPage(
                            state = state,
                            day = nextDay,
                            horizontalPadding = horizontalPadding,
                        )
                    } else {
                        Spacer(Modifier)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(days, pageWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            settleJob?.cancel()
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val index = days.indexOf(activeDay)
                            if (index >= 0) {
                                val startDay = activeDay
                                var newIndex = index
                                var nextOffset = dragOffset + dragAmount

                                while (
                                    nextOffset < -pageWidth / 2f &&
                                    newIndex < days.lastIndex
                                ) {
                                    newIndex += 1
                                    nextOffset += pageWidth
                                }
                                while (
                                    nextOffset > pageWidth / 2f &&
                                    newIndex > 0
                                ) {
                                    newIndex -= 1
                                    nextOffset -= pageWidth
                                }

                                if (newIndex == 0) {
                                    nextOffset = nextOffset.coerceAtMost(pageWidth * 0.28f)
                                }
                                if (newIndex == days.lastIndex) {
                                    nextOffset = nextOffset.coerceAtLeast(-pageWidth * 0.28f)
                                }

                                val newDay = days[newIndex]
                                if (newDay != startDay) {
                                    activeDay = newDay
                                    previewDayCallback(newDay)
                                }
                                dragOffset = nextOffset
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            settleJob = scope.launch {
                                animate(
                                    initialValue = dragOffset,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 1f,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) { value, _ ->
                                    dragOffset = value
                                }
                                settledDayCallback(activeDay)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            settleJob = scope.launch {
                                animate(
                                    initialValue = dragOffset,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 1f,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) { value, _ ->
                                    dragOffset = value
                                }
                                settledDayCallback(activeDay)
                            }
                        },
                    )
                },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val currentPlaceable = measurables[1].measure(
                constraints.copy(minWidth = width, maxWidth = width),
            )
            val height = currentPlaceable.height
            val adjacentConstraints = Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = 0,
                maxHeight = height,
            )
            val previousPlaceable = measurables[0].measure(adjacentConstraints)
            val nextPlaceable = measurables[2].measure(adjacentConstraints)

            layout(width, height) {
                previousPlaceable.place(
                    x = -width,
                    y = 0,
                )
                currentPlaceable.place(
                    x = 0,
                    y = 0,
                )
                nextPlaceable.place(
                    x = width,
                    y = 0,
                )
            }
        }
    }
}

@Composable
private fun PortraitDayPage(
    state: ScheduleState,
    day: Int,
    horizontalPadding: Dp,
) {
    val lessons = state.lessonsByDay[day].orEmpty()
    val dateLabel = state.dateForWeekday(day)?.let(::formatFullDate)

    if (lessons.isEmpty()) {
        EmptyState(
            title = "В этот день занятий нет",
            message = if (state.lessons.isEmpty()) {
                "Для этой группы пока нет занятий. Попробуйте обновить данные."
            } else {
                "Выберите другой день недели или другую учебную неделю."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = horizontalPadding),
            actionLabel = if (state.lessons.isEmpty()) "Обновить" else null,
            onAction = if (state.lessons.isEmpty()) {
                { state.loadSchedule(forceRefresh = true) }
            } else {
                null
            },
        )
    } else {
        DaySection(
            day = day,
            lessons = lessons,
            compact = true,
            dateLabel = dateLabel,
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = 4.dp,
            ),
        )
    }
}

@Composable
private fun DayPicker(
    days: List<Int>,
    selectedDay: Int,
    dateLabels: Map<Int, String?>,
    today: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "ДЕНЬ НЕДЕЛИ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(days, key = { it }) { day ->
                DayChip(
                    day = day,
                    selected = day == selectedDay,
                    dateLabel = dateLabels[day],
                    isToday = day == today,
                    onClick = { onSelect(day) },
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    day: Int,
    selected: Boolean,
    dateLabel: String?,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = weekdayAccent(day, scheme)
    val container by animateColorAsState(
        targetValue = if (selected) accent else scheme.surfaceContainerHigh,
        animationSpec = AppMotion.effects(),
        label = "dayChipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else scheme.onSurfaceVariant,
        animationSpec = AppMotion.effects(),
        label = "dayChipContent",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        border = when {
            selected -> null
            isToday -> BorderStroke(1.5.dp, accent)
            else -> BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
        },
        modifier = Modifier.height(48.dp),
    ) {
        Box(
            Modifier
                .widthIn(min = 40.dp)
                .padding(horizontal = 3.dp, vertical = 5.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    weekdayShort(day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    dateLabel ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (isToday) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(if (selected) Color.White else accent, CircleShape),
                )
            }
        }
    }
}
