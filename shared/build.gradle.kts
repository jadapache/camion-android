plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

android {
    namespace = "com.pronavicola.camion.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.12.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.12.1")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.ui:ui:1.12.1")
                implementation("org.jetbrains.compose.components:components-resources:1.12.1")
                implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.12.1")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

                // Lifecycle & ViewModel Multiplatform
                implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

                // Room Multiplatform
                implementation("androidx.room:room-runtime:2.8.5")
                implementation("androidx.sqlite:sqlite-bundled:2.7.1")

                // MQTT Client (Pure Java SE, runs on Android and JVM Desktop)
                implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

                // JSON
                implementation("org.json:json:20260814")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.19.1")
                implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")
                implementation("androidx.compose.ui:ui-tooling:1.12.1")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.pronavicola.camion.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.5")
    add("kspDesktop", "androidx.room:room-compiler:2.8.5")
}
