package com.frankgp.appinfo.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.frankgp.appinfo.MainActivity
import com.frankgp.appinfo.R
import com.frankgp.appinfo.data.PerformanceData
import com.frankgp.appinfo.data.formatSpeed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PerformanceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimeMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val initialNotification = buildNotification(
            title = "Iniciando monitor...",
            text = "Calculando rendimiento..."
        )
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        _isServiceRunning.value = true
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        updateJob?.cancel()

        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTimeMs = System.currentTimeMillis()

        updateJob = serviceScope.launch {
            while (isActive) {
                delay(1000)

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()
                val currentTimeMs = System.currentTimeMillis()

                val timeDiffSec = (currentTimeMs - lastTimeMs) / 1000.0
                if (timeDiffSec <= 0) continue

                val rxDiff = if (lastRxBytes >= 0 && currentRxBytes >= lastRxBytes) currentRxBytes - lastRxBytes else 0
                val txDiff = if (lastTxBytes >= 0 && currentTxBytes >= lastTxBytes) currentTxBytes - lastTxBytes else 0

                val downloadSpeed = (rxDiff / timeDiffSec).toLong()
                val uploadSpeed = (txDiff / timeDiffSec).toLong()

                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTimeMs = currentTimeMs

                // RAM Info
                var usedRamMb = 0L
                var totalRamMb = 0L
                var ramUsagePercent = 0
                try {
                    val actManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
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

                // Battery Info
                var batteryLevel = 0
                var isCharging = false
                try {
                    val batteryIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
                    } else {
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }
                    if (batteryIntent != null) {
                        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else 0
                        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    }
                } catch (_: Exception) {}

                val perfData = PerformanceData(
                    downloadSpeedBytesPerSec = downloadSpeed,
                    uploadSpeedBytesPerSec = uploadSpeed,
                    usedRamMb = usedRamMb,
                    totalRamMb = totalRamMb,
                    ramUsagePercent = ramUsagePercent,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging
                )

                _livePerformanceData.value = perfData

                // Update Notification
                val title = "⬇️ ${formatSpeed(downloadSpeed)}   ⬆️ ${formatSpeed(uploadSpeed)}"
                val text = "RAM: $ramUsagePercent% (%.1f GB/%.1f GB) • Batería: $batteryLevel%".format(
                    usedRamMb / 1024.0,
                    totalRamMb / 1024.0
                )

                updateNotification(title, text)
            }
        }
    }

    private fun updateNotification(title: String, text: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = buildNotification(title, text)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_speed)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Monitor de Rendimiento en Tiempo Real"
            val descriptionText = "Muestra la velocidad de bajada, subida y uso de RAM en la barra de estado"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    companion object {
        const val CHANNEL_ID = "performance_monitor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.frankgp.appinfo.STOP_PERFORMANCE_SERVICE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private val _livePerformanceData = MutableStateFlow(PerformanceData())
        val livePerformanceData: StateFlow<PerformanceData> = _livePerformanceData

        fun startService(context: Context) {
            try {
                val intent = Intent(context, PerformanceService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, PerformanceService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
