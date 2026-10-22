package com.frankgp.appinfo.data

enum class SpeedTestState {
    IDLE,
    TESTING_PING,
    TESTING_DOWNLOAD,
    TESTING_UPLOAD,
    COMPLETED,
    ERROR
}

data class SpeedTestResult(
    val state: SpeedTestState = SpeedTestState.IDLE,
    val pingMs: Long = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val progressPercent: Float = 0f,
    val statusText: String = "Listo para probar",
    val errorMessage: String? = null
)
