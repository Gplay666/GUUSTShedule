import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
    id("com.android.application") version "9.5.0"
}

group = "ru.uust.schedule"
version = "0.4.0"

val composeVersion = "1.12.2"
val material3Version = "1.9.0"
val ktorVersion = "3.6.0"
val coroutinesVersion = "1.11.0"
val serializationVersion = "1.12.0"

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
            implementation("org.jetbrains.compose.material3:material3:$material3Version")
            implementation("org.jetbrains.compose.ui:ui:$composeVersion")
            implementation("org.jetbrains.compose.ui:ui-backhandler:$composeVersion")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:$composeVersion")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.4")
            implementation("androidx.core:core-ktx:1.19.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
        }

        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
        }
    }
}

android {
    namespace = "ru.uust.schedule"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "ru.uust.schedule"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = project.version.toString()
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "ru.uust.schedule.MainKt"

        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "uust-schedule"
            packageVersion = "0.4.0"
            description = "Расписание УУНиТ"
            vendor = "UUST Schedule"
            copyright = "© 2026 UUST Schedule"
            modules("jdk.crypto.ec")

            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                menuGroup = "Education"
                appCategory = "Education"
                shortcut = true
            }
        }
    }
}
