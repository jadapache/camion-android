package com.pronavicola.camion.util

import java.io.File
import java.util.UUID

class DesktopPlatformInfoProvider : PlatformInfoProvider {
    private val isWindows = System.getProperty("os.name", "").contains("Windows", ignoreCase = true)
    private val isLinux = System.getProperty("os.name", "").contains("Linux", ignoreCase = true)

    override fun getHardwareFingerprint(): String {
        return try {
            if (isLinux) {
                getLinuxMachineId() ?: getPersistentUuid()
            } else if (isWindows) {
                getWindowsMachineGuid() ?: getPersistentUuid()
            } else {
                getPersistentUuid()
            }
        } catch (_: Exception) {
            getPersistentUuid()
        }
    }

    override fun getDeviceBrand(): String {
        return if (isLinux) {
            getLinuxOsName() ?: "Linux"
        } else if (isWindows) {
            "Microsoft Windows"
        } else {
            System.getProperty("os.name", "Desktop")
        }
    }

    override fun getDeviceModel(): String {
        return if (isLinux) {
            getLinuxDeviceModel() ?: System.getProperty("os.arch", "x86_64")
        } else if (isWindows) {
            System.getenv("COMPUTERNAME") ?: System.getProperty("os.arch", "x86_64")
        } else {
            System.getProperty("os.arch", "Desktop")
        }
    }

    override fun getOsVersion(): String {
        val osName = System.getProperty("os.name", "Desktop")
        val osVersion = System.getProperty("os.version", "1.0")
        return "$osName $osVersion"
    }

    private fun getLinuxMachineId(): String? {
        val paths = listOf("/etc/machine-id", "/var/lib/dbus/machine-id")
        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) return "LINUX_$content"
            }
        }
        return null
    }

    private fun getWindowsMachineGuid(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val match = Regex("""MachineGuid\s+REG_SZ\s+([a-zA-Z0-9\-]+)""").find(output)
            match?.groupValues?.get(1)?.let { "WIN_$it" }
        } catch (_: Exception) {
            null
        }
    }

    private fun getLinuxOsName(): String? {
        val file = File("/etc/os-release")
        if (file.exists() && file.canRead()) {
            val line = file.useLines { lines ->
                lines.firstOrNull { it.startsWith("PRETTY_NAME=") || it.startsWith("NAME=") }
            }
            if (line != null) {
                return line.substringAfter("=").replace("\"", "").trim()
            }
        }
        return null
    }

    private fun getLinuxDeviceModel(): String? {
        val piModel = File("/proc/device-tree/model")
        if (piModel.exists() && piModel.canRead()) {
            return piModel.readText().trim().replace("\u0000", "")
        }
        val dmiProduct = File("/sys/devices/virtual/dmi/id/product_name")
        if (dmiProduct.exists() && dmiProduct.canRead()) {
            val name = dmiProduct.readText().trim()
            if (name.isNotBlank() && name != "System Product Name") return name
        }
        return null
    }

    private fun getPersistentUuid(): String {
        val dataDir = File(System.getProperty("user.home"), ".camion")
        if (!dataDir.exists()) dataDir.mkdirs()
        val file = File(dataDir, "device_id")
        if (file.exists()) {
            val id = file.readText().trim()
            if (id.isNotBlank()) return id
        }
        val newId = "DESKTOP_${UUID.randomUUID().toString().take(12).uppercase()}"
        try {
            file.writeText(newId)
        } catch (_: Exception) {
        }
        return newId
    }
}

actual fun getPlatformInfoProvider(platformContext: Any?): PlatformInfoProvider {
    return DesktopPlatformInfoProvider()
}
