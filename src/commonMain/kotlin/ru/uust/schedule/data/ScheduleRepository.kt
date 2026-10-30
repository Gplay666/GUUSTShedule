package ru.uust.schedule.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
) {
    private val groupsMutex = Mutex()
    private val scheduleMutex = Mutex()

    private var groupsCache: List<Group>? = null
    private val scheduleCache = mutableMapOf<Int, List<Lesson>>()

    fun cachedGroups(): List<Group>? =
        groupsCache ?: cache.loadGroups()?.also { groupsCache = it }

    fun cachedSchedule(groupId: Int): List<Lesson>? =
        scheduleCache[groupId] ?: cache.loadSchedule(groupId)?.also { scheduleCache[groupId] = it }

    fun cachedSelectedGroupId(): Int? = cache.loadSelectedGroupId()

    fun rememberSelectedGroup(groupId: Int?) {
        cache.saveSelectedGroupId(groupId)
    }

    suspend fun loadGroups(
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Group>> = groupsMutex.withLock {
        if (!forceRefresh) {
            groupsCache?.let { return@withLock LoadResult(it, fromCache = false) }
        }

        val cached = groupsCache ?: cache.loadGroups()?.also { groupsCache = it }

        if (!allowNetwork) {
            if (cached != null) {
                return@withLock LoadResult(cached, fromCache = true)
            }
            throw OfflineCacheMissException("Нет сохранённого списка групп")
        }

        if (!networkAvailable()) {
            if (cached != null) {
                return@withLock LoadResult(cached, fromCache = true, networkFailed = true)
            }
            throw OfflineCacheMissException("Нет подключения к интернету")
        }

        try {
            val groups = client
                .get("$API_BASE/groups") {
                    parameter("site", "schedule")
                }
                .body<List<GroupDto>>()
                .map { it.toGroup() }
                .sortedWith(compareBy<Group> { it.title.lowercase() }.thenBy { it.id })

            groupsCache = groups
            cache.saveGroups(groups)
            LoadResult(groups, fromCache = false)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (cached != null) {
                LoadResult(cached, fromCache = true, networkFailed = true)
            } else {
                throw throwable
            }
        }
    }

    suspend fun loadSchedule(
        groupId: Int,
        forceRefresh: Boolean = false,
        allowNetwork: Boolean = true,
    ): LoadResult<List<Lesson>> = scheduleMutex.withLock {
        if (!forceRefresh) {
            scheduleCache[groupId]?.let { return@withLock LoadResult(it, fromCache = false) }
        }

        val cached = scheduleCache[groupId]
            ?: cache.loadSchedule(groupId)?.also { scheduleCache[groupId] = it }

        if (!allowNetwork) {
            if (cached != null) {
                return@withLock LoadResult(cached, fromCache = true)
            }
            throw OfflineCacheMissException("Расписание этой группы не сохранено")
        }

        if (!networkAvailable()) {
            if (cached != null) {
                return@withLock LoadResult(cached, fromCache = true, networkFailed = true)
            }
            throw OfflineCacheMissException("Нет подключения к интернету")
        }

        try {
            val lessons = client
                .get("$API_BASE/schedule/0/$groupId/semester/$SEMESTER") {
                    parameter("site", "schedule")
                }
                .body<List<LessonDto>>()
                .mapIndexedNotNull { index, dto -> dto.toLesson(index) }
                .sortedWith(
                    compareBy<Lesson> { it.weekday }
                        .thenBy { it.pair ?: 99 }
                        .thenBy { it.subject.lowercase() }
                )

            scheduleCache[groupId] = lessons
            cache.saveSchedule(groupId, lessons)
            LoadResult(lessons, fromCache = false)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (cached != null) {
                LoadResult(cached, fromCache = true, networkFailed = true)
            } else {
                throw throwable
            }
        }
    }
}
