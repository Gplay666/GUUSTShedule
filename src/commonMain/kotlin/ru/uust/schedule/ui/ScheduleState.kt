package ru.uust.schedule.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import ru.uust.schedule.data.DEFAULT_GROUP_ID
import ru.uust.schedule.data.Group
import ru.uust.schedule.data.Lesson
import ru.uust.schedule.data.OfflineCacheMissException
import ru.uust.schedule.data.ScheduleRepository
import ru.uust.schedule.util.AcademicCalendar
import ru.uust.schedule.util.currentAcademicWeek
import ru.uust.schedule.util.currentWeekday
import ru.uust.schedule.util.readSystemTheme

@Stable
class ScheduleState(
    private val repository: ScheduleRepository,
    private val scope: CoroutineScope,
    initialSystemDark: Boolean = false,
) {
    private val themePreferences = ThemePreferences()

    var groups by mutableStateOf<List<Group>>(emptyList())
        private set

    var query by mutableStateOf("")
        private set

    var selectedGroup by mutableStateOf<Group?>(null)
        private set

    var lessons by mutableStateOf<List<Lesson>>(emptyList())
        private set

    var selectedWeek by mutableStateOf<Int?>(null)
        private set

    val todayWeekday: Int = currentWeekday()
    val academicWeek: Int = currentAcademicWeek()

    var selectedDay by mutableStateOf(if (todayWeekday in 1..6) todayWeekday else 1)
        private set

    var isLoadingGroups by mutableStateOf(true)
        private set

    var isLoadingSchedule by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** True after the app detected that the network is unavailable. */
    var isOffline by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(themePreferences.loadThemeMode())
        private set

    var palette by mutableStateOf(themePreferences.loadPalette())
        private set

    var systemDark by mutableStateOf(initialSystemDark)
        private set

    var systemAccent by mutableStateOf<Color?>(null)
        private set

    val isDark: Boolean
        get() = when (themeMode) {
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
            ThemeMode.System -> systemDark
        }

    private var scheduleJob: Job? = null
    private var systemThemeJob: Job? = null
    private var scheduleRequest = 0
    private var weekSelectionInitialized = false
    private var isInitializing = false

    init {
        refreshSystemTheme()
    }

    val filteredGroups: List<Group>
        get() {
            val text = query.trim()
            if (text.isEmpty()) return emptyList()

            return groups.asSequence()
                .filter {
                    it.title.contains(text, ignoreCase = true) ||
                        it.faculty.contains(text, ignoreCase = true) ||
                        it.id.toString().contains(text)
                }
                .sortedWith(compareBy<Group> { it.title.length }.thenBy { it.title.lowercase() })
                .take(24)
                .toList()
        }

    val weeks: List<Int>
        get() = lessons.flatMap { it.weeks }.distinct().sorted()

    val currentWeekAvailable: Boolean
        get() = academicWeek in weeks

    fun dateForWeekday(day: Int): LocalDate? =
        selectedWeek?.let { AcademicCalendar.weekdayDate(it, day) }

    val visibleLessons: List<Lesson>
        get() = selectedWeek?.let { week -> lessons.filter { week in it.weeks } } ?: lessons

    val lessonsByDay: Map<Int, List<Lesson>>
        get() = visibleLessons.groupBy { it.weekday }.toSortedMap()

    val lessonsByCell: Map<Pair<Int, Int>, List<Lesson>>
        get() = visibleLessons.groupBy { it.weekday to (it.pair ?: 0) }

    val availableWeekdays: List<Int>
        get() = buildList {
            addAll(1..6)
            if (lessons.any { it.weekday == 7 }) add(7)
        }

    val pairs: List<Int>
        get() {
            val maxPair = maxOf(6, lessons.mapNotNull { it.pair }.maxOrNull() ?: 6)
            return (1..maxPair).toList()
        }

    suspend fun initialize() {
        if (isInitializing) return
        if (groups.isNotEmpty() && selectedGroup != null) {
            loadSchedule()
            return
        }

        isInitializing = true
        isLoadingGroups = true
        error = null
        isOffline = false

        // Show the last successful local copy immediately, then refresh it only
        // if a network interface is available.
        applyCachedSnapshot()
        isLoadingGroups = groups.isEmpty()

        try {
            val result = repository.loadGroups(
                forceRefresh = true,
                allowNetwork = true,
            )
            val previousGroupId = selectedGroup?.id
            groups = result.data
            if (result.networkFailed) {
                isOffline = true
            }

            val preferredId = repository.cachedSelectedGroupId()
            val preferred = groups.find { it.id == preferredId }
                ?: groups.find { it.id == DEFAULT_GROUP_ID }
                ?: groups.firstOrNull()

            if (preferred?.id != previousGroupId) {
                lessons = emptyList()
                selectedWeek = null
                weekSelectionInitialized = false
            }
            selectedGroup = preferred
            query = preferred?.title.orEmpty()
            if (preferred != null) {
                repository.rememberSelectedGroup(preferred.id)
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            println("UUST: failed to load groups: ${throwable.message}")
            isOffline = true
            if (groups.isEmpty()) {
                error = "Нет подключения к интернету и нет сохранённого списка групп. " +
                    "Подключитесь и нажмите «Обновить»."
            }
        } finally {
            isLoadingGroups = false
            isInitializing = false
        }

        if (selectedGroup != null) {
            loadSchedule(
                forceRefresh = true,
                allowNetwork = !isOffline,
            )
        }
    }

    private fun applyCachedSnapshot() {
        val cachedGroups = repository.cachedGroups() ?: return
        groups = cachedGroups

        val preferred = cachedGroups.find { it.id == repository.cachedSelectedGroupId() }
            ?: cachedGroups.find { it.id == DEFAULT_GROUP_ID }
            ?: cachedGroups.firstOrNull()

        selectedGroup = preferred
        query = preferred?.title.orEmpty()
        if (preferred != null) {
            repository.cachedSchedule(preferred.id)?.let(::applyLessons)
        }
    }

    private fun applyLessons(loaded: List<Lesson>) {
        lessons = loaded
        if (!weekSelectionInitialized && loaded.isNotEmpty()) {
            selectedWeek = academicWeek.takeIf { it in weeks }
            weekSelectionInitialized = true
        } else if (selectedWeek != null && selectedWeek !in weeks) {
            selectedWeek = null
        }
    }

    fun onQueryChange(value: String) {
        query = value
        if (error != null && lessons.isNotEmpty()) {
            error = null
        }
    }

    fun selectGroup(group: Group) {
        query = group.title
        if (selectedGroup?.id == group.id) return

        selectedGroup = group
        lessons = emptyList()
        selectedWeek = null
        weekSelectionInitialized = false
        selectedDay = if (todayWeekday in 1..6) todayWeekday else 1
        repository.rememberSelectedGroup(group.id)
        loadSchedule()
    }

    fun loadSchedule(
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ) {
        val group = selectedGroup ?: return
        val requestId = ++scheduleRequest

        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            isLoadingSchedule = true
            if (forceRefresh || lessons.isEmpty()) {
                error = null
            }

            try {
                val result = repository.loadSchedule(
                    groupId = group.id,
                    forceRefresh = forceRefresh,
                    allowNetwork = allowNetwork && (forceRefresh || !isOffline),
                )
                if (requestId == scheduleRequest) {
                    applyLessons(result.data)
                    when {
                        result.networkFailed -> isOffline = true
                        !result.fromCache -> isOffline = false
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                println("UUST: failed to load schedule: ${throwable.message}")
                if (requestId == scheduleRequest) {
                    isOffline = true
                    error = if (throwable is OfflineCacheMissException) {
                        "Нет подключения к интернету, а расписание этой группы не сохранено. " +
                            "Подключитесь и нажмите «Обновить»."
                    } else {
                        "Не удалось загрузить расписание. Проверьте подключение к интернету."
                    }
                }
            } finally {
                if (requestId == scheduleRequest) {
                    isLoadingSchedule = false
                }
            }
        }
    }

    fun selectWeek(week: Int?) {
        selectedWeek = week
    }

    fun canMoveWeek(delta: Int): Boolean {
        val current = selectedWeek ?: return false
        val index = weeks.indexOf(current)
        if (index < 0) return false
        return (index + delta) in weeks.indices
    }

    fun moveWeek(delta: Int) {
        val current = selectedWeek ?: return
        val available = weeks
        val index = available.indexOf(current)
        if (index < 0) return
        selectedWeek = available[(index + delta).coerceIn(0, available.lastIndex)]
    }

    fun selectCurrentWeek() {
        selectedWeek = academicWeek.takeIf { it in weeks }
    }

    fun selectDay(day: Int) {
        selectedDay = day
    }

    fun toggleTheme() {
        chooseThemeMode(if (isDark) ThemeMode.Light else ThemeMode.Dark)
    }

    fun chooseThemeMode(mode: ThemeMode) {
        themeMode = mode
        themePreferences.saveThemeMode(mode)
        if (mode == ThemeMode.System) {
            refreshSystemTheme()
        }
    }

    fun choosePalette(themePalette: ThemePalette) {
        palette = themePalette
        themePreferences.savePalette(themePalette)
        if (themePalette == ThemePalette.System) {
            refreshSystemTheme()
        }
    }

    fun updateSystemDark(isDark: Boolean) {
        systemDark = isDark
    }

    fun refreshSystemTheme() {
        systemThemeJob?.cancel()
        systemThemeJob = scope.launch {
            val info = withContext(Dispatchers.Default) { readSystemTheme() }
            info.isDark?.let { systemDark = it }
            systemAccent = info.accentColor
        }
    }

    fun clearError() {
        error = null
    }

    fun retry() {
        if (groups.isEmpty()) {
            scope.launch { initialize() }
        } else {
            loadSchedule(forceRefresh = true)
        }
    }
}
