package com.frankgp.appinfo.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceRepository(private val context: Context) {

    suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.IO) {
        val androidVersion = try { getAndroidVersionName(Build.VERSION.SDK_INT) } catch (_: Exception) { "Android ${Build.VERSION.RELEASE}" }
        val apiLevel = Build.VERSION.SDK_INT
        val securityPatch = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"
        } catch (_: Exception) { "N/A" }
        val buildId = try { Build.DISPLAY ?: "" } catch (_: Exception) { "" }

        val manufacturer = try { Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Desconocido" } catch (_: Exception) { "Desconocido" }
        val model = try { Build.MODEL ?: "" } catch (_: Exception) { "" }
        val brand = try { Build.BRAND?.replaceFirstChar { it.uppercase() } ?: "" } catch (_: Exception) { "" }
        val board = try { Build.BOARD ?: "" } catch (_: Exception) { "" }
        val hardware = try { Build.HARDWARE ?: "" } catch (_: Exception) { "" }
        val cpuCores = try { Runtime.getRuntime().availableProcessors() } catch (_: Exception) { 1 }
        val supportedAbis = try { Build.SUPPORTED_ABIS?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }

        // RAM
        var totalRamMb = 0L
        var availRamMb = 0L
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalRamMb = memInfo.totalMem / (1024 * 1024)
                availRamMb = memInfo.availMem / (1024 * 1024)
            }
        } catch (_: Exception) {}

        // Storage
        var totalStorageGb = 0.0
        var availStorageGb = 0.0
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong
            totalStorageGb = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
            availStorageGb = (availBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {}

        // Display
        var widthPx = 0
        var heightPx = 0
        var densityDpi = 0
        var refreshRateHz = 60f
        try {
            val metrics = context.resources.displayMetrics
            widthPx = metrics.widthPixels
            heightPx = metrics.heightPixels
            densityDpi = metrics.densityDpi

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            refreshRateHz = windowManager?.defaultDisplay?.refreshRate ?: 60f
        } catch (_: Exception) {}

        // Battery
        var batteryLevel = 0
        var isCharging = false
        try {
            val batteryIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else 0

                val statusExtra = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING || statusExtra == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (_: Exception) {}

        // Network
        val networkType = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            when {
                caps == null -> "Sin conexión"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Red Móvil"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Conectado"
            }
        } catch (_: Exception) {
            "Desconocido"
        }

        DeviceInfo(
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            buildId = buildId,
            manufacturer = manufacturer,
            model = model,
            brand = brand,
            board = board,
            hardware = hardware,
            cpuCores = cpuCores,
            supportedAbis = supportedAbis,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            totalStorageGb = totalStorageGb,
            availableStorageGb = availStorageGb,
            screenWidthPx = widthPx,
            screenHeightPx = heightPx,
            screenDensityDpi = densityDpi,
            refreshRateHz = refreshRateHz,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            networkType = networkType
        )
    }
}
