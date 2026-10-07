package com.frankgp.appinfo.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        val applications = pm.getInstalledApplications(flags)
        
        val appList = mutableListOf<AppInfo>()

        for (appInfo in applications) {
            try {
                val name = pm.getApplicationLabel(appInfo).toString()
                val packageName = appInfo.packageName
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                var versionName = "Desconocida"
                var versionCode = 0L
                var firstInstallTime = 0L
                var lastUpdateTime = 0L
                try {
                    val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(packageName, 0)
                    }
                    versionName = pkgInfo.versionName ?: "Desconocida"
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode.toLong()
                    }
                    firstInstallTime = pkgInfo.firstInstallTime
                    lastUpdateTime = pkgInfo.lastUpdateTime
                } catch (_: Exception) {
                    versionName = "N/A"
                }

                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    null
                }

                val analysis = analyzeApk(context, appInfo)

                appList.add(
                    AppInfo(
                        name = name,
                        packageName = packageName,
                        versionName = versionName,
                        versionCode = versionCode,
                        icon = icon,
                        languageType = analysis.languageType,
                        framework = analysis.framework,
                        architectures = analysis.architectures,
                        nativeLibraries = analysis.nativeLibraries,
                        isSystemApp = isSystemApp,
                        apkPath = appInfo.sourceDir ?: "",
                        firstInstallTime = firstInstallTime,
                        lastUpdateTime = lastUpdateTime,
                        usesCredentialsApi = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        appList.sortedBy { it.name.lowercase() }
    }

    suspend fun checkUsesCredentialsApi(apkPath: String): Boolean = withContext(Dispatchers.IO) {
        if (apkPath.isBlank()) return@withContext false
        try {
            ZipFile(apkPath).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.startsWith("classes") && name.endsWith(".dex")) {
                        zip.getInputStream(entry).use { input ->
                            val bytes = input.readBytes()
                            val text = String(bytes, Charsets.ISO_8859_1)
                            if (text.contains("androidx/credentials") || text.contains("CredentialManager")) {
                                return@withContext true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        false
    }

    private data class ApkAnalysis(
        val languageType: LanguageType,
        val framework: String?,
        val architectures: List<String>,
        val nativeLibraries: List<String>
    )

    private fun analyzeApk(context: Context, appInfo: ApplicationInfo): ApkAnalysis {
        val apkPaths = mutableListOf<String>()
        appInfo.sourceDir?.let { apkPaths.add(it) }
        appInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

        val nativeLibs = mutableSetOf<String>()
        val architectures = mutableSetOf<String>()
        val zipEntryNames = mutableSetOf<String>()
        var hasDex = false
        var hasNativeActivity = false

        try {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(appInfo.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(appInfo.packageName, PackageManager.GET_ACTIVITIES)
            }
            pkgInfo.activities?.forEach { act ->
                if (act.name?.contains("NativeActivity", ignoreCase = true) == true) {
                    hasNativeActivity = true
                }
            }
        } catch (_: Exception) {}

        for (path in apkPaths) {
            try {
                ZipFile(path).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        zipEntryNames.add(name)
                        if (name.startsWith("classes") && name.endsWith(".dex")) {
                            hasDex = true
                        }
                        if (name.startsWith("lib/")) {
                            val parts = name.split("/")
                            if (parts.size >= 3) {
                                val abi = parts[1]
                                architectures.add(abi)
                                val libName = parts.last()
                                if (libName.endsWith(".so")) {
                                    nativeLibs.add(libName)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val framework = detectFramework(nativeLibs, zipEntryNames)

        // Filter out standard AndroidX / Google / Chromium / SQLite infra libraries from counting as "custom native code"
        val customNativeLibs = nativeLibs.filter { lib ->
            val lower = lib.lowercase()
            !lower.contains("androidx") &&
            !lower.contains("sqlite") &&
            !lower.contains("cronet") &&
            !lower.contains("conscrypt") &&
            !lower.contains("app_inspector") &&
            !lower.contains("profilercpu") &&
            !lower.contains("jingle") &&
            !lower.contains("image_codec") &&
            !lower.contains("native_bridge")
        }

        val hasCustomSo = customNativeLibs.isNotEmpty()

        val languageType = when {
            framework != null -> {
                if (framework == "Unity" || framework == "Unreal Engine") LanguageType.NATIVE else LanguageType.HYBRID
            }
            hasDex && hasCustomSo -> LanguageType.HYBRID
            !hasDex && nativeLibs.isNotEmpty() -> LanguageType.NATIVE
            hasNativeActivity -> LanguageType.NATIVE
            else -> LanguageType.JAVA_KOTLIN
        }

        val formattedArchitectures = architectures.map { abi ->
            when (abi) {
                "arm64-v8a" -> "ARM64 (v8a)"
                "armeabi-v7a" -> "ARMv7 (v7a)"
                "x86" -> "x86"
                "x86_64" -> "x86_64"
                "armeabi" -> "ARMv5/v6"
                else -> abi
            }
        }.distinct()

        return ApkAnalysis(
            languageType = languageType,
            framework = framework,
            architectures = if (formattedArchitectures.isEmpty()) listOf("Independiente (Dalvik/ART)") else formattedArchitectures,
            nativeLibraries = nativeLibs.sorted()
        )
    }

    private fun detectFramework(nativeLibs: Set<String>, zipEntryNames: Set<String>): String? {
        if (nativeLibs.any { it.contains("unity", ignoreCase = true) || it.contains("il2cpp", ignoreCase = true) }) {
            return "Unity"
        }
        if (nativeLibs.any { it.contains("ue4", ignoreCase = true) || it.contains("unreal", ignoreCase = true) }) {
            return "Unreal Engine"
        }
        if (nativeLibs.any { it.contains("flutter", ignoreCase = true) } || zipEntryNames.any { it.contains("flutter_assets") }) {
            return "Flutter"
        }
        if (nativeLibs.any { it.contains("reactnative", ignoreCase = true) || it.contains("jsc", ignoreCase = true) } || zipEntryNames.any { it.contains("index.android.bundle") }) {
            return "React Native"
        }
        if (zipEntryNames.any { it.startsWith("assets/www/") || it.contains("www/index.html") || it.contains("capacitor.config.json") }) {
            return "Ionic / Cordova / Capacitor"
        }
        if (nativeLibs.any { it.contains("monosgen", ignoreCase = true) || it.contains("xamarin", ignoreCase = true) } || zipEntryNames.any { it.endsWith(".dll") }) {
            return "Xamarin / .NET"
        }
        if (nativeLibs.any { it.contains("cocos2d", ignoreCase = true) }) {
            return "Cocos2d"
        }
        return null
    }
}
