plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.triplet.play)
}

play {
    serviceAccountCredentials.set(file("E:/dev/android/play-service-account.json"))
    track.set("internal")
}

android {
    namespace = "com.frankgp.appinfo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.frankgp.appinfo"
        minSdk = 24
        targetSdk = 37
        versionCode = 7
        versionName = "26.10.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /* 
    https://play.google.com/console/u/0/developers/9004578905216647982/app/4973761786136133435/tracks/internal-testing
    adb shell am start -a android.intent.action.VIEW -d "market://details?id=com.frankgp.appinfo" 
    */

    signingConfigs {
        create("release") {
            storeFile = file("E:/dev/android/release.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}