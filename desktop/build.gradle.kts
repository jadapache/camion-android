import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
}

compose.desktop {
    application {
        mainClass = "com.pronavicola.camion.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage,
                TargetFormat.Msi,
                TargetFormat.Exe,
            )
            packageName = "camion-hmi"
            packageVersion = "1.0.0"
            description = "Camion HMI Industrial Telemetry and Control"
            vendor = "Pronavicola"
            copyright = "© 2026 Pronavicola"

            linux {
                shortcut = true
                menuGroup = "Industrial"
            }
            windows {
                shortcut = true
                menuGroup = "Pronavicola"
                upgradeUuid = "8b64e521-7291-4d3b-8b5e-26f59c836582"
            }
        }
    }
}
