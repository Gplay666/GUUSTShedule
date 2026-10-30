package ru.uust.schedule.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val API_BASE = "https://dev.uust-time.ru/api/v/852972"
const val SEMESTER = "241"
const val DEFAULT_GROUP_ID = 14381

expect fun createHttpClient(): HttpClient

fun HttpClientConfig<*>.configureScheduleClient() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 12_000
        socketTimeoutMillis = 30_000
    }
    defaultRequest {
        header(HttpHeaders.UserAgent, "UUSTSchedule/0.1 (Kotlin Multiplatform)")
        header(HttpHeaders.Accept, "application/json")
    }
}
