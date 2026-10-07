package com.frankgp.appinfo.data

import android.graphics.drawable.Drawable

enum class LanguageType(val displayName: String) {
    JAVA_KOTLIN("Java / Kotlin"),
    NATIVE("Nativa (C++)"),
    HYBRID("Híbrida")
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val languageType: LanguageType,
    val framework: String?,
    val architectures: List<String>,
    val nativeLibraries: List<String>,
    val isSystemApp: Boolean,
    val apkPath: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val usesCredentialsApi: Boolean
)
