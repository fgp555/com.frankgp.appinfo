package com.frankgp.appinfo.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
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

                val apkFile = appInfo.sourceDir?.let { File(it) }
                val apkSizeMb = if (apkFile != null && apkFile.exists()) {
                    apkFile.length() / (1024.0 * 1024.0)
                } else {
                    0.0
                }

                val minSdkVersion = appInfo.minSdkVersion
                val targetSdkVersion = appInfo.targetSdkVersion

                appList.add(
                    AppInfo(
                        name = name,
                        packageName = packageName,
                        versionName = versionName,
                        versionCode = versionCode,
                        icon = icon,
                        isSystemApp = isSystemApp,
                        apkPath = appInfo.sourceDir ?: "",
                        firstInstallTime = firstInstallTime,
                        lastUpdateTime = lastUpdateTime,
                        minSdkVersion = minSdkVersion,
                        targetSdkVersion = targetSdkVersion,
                        apkSizeMb = apkSizeMb,
                        details = null
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        appList.sortedBy { it.name.lowercase() }
    }

    suspend fun getAppDetails(packageName: String, apkPath: String): AppDetailsData = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
        } catch (_: Exception) {
            return@withContext AppDetailsData()
        }

        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val uid = appInfo.uid
        val dataDir = appInfo.dataDir ?: ""
        val isSplitApk = !appInfo.splitSourceDirs.isNullOrEmpty()

        val fingerprints = getSigningFingerprints(context, packageName)
        val permissionData = getPermissionData(context, packageName)
        val componentData = getComponentData(context, packageName)
        val analysis = analyzeApk(context, appInfo)
        val usesCredentialsApi = checkUsesCredentialsApi(apkPath)

        AppDetailsData(
            languageType = analysis.languageType,
            framework = analysis.framework,
            architectures = analysis.architectures,
            nativeLibraries = analysis.nativeLibraries,
            sha1Fingerprint = fingerprints.sha1,
            sha256Fingerprint = fingerprints.sha256,
            permissionsCount = permissionData.totalPermissions,
            dangerousPermissions = permissionData.dangerousPermissions,
            activitiesCount = componentData.activitiesCount,
            exportedActivitiesCount = componentData.exportedActivitiesCount,
            servicesCount = componentData.servicesCount,
            receiversCount = componentData.receiversCount,
            providersCount = componentData.providersCount,
            launcherActivity = componentData.launcherActivity,
            detectedSdks = analysis.detectedSdks,
            isSplitApk = isSplitApk,
            isDebuggable = isDebuggable,
            uid = uid,
            dataDir = dataDir,
            usesCredentialsApi = usesCredentialsApi
        )
    }

    private fun checkUsesCredentialsApi(apkPath: String): Boolean {
        if (apkPath.isBlank()) return false
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
                                return true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private data class SigningFingerprints(val sha1: String, val sha256: String)

    private fun getSigningFingerprints(context: Context, packageName: String): SigningFingerprints {
        return try {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }
            if (!signatures.isNullOrEmpty()) {
                val cert = signatures[0].toByteArray()

                val mdSha1 = MessageDigest.getInstance("SHA-1")
                val sha1Str = mdSha1.digest(cert).joinToString(":") { "%02X".format(it) }

                val mdSha256 = MessageDigest.getInstance("SHA-256")
                val sha256Str = mdSha256.digest(cert).joinToString(":") { "%02X".format(it) }

                SigningFingerprints(sha1Str, sha256Str)
            } else {
                SigningFingerprints("No disponible", "No disponible")
            }
        } catch (_: Exception) {
            SigningFingerprints("No disponible", "No disponible")
        }
    }

    private data class PermissionData(
        val totalPermissions: Int,
        val dangerousPermissions: List<String>
    )

    private fun getPermissionData(context: Context, packageName: String): PermissionData {
        var total = 0
        val dangerousList = mutableListOf<String>()
        try {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val requested = pkgInfo.requestedPermissions
            if (requested != null) {
                total = requested.size
                for (perm in requested) {
                    val shortName = perm.substringAfterLast(".")
                    try {
                        val pInfo = pm.getPermissionInfo(perm, 0)
                        val isDangerous = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pInfo.protection == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
                        } else {
                            @Suppress("DEPRECATION")
                            (pInfo.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_DANGEROUS) != 0
                        }
                        if (isDangerous) {
                            dangerousList.add(shortName)
                        }
                    } catch (_: Exception) {
                        val lower = perm.lowercase()
                        if (lower.contains("camera") || lower.contains("location") || lower.contains("audio") ||
                            lower.contains("storage") || lower.contains("contacts") || lower.contains("sms") ||
                            lower.contains("bluetooth")
                        ) {
                            dangerousList.add(shortName)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return PermissionData(total, dangerousList.distinct().sorted())
    }

    private data class ComponentData(
        val activitiesCount: Int,
        val exportedActivitiesCount: Int,
        val servicesCount: Int,
        val receiversCount: Int,
        val providersCount: Int,
        val launcherActivity: String?
    )

    private fun getComponentData(context: Context, packageName: String): ComponentData {
        var activities = 0
        var exportedActivities = 0
        var services = 0
        var receivers = 0
        var providers = 0
        var launcher: String? = null

        try {
            val pm = context.packageManager
            launcher = pm.getLaunchIntentForPackage(packageName)?.component?.className

            val flags = (PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS).toLong()
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags.toInt())
            }

            pkgInfo.activities?.let {
                activities = it.size
                exportedActivities = it.count { act -> act.exported }
            }
            pkgInfo.services?.let { services = it.size }
            pkgInfo.receivers?.let { receivers = it.size }
            pkgInfo.providers?.let { providers = it.size }
        } catch (_: Exception) {}

        return ComponentData(
            activitiesCount = activities,
            exportedActivitiesCount = exportedActivities,
            servicesCount = services,
            receiversCount = receivers,
            providersCount = providers,
            launcherActivity = launcher
        )
    }

    private data class ApkAnalysis(
        val languageType: LanguageType,
        val framework: String?,
        val architectures: List<String>,
        val nativeLibraries: List<String>,
        val detectedSdks: List<String>
    )

    private fun analyzeApk(context: Context, appInfo: ApplicationInfo): ApkAnalysis {
        val apkPaths = mutableListOf<String>()
        appInfo.sourceDir?.let { apkPaths.add(it) }
        appInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

        val nativeLibs = mutableSetOf<String>()
        val architectures = mutableSetOf<String>()
        val zipEntryNames = mutableSetOf<String>()
        val detectedSdks = mutableSetOf<String>()
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
                            try {
                                zip.getInputStream(entry).use { input ->
                                    val bytes = input.readBytes()
                                    val text = String(bytes, Charsets.ISO_8859_1)
                                    if (text.contains("androidx/compose")) detectedSdks.add("Jetpack Compose")
                                    if (text.contains("dagger/hilt") || text.contains("dagger/internal")) detectedSdks.add("Hilt / Dagger")
                                    if (text.contains("org/koin")) detectedSdks.add("Koin")
                                    if (text.contains("retrofit2")) detectedSdks.add("Retrofit")
                                    if (text.contains("okhttp3")) detectedSdks.add("OkHttp")
                                    if (text.contains("androidx/room")) detectedSdks.add("Room")
                                    if (text.contains("com/google/firebase")) detectedSdks.add("Firebase")
                                    if (text.contains("com/bumptech/glide")) detectedSdks.add("Glide")
                                    if (text.contains("coil")) detectedSdks.add("Coil")
                                    if (text.contains("com/facebook")) detectedSdks.add("Facebook SDK")
                                    if (text.contains("com/google/android/gms")) detectedSdks.add("Google Play Services")
                                }
                            } catch (_: Exception) {}
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
            nativeLibraries = nativeLibs.sorted(),
            detectedSdks = detectedSdks.sorted()
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
