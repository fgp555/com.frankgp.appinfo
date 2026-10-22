package com.frankgp.appinfo.data

data class PerformanceData(
    val downloadSpeedBytesPerSec: Long = 0,
    val uploadSpeedBytesPerSec: Long = 0,
    val usedRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val ramUsagePercent: Int = 0,
    val cpuUsagePercent: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false
)

fun formatSpeed(bytesPerSec: Long): String {
    val bitsPerSec = bytesPerSec * 8.0
    return when {
        bitsPerSec >= 1_000_000_000 -> "%.1f Gbps".format(bitsPerSec / 1_000_000_000.0)
        bitsPerSec >= 1_000_000 -> "%.1f Mbps".format(bitsPerSec / 1_000_000.0)
        bitsPerSec >= 1_000 -> "%.0f Kbps".format(bitsPerSec / 1_000.0)
        else -> "%.0f bps".format(bitsPerSec)
    }
}
