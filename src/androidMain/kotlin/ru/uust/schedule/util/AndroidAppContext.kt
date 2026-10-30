package ru.uust.schedule.util

import android.content.Context

/**
 * Process-wide application context. It is initialised by [ru.uust.schedule.UustApplication]
 * before any activity or repository is created.
 */
internal object AndroidAppContext {
    lateinit var context: Context
}
