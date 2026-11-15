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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import ru.uust.schedule.data.DEFAULT_GROUP_ID
import ru.uust.schedule.data.Group
import ru.uust.schedule.data.GroupTarget
import ru.uust.schedule.data.Lesson
import ru.uust.schedule.data.OfflineCacheMissException
import ru.uust.schedule.data.ScheduleRepository
import ru.uust.schedule.data.ScheduleTarget
import ru.uust.schedule.data.SelectedTarget
import ru.uust.schedule.data.TargetKind
import ru.uust.schedule.data.Teacher
import ru.uust.schedule.data.TeacherTarget
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

    var teachers by mutableStateOf<List<Teacher>>(emptyList())
        private set

    var query by mutableStateOf("")
        private set

    var selectedTarget by mutableStateOf<ScheduleTarget?>(null)
        private set

    var lessons by mutableStateOf<List<Lesson>>(emptyList())
        private set

    var selectedWeek by mutableStateOf<Int?>(null)
        private set

    val todayWeekday: Int = currentWeekday()
    val academicWeek: Int = currentAcademicWeek()

    var selectedDay by mutableStateOf(if (todayWeekday in 1..6) todayWeekday else 1)
        private set

    var isLoadingCatalog by mutableStateOf(true)
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

    private data class CatalogSearchEntry(
        val target: ScheduleTarget,
        val titleLower: String,
        val subtitleLower: String,
        val idText: String,
    )

    init {
        refreshSystemTheme()
    }

    /**
     * Both catalogs share one index so the search field returns groups and
     * teachers together, as requested. Lower-casing happens only when either
     * catalog changes, not on every keystroke.
     */
    private val catalogSearchIndex by derivedStateOf {
        buildList(groups.size + teachers.size) {
            groups.forEach { group ->
                add(
                    CatalogSearchEntry(
                        target = GroupTarget(group),
                        titleLower = group.title.lowercase(),
                        subtitleLower = group.subtitle.lowercase(),
                        idText = group.id.toString(),
                    ),
                )
            }
            teachers.forEach { teacher ->
                add(
                    CatalogSearchEntry(
                        target = TeacherTarget(teacher),
                        titleLower = teacher.title.lowercase(),
                        subtitleLower = teacher.department.lowercase(),
                        idText = teacher.id.toString(),
                    ),
                )
            }
        }
    }

    val filteredTargets: List<ScheduleTarget> by derivedStateOf {
        val text = query.trim()
        if (text.isEmpty()) {
            emptyList()
        } else {
            val needle = text.lowercase()
            catalogSearchIndex.asSequence()
                .mapNotNull { entry ->
                    entry.matchScore(needle, text)?.let { score -> entry to score }
                }
                .sortedWith(
                    compareBy<Pair<CatalogSearchEntry, Int>> { it.second }
                        .thenBy { it.first.target.title.length }
                        .thenBy { it.first.titleLower },
                )
                .take(24)
                .map { it.first.target }
                .toList()
        }
    }

    private fun CatalogSearchEntry.matchScore(needle: String, rawQuery: String): Int? = when {
        titleLower == needle -> 0
        titleLower.startsWith(needle) -> 1
        titleLower.contains(" $needle") -> 2
        titleLower.contains(needle) -> 3
        subtitleLower.contains(needle) -> 4
        idText.contains(rawQuery) -> 5
        else -> null
    }

    fun isSelected(target: ScheduleTarget): Boolean {
        val current = selectedTarget ?: return false
        return current.kind == target.kind && current.id == target.id
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
        if (groups.isNotEmpty() && teachers.isNotEmpty() && selectedTarget != null) {
            loadSchedule()
            return
        }

        isInitializing = true
        isLoadingCatalog = true
        error = null
        isOffline = false

        try {
            // Show the last successful local copy immediately, then refresh both
            // catalogs in parallel. One failing catalog must not break the other.
            applyCachedSnapshot()
            isLoadingCatalog = groups.isEmpty() && teachers.isEmpty()

            val (groupsResult, teachersResult) = coroutineScope {
                val groupsDeferred = async {
                    loadCatching { repository.loadGroups(forceRefresh = true) }
                }
                val teachersDeferred = async {
                    loadCatching { repository.loadTeachers(forceRefresh = true) }
                }
                groupsDeferred.await() to teachersDeferred.await()
            }

            groupsResult.getOrNull()?.let { result -> groups = result.data }
            teachersResult.getOrNull()?.let { result -> teachers = result.data }

            val anyFailure = groupsResult.isFailure ||
                teachersResult.isFailure ||
                groupsResult.getOrNull()?.networkFailed == true ||
                teachersResult.getOrNull()?.networkFailed == true
            if (anyFailure) {
                isOffline = true
            }

            val previousTarget = selectedTarget?.selection
            val preferred = resolveTarget(previousTarget ?: repository.cachedSelectedTarget())
            if (preferred?.selection != previousTarget) {
                lessons = emptyList()
                selectedWeek = null
                weekSelectionInitialized = false
            }
            selectedTarget = preferred
            query = preferred?.title.orEmpty()
            if (preferred != null) {
                repository.rememberSelectedTarget(preferred.selection)
            }

            if (groups.isEmpty() && teachers.isEmpty()) {
                error = "Нет подключения к интернету и нет сохранённого списка групп " +
                    "и преподавателей. Подключитесь и нажмите «Обновить»."
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            println("UUST: failed to load catalogs: ${throwable.message}")
            isOffline = true
            if (groups.isEmpty() && teachers.isEmpty()) {
                error = "Нет подключения к интернету и нет сохранённого списка групп " +
                    "и преподавателей. Подключитесь и нажмите «Обновить»."
            }
        } finally {
            isLoadingCatalog = false
            isInitializing = false
        }

        if (selectedTarget != null) {
            loadSchedule(forceRefresh = true)
        }
    }

    private suspend fun <T> loadCatching(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Result.failure(throwable)
    }

    private suspend fun applyCachedSnapshot() {
        val cachedGroups = repository.cachedGroups()
        val cachedTeachers = repository.cachedTeachers()
        if (cachedGroups == null && cachedTeachers == null) return

        if (cachedGroups != null) groups = cachedGroups
        if (cachedTeachers != null) teachers = cachedTeachers

        val preferred = resolveTarget(repository.cachedSelectedTarget())
        selectedTarget = preferred
        query = preferred?.title.orEmpty()
        if (preferred != null) {
            repository.cachedSchedule(preferred.selection)?.let(::applyLessons)
        }
    }

    private fun resolveTarget(selection: SelectedTarget?): ScheduleTarget? {
        if (selection != null) {
            val found = when (selection.kind) {
                TargetKind.Group -> groups
                    .firstOrNull { it.id == selection.id }
                    ?.let(::GroupTarget)
                TargetKind.Teacher -> teachers
                    .firstOrNull { it.id == selection.id }
                    ?.let(::TeacherTarget)
            }
            if (found != null) return found
        }

        return groups.firstOrNull { it.id == DEFAULT_GROUP_ID }?.let(::GroupTarget)
            ?: groups.firstOrNull()?.let(::GroupTarget)
            ?: teachers.firstOrNull()?.let(::TeacherTarget)
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

    fun selectTarget(target: ScheduleTarget) {
        query = target.title
        if (isSelected(target)) return

        selectedTarget = target
        lessons = emptyList()
        selectedWeek = null
        weekSelectionInitialized = false
        selectedDay = if (todayWeekday in 1..6) todayWeekday else 1
        repository.rememberSelectedTarget(target.selection)
        loadSchedule()
    }

    fun loadSchedule(
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ) {
        val target = selectedTarget ?: return
        val requestId = ++scheduleRequest

        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            isLoadingSchedule = true
            if (forceRefresh || lessons.isEmpty()) {
                error = null
            }

            try {
                val result = repository.loadSchedule(
                    target = target.selection,
                    forceRefresh = forceRefresh,
                    allowNetwork = allowNetwork,
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
                        "Нет подключения к интернету, а расписание ${target.ownerPhrase()} " +
                            "не сохранено. Подключитесь и нажмите «Обновить»."
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

    private fun ScheduleTarget.ownerPhrase(): String = when (this) {
        is GroupTarget -> "этой группы"
        is TeacherTarget -> "этого преподавателя"
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
        if (groups.isEmpty() || teachers.isEmpty()) {
            scope.launch { initialize() }
        } else {
            loadSchedule(forceRefresh = true)
        }
    }
}
