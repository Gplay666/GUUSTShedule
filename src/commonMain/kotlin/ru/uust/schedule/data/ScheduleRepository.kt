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
    private val scheduleMutex = Mutex()

    private var groupsCache: List<Group>? = null
    private val scheduleCache = mutableMapOf<Int, List<Lesson>>()

    private var selectedGroupIdCache: Int? = null
    private var selectedGroupIdLoaded = false

    suspend fun cachedGroups(): List<Group>? {
        groupsCache?.let { return it }
        return withContext(ioDispatcher) { cache.loadGroups() }?.also { groupsCache = it }
    }

    suspend fun cachedSchedule(groupId: Int): List<Lesson>? {
        scheduleCache[groupId]?.let { return it }
        return withContext(ioDispatcher) { cache.loadSchedule(groupId) }
            ?.also { scheduleCache[groupId] = it }
    }

    suspend fun cachedSelectedGroupId(): Int? {
        if (selectedGroupIdLoaded) return selectedGroupIdCache
        val loaded = withContext(ioDispatcher) { cache.loadSelectedGroupId() }
        selectedGroupIdCache = loaded
        selectedGroupIdLoaded = true
        return loaded
    }

    fun rememberSelectedGroup(groupId: Int?) {
        selectedGroupIdCache = groupId
        selectedGroupIdLoaded = true
        // The selection file is tiny; keeping this synchronous avoids making
        // every caller launch a separate coroutine for a few bytes.
        cache.saveSelectedGroupId(groupId)
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

    suspend fun loadSchedule(
        groupId: Int,
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Lesson>> {
        scheduleMutex.lock()
        try {
            if (!forceRefresh) {
                scheduleCache[groupId]?.let { return LoadResult(it, fromCache = false) }
            }

            val cached = scheduleCache[groupId]
                ?: withContext(ioDispatcher) { cache.loadSchedule(groupId) }
                    ?.also { scheduleCache[groupId] = it }

            if (!allowNetwork) {
                if (cached != null) {
                    return LoadResult(cached, fromCache = true)
                }
                throw OfflineCacheMissException("Расписание этой группы не сохранено")
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
                        .get("$API_BASE/schedule/0/$groupId/semester/$SEMESTER") {
                            parameter("site", "schedule")
                        }
                        .body<List<LessonDto>>()
                        .mapIndexedNotNull { index, dto -> dto.toLesson(index) }
                        .map { lesson -> lesson to lesson.subject.lowercase() }
                        .sortedWith(
                            compareBy<Pair<Lesson, String>> { it.first.weekday }
                                .thenBy { it.first.pair ?: 99 }
                                .thenBy { it.second },
                        )
                        .map { it.first }
                    cache.saveSchedule(groupId, loaded)
                    loaded
                }

                scheduleCache[groupId] = lessons
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
}
