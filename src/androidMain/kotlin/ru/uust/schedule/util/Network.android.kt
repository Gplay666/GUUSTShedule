package ru.uust.schedule.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun isNetworkAvailable(): Boolean = withContext(Dispatchers.IO) {
    try {
        val connectivityManager = AndroidAppContext.context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return@withContext false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return@withContext false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Throwable) {
        false
    }
}
