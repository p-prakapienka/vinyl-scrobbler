import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    android {
        namespace = "io.github.pprakapienka.vinylscrobbler.shared"
        compileSdk = 36
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
            implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
            implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
            implementation("io.ktor:ktor-client-core:3.5.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.5.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.4")
            implementation("io.ktor:ktor-client-okhttp:3.5.2")
        }

        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation("io.ktor:ktor-client-cio:3.5.2")
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.pprakapienka.vinylscrobbler.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "Vinyl Scrobbler"
            packageVersion = "0.2.0"
        }
    }
}
