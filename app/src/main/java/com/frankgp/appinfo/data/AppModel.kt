package com.frankgp.appinfo.data

import android.graphics.drawable.Drawable

enum class LanguageType(val displayName: String) {
    JAVA_KOTLIN("Java / Kotlin"),
    NATIVE("Nativa (C++)"),
    HYBRID("Híbrida")
}

data class AppDetailsData(
    val languageType: LanguageType = LanguageType.JAVA_KOTLIN,
    val framework: String? = null,
    val architectures: List<String> = emptyList(),
    val nativeLibraries: List<String> = emptyList(),
    val sha1Fingerprint: String = "No disponible",
    val sha256Fingerprint: String = "No disponible",
    val permissionsCount: Int = 0,
    val dangerousPermissions: List<String> = emptyList(),
    val activitiesCount: Int = 0,
    val exportedActivitiesCount: Int = 0,
    val servicesCount: Int = 0,
    val receiversCount: Int = 0,
    val providersCount: Int = 0,
    val launcherActivity: String? = null,
    val detectedSdks: List<String> = emptyList(),
    val isSplitApk: Boolean = false,
    val isDebuggable: Boolean = false,
    val uid: Int = 0,
    val dataDir: String = "",
    val usesCredentialsApi: Boolean = false
)

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val apkPath: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val minSdkVersion: Int = 1,
    val targetSdkVersion: Int = 1,
    val apkSizeMb: Double = 0.0,
    val details: AppDetailsData? = null
) {
    val languageType: LanguageType
        get() = details?.languageType ?: LanguageType.JAVA_KOTLIN

    val framework: String?
        get() = details?.framework

    val architectures: List<String>
        get() = details?.architectures ?: emptyList()

    val nativeLibraries: List<String>
        get() = details?.nativeLibraries ?: emptyList()

    val usesCredentialsApi: Boolean
        get() = details?.usesCredentialsApi ?: false

    val isSplitApk: Boolean
        get() = details?.isSplitApk ?: false

    val isDebuggable: Boolean
        get() = details?.isDebuggable ?: false

    val uid: Int
        get() = details?.uid ?: 0

    val dataDir: String
        get() = details?.dataDir ?: ""

    val sha1Fingerprint: String
        get() = details?.sha1Fingerprint ?: "No disponible"

    val sha256Fingerprint: String
        get() = details?.sha256Fingerprint ?: "No disponible"

    val permissionsCount: Int
        get() = details?.permissionsCount ?: 0

    val dangerousPermissions: List<String>
        get() = details?.dangerousPermissions ?: emptyList()

    val activitiesCount: Int
        get() = details?.activitiesCount ?: 0

    val exportedActivitiesCount: Int
        get() = details?.exportedActivitiesCount ?: 0

    val servicesCount: Int
        get() = details?.servicesCount ?: 0

    val receiversCount: Int
        get() = details?.receiversCount ?: 0

    val providersCount: Int
        get() = details?.providersCount ?: 0

    val launcherActivity: String?
        get() = details?.launcherActivity

    val detectedSdks: List<String>
        get() = details?.detectedSdks ?: emptyList()
}

fun getAndroidVersionName(sdkInt: Int): String {
    return when (sdkInt) {
        1 -> "Android 1.0"
        2 -> "Android 1.1"
        3 -> "Android 1.5 Cupcake"
        4 -> "Android 1.6 Donut"
        5 -> "Android 2.0 Eclair"
        6 -> "Android 2.0.1 Eclair"
        7 -> "Android 2.1 Eclair"
        8 -> "Android 2.2 Froyo"
        9 -> "Android 2.3 Gingerbread"
        10 -> "Android 2.3.3 Gingerbread"
        11 -> "Android 3.0 Honeycomb"
        12 -> "Android 3.1 Honeycomb"
        13 -> "Android 3.2 Honeycomb"
        14 -> "Android 4.0 Ice Cream Sandwich"
        15 -> "Android 4.0.3 Ice Cream Sandwich"
        16 -> "Android 4.1 Jelly Bean"
        17 -> "Android 4.2 Jelly Bean"
        18 -> "Android 4.3 Jelly Bean"
        19 -> "Android 4.4 KitKat"
        20 -> "Android 4.4W KitKat Wear"
        21 -> "Android 5.0 Lollipop"
        22 -> "Android 5.1 Lollipop"
        23 -> "Android 6.0 Marshmallow"
        24 -> "Android 7.0 Nougat"
        25 -> "Android 7.1 Nougat"
        26 -> "Android 8.0 Oreo"
        27 -> "Android 8.1 Oreo"
        28 -> "Android 9.0 Pie"
        29 -> "Android 10"
        30 -> "Android 11"
        31 -> "Android 12"
        32 -> "Android 12L"
        33 -> "Android 13"
        34 -> "Android 14"
        35 -> "Android 15"
        36 -> "Android 16"
        else -> "API $sdkInt"
    }
}
