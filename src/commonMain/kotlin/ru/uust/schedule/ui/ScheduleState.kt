package ru.uust.schedule.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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

    private data class GroupSearchEntry(
        val group: Group,
        val titleLower: String,
        val facultyLower: String,
        val idText: String,
    )

    init {
        refreshSystemTheme()
    }

    /**
     * Lower-casing is done once when the group list changes. The previous
     * implementation lower-cased every title again for every sort comparison.
     */
    private val groupSearchIndex by derivedStateOf {
        groups.map { group ->
            GroupSearchEntry(
                group = group,
                titleLower = group.title.lowercase(),
                facultyLower = group.faculty.lowercase(),
                idText = group.id.toString(),
            )
        }
    }

    val filteredGroups: List<Group> by derivedStateOf {
        val text = query.trim()
        if (text.isEmpty()) {
            emptyList()
        } else {
            val needle = text.lowercase()
            groupSearchIndex.asSequence()
                .filter { entry ->
                    entry.titleLower.contains(needle) ||
                        entry.facultyLower.contains(needle) ||
                        entry.idText.contains(text)
                }
                .sortedWith(
                    compareBy<GroupSearchEntry> { it.group.title.length }
                        .thenBy { it.titleLower },
                )
                .take(24)
                .map { it.group }
                .toList()
        }
    }

    val weeks: List<Int> by derivedStateOf {
        buildSet {
            lessons.forEach { addAll(it.weeks) }
        }.sorted()
    }

    val currentWeekAvailable: Boolean by derivedStateOf {
        academicWeek in weeks
    }

    fun dateForWeekday(day: Int): LocalDate? =
        selectedWeek?.let { AcademicCalendar.weekdayDate(it, day) }

    val visibleLessons: List<Lesson> by derivedStateOf {
        selectedWeek?.let { week -> lessons.filter { week in it.weeks } } ?: lessons
    }

    val lessonsByDay: Map<Int, List<Lesson>> by derivedStateOf {
        visibleLessons.groupBy { it.weekday }.toSortedMap()
    }

    val lessonsByCell: Map<Pair<Int, Int>, List<Lesson>> by derivedStateOf {
        visibleLessons.groupBy { it.weekday to (it.pair ?: 0) }
    }

    val availableWeekdays: List<Int> by derivedStateOf {
        buildList {
            addAll(1..6)
            if (lessons.any { it.weekday == 7 }) add(7)
        }
    }

    val pairs: List<Int> by derivedStateOf {
        val maxPair = maxOf(6, lessons.maxOfOrNull { it.pair ?: 0 } ?: 0)
        (1..maxPair).toList()
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

        try {
            // Show the last successful local copy immediately, then refresh it
            // only if a network interface is available.
            applyCachedSnapshot()
            isLoadingGroups = groups.isEmpty()

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

    private suspend fun applyCachedSnapshot() {
        val cachedGroups = repository.cachedGroups() ?: return
        groups = cachedGroups

        val preferredId = repository.cachedSelectedGroupId()
        val preferred = cachedGroups.find { it.id == preferredId }
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
