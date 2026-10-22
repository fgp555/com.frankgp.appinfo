package com.frankgp.appinfo.data

data class DeviceInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,
    val manufacturer: String,
    val model: String,
    val brand: String,
    val board: String,
    val hardware: String,
    val cpuCores: Int,
    val supportedAbis: List<String>,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val availableStorageGb: Double,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensityDpi: Int,
    val refreshRateHz: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String
)
