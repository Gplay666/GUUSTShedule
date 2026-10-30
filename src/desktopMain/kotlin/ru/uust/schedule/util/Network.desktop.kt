package ru.uust.schedule.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.NetworkInterface

actual suspend fun isNetworkAvailable(): Boolean = withContext(Dispatchers.IO) {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@withContext false
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) continue

            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address.address.isNotEmpty()) {
                    return@withContext true
                }
            }
        }
        false
    } catch (_: Throwable) {
        false
    }
}
