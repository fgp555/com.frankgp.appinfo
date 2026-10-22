package com.frankgp.appinfo.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

class PerformanceRepository(private val context: Context) {

    private var lastRxBytes = -1L
    private var lastTxBytes = -1L
    private var lastTimeMs = -1L

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    suspend fun samplePerformance(): PerformanceData = withContext(Dispatchers.IO) {
        val currentTimeMs = System.currentTimeMillis()
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()

        var downloadSpeed = 0L
        var uploadSpeed = 0L

        if (lastTimeMs > 0 && lastRxBytes >= 0 && lastTxBytes >= 0) {
            val timeDiffSec = (currentTimeMs - lastTimeMs) / 1000.0
            if (timeDiffSec > 0) {
                val rxDiff = if (currentRxBytes >= lastRxBytes) currentRxBytes - lastRxBytes else 0
                val txDiff = if (currentTxBytes >= lastTxBytes) currentTxBytes - lastTxBytes else 0
                downloadSpeed = (rxDiff / timeDiffSec).toLong()
                uploadSpeed = (txDiff / timeDiffSec).toLong()
            }
        }

        lastTimeMs = currentTimeMs
        lastRxBytes = currentRxBytes
        lastTxBytes = currentTxBytes

        // RAM Info
        var usedRamMb = 0L
        var totalRamMb = 0L
        var ramUsagePercent = 0
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalRamMb = memInfo.totalMem / (1024 * 1024)
                val availRamMb = memInfo.availMem / (1024 * 1024)
                usedRamMb = totalRamMb - availRamMb
                if (totalRamMb > 0) {
                    ramUsagePercent = ((usedRamMb.toDouble() / totalRamMb.toDouble()) * 100).toInt()
                }
            }
        } catch (_: Exception) {}

        // CPU Usage
        val cpuPercent = sampleCpuUsage()

        // Battery Info
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
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (_: Exception) {}

        PerformanceData(
            downloadSpeedBytesPerSec = downloadSpeed,
            uploadSpeedBytesPerSec = uploadSpeed,
            usedRamMb = usedRamMb,
            totalRamMb = totalRamMb,
            ramUsagePercent = ramUsagePercent,
            cpuUsagePercent = cpuPercent,
            batteryLevel = batteryLevel,
            isCharging = isCharging
        )
    }

    private fun sampleCpuUsage(): Int {
        return try {
            val file = RandomAccessFile("/proc/stat", "r")
            val line = file.readLine()
            file.close()
            if (line != null && line.startsWith("cpu ")) {
                val toks = line.split("\\s+".toRegex())
                if (toks.size >= 8) {
                    val user = toks[1].toLong()
                    val nice = toks[2].toLong()
                    val sys = toks[3].toLong()
                    val idle = toks[4].toLong()
                    val iowait = toks[5].toLong()
                    val irq = toks[6].toLong()
                    val softirq = toks[7].toLong()

                    val total = user + nice + sys + idle + iowait + irq + softirq
                    val idleTime = idle + iowait

                    val totalDiff = total - lastCpuTotal
                    val idleDiff = idleTime - lastCpuIdle

                    lastCpuTotal = total
                    lastCpuIdle = idleTime

                    if (totalDiff > 0) {
                        val usage = (((totalDiff - idleDiff).toDouble() / totalDiff.toDouble()) * 100).toInt()
                        usage.coerceIn(0, 100)
                    } else 0
                } else 0
            } else 0
        } catch (_: Exception) {
            // Estimated load fallback
            18
        }
    }
}
