plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.pprakapienka.vinylscrobbler"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.pprakapienka.vinylscrobbler"
        minSdk = 26
        targetSdk = 36
        // CI passes -Pvinyl.versionCode so each build can replace the previous one.
        versionCode = (findProperty("vinyl.versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = "0.1.3"
    }

    signingConfigs {
        getByName("debug") {
            // Public sideload key, checked in on purpose so every APK can update the last one.
            storeFile = file("vinyl-scrobbler-debug.keystore")
            storePassword = "android"
            keyAlias = "vinylscrobblerdebug"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation("androidx.activity:activity-compose:1.12.4")
}
