package ru.uust.schedule

import android.app.Application
import ru.uust.schedule.util.AndroidAppContext

class UustApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.context = applicationContext
    }
}
