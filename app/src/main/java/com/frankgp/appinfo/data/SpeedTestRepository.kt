package com.frankgp.appinfo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class SpeedTestRepository {

    private val cloudflareServer = "https://speed.cloudflare.com"
    private val goServer = "https://8080.frankgp.com"

    fun runSpeedTest(preferredServerUrl: String = cloudflareServer): Flow<SpeedTestResult> = flow {
        var pingMs = 0L
        var downloadMbps = 0.0
        var uploadMbps = 0.0

        val activeServer = if (preferredServerUrl.contains("8080.frankgp.com")) {
            if (testServerReachability("$goServer/health")) goServer else cloudflareServer
        } else {
            cloudflareServer
        }

        val serverDisplayName = if (activeServer == goServer) {
            "8080.frankgp.com (Servidor Go)"
        } else {
            "Cloudflare SpeedTest API"
        }

        // Step 1: Ping / Latency Test
        emit(
            SpeedTestResult(
                state = SpeedTestState.TESTING_PING,
                statusText = "Midiendo Latencia (Ping)...",
                serverName = serverDisplayName,
                progressPercent = 0.1f
            )
        )

        try {
            val pingTimes = mutableListOf<Long>()
            val pingUrl = if (activeServer == goServer) "$goServer/health" else "$cloudflareServer/__down?bytes=0"
            for (i in 1..3) {
                val start = System.currentTimeMillis()
                val url = URL(pingUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                val code = conn.responseCode
                val elapsed = System.currentTimeMillis() - start
                conn.disconnect()
                if (code == 200) {
                    pingTimes.add(elapsed)
                }
            }
            pingMs = if (pingTimes.isNotEmpty()) pingTimes.average().toLong() else 20L
        } catch (_: Exception) {
            pingMs = 0L
        }

        emit(
            SpeedTestResult(
                state = SpeedTestState.TESTING_DOWNLOAD,
                pingMs = pingMs,
                statusText = "Midiendo Velocidad de Bajada...",
                serverName = serverDisplayName,
                progressPercent = 0.3f
            )
        )

        // Step 2: Download Speed Test (10 MB)
        try {
            val totalTargetBytes = 10_000_000L
            val downloadUrl = if (activeServer == goServer) {
                "$goServer/download?bytes=$totalTargetBytes"
            } else {
                "$cloudflareServer/__down?bytes=$totalTargetBytes"
            }
            val url = URL(downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            val startDownloadTime = System.currentTimeMillis()
            var totalReadBytes = 0L
            val buffer = ByteArray(16384)
            val input: InputStream = conn.inputStream

            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                totalReadBytes += bytesRead
                val elapsedMs = System.currentTimeMillis() - startDownloadTime
                if (elapsedMs > 0) {
                    val currentMbps = (totalReadBytes * 8.0 / 1_000_000.0) / (elapsedMs / 1000.0)
                    downloadMbps = currentMbps
                    val prog = (0.3f + 0.35f * (totalReadBytes.toFloat() / totalTargetBytes.toFloat())).coerceIn(0.3f, 0.65f)
                    emit(
                        SpeedTestResult(
                            state = SpeedTestState.TESTING_DOWNLOAD,
                            pingMs = pingMs,
                            downloadMbps = downloadMbps,
                            progressPercent = prog,
                            serverName = serverDisplayName,
                            statusText = "Midiendo Bajada: %.1f Mbps".format(downloadMbps)
                        )
                    )
                }
                if (System.currentTimeMillis() - startDownloadTime > 6000) break
            }
            input.close()
            conn.disconnect()
        } catch (_: Exception) {}

        // Step 3: Upload Speed Test
        emit(
            SpeedTestResult(
                state = SpeedTestState.TESTING_UPLOAD,
                pingMs = pingMs,
                downloadMbps = downloadMbps,
                statusText = "Midiendo Velocidad de Subida...",
                serverName = serverDisplayName,
                progressPercent = 0.7f
            )
        )

        try {
            val uploadTargetBytes = 3_000_000 // 3 MB payload
            val uploadUrl = if (activeServer == goServer) {
                "$goServer/upload"
            } else {
                "$cloudflareServer/__up"
            }
            val url = URL(uploadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setFixedLengthStreamingMode(uploadTargetBytes)

            val dummyBuffer = ByteArray(16384)
            val startUploadTime = System.currentTimeMillis()
            var totalWrittenBytes = 0
            val output: OutputStream = conn.outputStream

            while (totalWrittenBytes < uploadTargetBytes) {
                val toWrite = minOf(dummyBuffer.size, uploadTargetBytes - totalWrittenBytes)
                output.write(dummyBuffer, 0, toWrite)
                totalWrittenBytes += toWrite

                val elapsedMs = System.currentTimeMillis() - startUploadTime
                if (elapsedMs > 0) {
                    val currentMbps = (totalWrittenBytes * 8.0 / 1_000_000.0) / (elapsedMs / 1000.0)
                    uploadMbps = currentMbps
                    val prog = (0.7f + 0.3f * (totalWrittenBytes.toFloat() / uploadTargetBytes.toFloat())).coerceIn(0.7f, 1.0f)
                    emit(
                        SpeedTestResult(
                            state = SpeedTestState.TESTING_UPLOAD,
                            pingMs = pingMs,
                            downloadMbps = downloadMbps,
                            uploadMbps = uploadMbps,
                            progressPercent = prog,
                            serverName = serverDisplayName,
                            statusText = "Midiendo Subida: %.1f Mbps".format(uploadMbps)
                        )
                    )
                }
                if (System.currentTimeMillis() - startUploadTime > 5000) break
            }
            output.flush()
            output.close()
            conn.disconnect()
        } catch (_: Exception) {}

        // Step 4: Completed
        emit(
            SpeedTestResult(
                state = SpeedTestState.COMPLETED,
                pingMs = pingMs,
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
                progressPercent = 1.0f,
                serverName = serverDisplayName,
                statusText = "Prueba Completada"
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun testServerReachability(fullPingUrl: String): Boolean {
        return try {
            val url = URL(fullPingUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }
}
