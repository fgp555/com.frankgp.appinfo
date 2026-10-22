package com.frankgp.appinfo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.frankgp.appinfo.data.PerformanceData
import com.frankgp.appinfo.data.SpeedTestResult
import com.frankgp.appinfo.data.SpeedTestState
import com.frankgp.appinfo.data.formatSpeed
import com.frankgp.appinfo.service.PerformanceService

@Composable
fun PerformanceScreen(
    performanceData: PerformanceData,
    speedTestResult: SpeedTestResult = SpeedTestResult(),
    selectedServerUrl: String = "https://speed.cloudflare.com",
    onSelectServerUrl: (String) -> Unit = {},
    onRunSpeedTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Switch Tile Card (En Desarrollo)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(context, "Funcionalidad en desarrollo 🛠️", Toast.LENGTH_SHORT).show()
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Notificación en Tiempo Real",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Próximamente en barra de estado (En desarrollo 🛠️)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Switch(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {
                        Toast.makeText(context, "Funcionalidad en desarrollo 🛠️", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 1. Red y Ancho de Banda (Upload & Download Speed)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text("Ancho de Banda de Red (Velocidad)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Download
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1976D2).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF1976D2))
                            Text("Bajada (Download)", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = formatSpeed(performanceData.downloadSpeedBytesPerSec),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Upload
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF2E7D32))
                            Text("Subida (Upload)", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = formatSpeed(performanceData.uploadSpeedBytesPerSec),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }

        // Test de Velocidad de Red
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(22.dp))
                    Column {
                        Text("Test de Velocidad de Red", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (speedTestResult.serverName.isNotBlank()) {
                            Text(
                                text = "Servidor Activo: ${speedTestResult.serverName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()

                // Server Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedServerUrl == "https://8080.frankgp.com",
                        onClick = { onSelectServerUrl("https://8080.frankgp.com") },
                        label = { Text("Servidor Go (8080)") }
                    )
                    FilterChip(
                        selected = selectedServerUrl == "https://speed.cloudflare.com",
                        onClick = { onSelectServerUrl("https://speed.cloudflare.com") },
                        label = { Text("Cloudflare API") }
                    )
                }

                if (speedTestResult.state != SpeedTestState.IDLE) {
                    Text(
                        text = speedTestResult.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { speedTestResult.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Latencia (Ping):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (speedTestResult.pingMs > 0) "${speedTestResult.pingMs} ms" else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Bajada (Mbps):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (speedTestResult.downloadMbps > 0) "%.1f Mbps".format(speedTestResult.downloadMbps) else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Column {
                        Text("Subida (Mbps):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (speedTestResult.uploadMbps > 0) "%.1f Mbps".format(speedTestResult.uploadMbps) else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                val isTesting = speedTestResult.state == SpeedTestState.TESTING_PING ||
                        speedTestResult.state == SpeedTestState.TESTING_DOWNLOAD ||
                        speedTestResult.state == SpeedTestState.TESTING_UPLOAD

                Button(
                    onClick = onRunSpeedTest,
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isTesting) Icons.Default.Refresh else Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTesting) "Midiendo..." else "🚀 Iniciar Test de Velocidad")
                }
            }
        }

        // 2. Uso de Procesador (CPU)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(22.dp))
                    Text("Procesador (CPU)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()

                val cpuProgressTarget = (performanceData.cpuUsagePercent / 100f).coerceIn(0f, 1f)
                val animatedCpuProgress by animateFloatAsState(targetValue = cpuProgressTarget, label = "cpuProgress")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Uso de CPU:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${performanceData.cpuUsagePercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                LinearProgressIndicator(
                    progress = { animatedCpuProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (performanceData.cpuUsagePercent > 80) MaterialTheme.colorScheme.error else Color(0xFFD32F2F)
                )
            }
        }

        // 3. Uso de Memoria RAM
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text("Memoria RAM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()

                val progressTarget = if (performanceData.totalRamMb > 0) performanceData.ramUsagePercent / 100f else 0f
                val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "ramProgress")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Uso de RAM:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${performanceData.ramUsagePercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (performanceData.ramUsagePercent > 85) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "En uso: %.1f GB de %.1f GB".format(
                        performanceData.usedRamMb / 1024.0,
                        performanceData.totalRamMb / 1024.0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Estado de Batería
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                    Text("Batería", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nivel Actual:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${performanceData.batteryLevel}% ${if (performanceData.isCharging) "(⚡ Cargando)" else "(🔋 Desconectado)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (performanceData.isCharging) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
