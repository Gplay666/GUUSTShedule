package ru.uust.schedule.util

/**
 * Lightweight device-level connectivity check. It only reports whether an
 * active network interface exists; the actual API request remains the source
 * of truth for whether the service is reachable.
 */
expect suspend fun isNetworkAvailable(): Boolean
