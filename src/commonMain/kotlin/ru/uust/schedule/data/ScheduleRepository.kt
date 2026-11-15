package ru.uust.schedule.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import ru.uust.schedule.util.isNetworkAvailable

data class LoadResult<T>(
    val data: T,
    val fromCache: Boolean,
    val networkFailed: Boolean = false,
)

class OfflineCacheMissException(message: String) : Exception(message)

class ScheduleRepository(
    private val client: HttpClient = createHttpClient(),
    private val cache: ScheduleCache = ScheduleCache(),
    private val networkAvailable: suspend () -> Boolean = { isNetworkAvailable() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val groupsMutex = Mutex()
    private val teachersMutex = Mutex()
    private val scheduleMutex = Mutex()

    private var groupsCache: List<Group>? = null
    private var teachersCache: List<Teacher>? = null
    private val scheduleCache = mutableMapOf<SelectedTarget, List<Lesson>>()

    private var selectedTargetCache: SelectedTarget? = null
    private var selectedTargetLoaded = false

    suspend fun cachedGroups(): List<Group>? {
        groupsCache?.let { return it }
        return withContext(ioDispatcher) { cache.loadGroups() }?.also { groupsCache = it }
    }

    suspend fun cachedTeachers(): List<Teacher>? {
        teachersCache?.let { return it }
        return withContext(ioDispatcher) { cache.loadTeachers() }?.also { teachersCache = it }
    }

    suspend fun cachedSchedule(target: SelectedTarget): List<Lesson>? {
        scheduleCache[target]?.let { return it }
        return withContext(ioDispatcher) { cache.loadSchedule(target) }
            ?.also { scheduleCache[target] = it }
    }

    suspend fun cachedSelectedTarget(): SelectedTarget? {
        if (selectedTargetLoaded) return selectedTargetCache
        val loaded = withContext(ioDispatcher) { cache.loadSelectedTarget() }
        selectedTargetCache = loaded
        selectedTargetLoaded = true
        return loaded
    }

    fun rememberSelectedTarget(target: SelectedTarget?) {
        selectedTargetCache = target
        selectedTargetLoaded = true
        // The selection file is tiny; keeping this synchronous avoids making
        // every caller launch a separate coroutine for a few bytes.
        cache.saveSelectedTarget(target)
    }

    suspend fun loadGroups(
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Group>> {
        groupsMutex.lock()
        try {
            if (!forceRefresh) {
                groupsCache?.let { return LoadResult(it, fromCache = false) }
            }

            val cached = groupsCache
                ?: withContext(ioDispatcher) { cache.loadGroups() }?.also { groupsCache = it }

            if (!allowNetwork) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true)
                }
                throw OfflineCacheMissException("Нет сохранённого списка групп")
            }

            if (!networkAvailable()) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw OfflineCacheMissException("Нет подключения к интернету")
            }

            try {
                val groups = withContext(ioDispatcher) {
                    val loaded = client
                        .get("$API_BASE/groups") {
                            parameter("site", "schedule")
                        }
                        .body<List<GroupDto>>()
                        .map { it.toGroup() }
                        .map { group -> group.title.lowercase() to group }
                        .sortedWith(
                            compareBy<Pair<String, Group>> { it.first }
                                .thenBy { it.second.id },
                        )
                        .map { it.second }
                    cache.saveGroups(loaded)
                    loaded
                }

                groupsCache = groups
                return LoadResult(groups, fromCache = false)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw throwable
            }
        } finally {
            groupsMutex.unlock()
        }
    }

    suspend fun loadTeachers(
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Teacher>> {
        teachersMutex.lock()
        try {
            if (!forceRefresh) {
                teachersCache?.let { return LoadResult(it, fromCache = false) }
            }

            val cached = teachersCache
                ?: withContext(ioDispatcher) { cache.loadTeachers() }?.also { teachersCache = it }

            if (!allowNetwork) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true)
                }
                throw OfflineCacheMissException("Нет сохранённого списка преподавателей")
            }

            if (!networkAvailable()) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw OfflineCacheMissException("Нет подключения к интернету")
            }

            try {
                val teachers = withContext(ioDispatcher) {
                    val loaded = client
                        .get("$API_BASE/teachers") {
                            parameter("site", "schedule")
                        }
                        .body<Map<String, TeacherDto>>()
                        .values
                        .map { it.toTeacher() }
                        .sortedWith(
                            compareBy<Teacher> { it.title.lowercase() }
                                .thenBy { it.id },
                        )
                    cache.saveTeachers(loaded)
                    loaded
                }

                teachersCache = teachers
                return LoadResult(teachers, fromCache = false)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw throwable
            }
        } finally {
            teachersMutex.unlock()
        }
    }

    suspend fun loadSchedule(
        target: SelectedTarget,
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Lesson>> {
        scheduleMutex.lock()
        try {
            if (!forceRefresh) {
                scheduleCache[target]?.let { return LoadResult(it, fromCache = false) }
            }

            val cached = scheduleCache[target]
                ?: withContext(ioDispatcher) { cache.loadSchedule(target) }
                    ?.also { scheduleCache[target] = it }

            if (!allowNetwork) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true)
                }
                throw OfflineCacheMissException("Расписание ${target.ownerGenitive()} не сохранено")
            }

            if (!networkAvailable()) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw OfflineCacheMissException("Нет подключения к интернету")
            }

            try {
                val lessons = withContext(ioDispatcher) {
                    val loaded = client
                        .get(
                            "$API_BASE/schedule/${target.kind.scheduleType}/${target.id}" +
                                "/semester/$SEMESTER",
                        ) {
                            parameter("site", "schedule")
                        }
                        .body<List<LessonDto>>()
                        .mapIndexedNotNull { index, dto ->
                            dto.toLesson(
                                sequence = index,
                                includeGroup = target.kind == TargetKind.Teacher,
                            )
                        }
                        .map { lesson -> lesson to lesson.subject.lowercase() }
                        .sortedWith(
                            compareBy<Pair<Lesson, String>> { it.first.weekday }
                                .thenBy { it.first.pair ?: 99 }
                                .thenBy { it.second },
                        )
                        .map { it.first }
                    cache.saveSchedule(target, loaded)
                    loaded
                }

                scheduleCache[target] = lessons
                return LoadResult(lessons, fromCache = false)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (cached != null) {
                    return LoadResult(cached, fromCache = true, networkFailed = true)
                }
                throw throwable
            }
        } finally {
            scheduleMutex.unlock()
        }
    }

    private fun SelectedTarget.ownerGenitive(): String = when (kind) {
        TargetKind.Group -> "этой группы"
        TargetKind.Teacher -> "этого преподавателя"
    }
}
