package com.pronavicola.camion.util

import java.awt.Desktop
import java.io.File

class DesktopPlatformCsvSharer : PlatformCsvSharer {
    override fun shareCsv(fileName: String, csvContent: String): Result<String> {
        return try {
            val exportDir = File(System.getProperty("java.io.tmpdir"), "csv_exports").apply {
                if (!exists()) mkdirs()
            }
            val cleanName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"
            val file = File(exportDir, cleanName)
            file.writeText(csvContent, Charsets.UTF_8)

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
            }
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

actual fun getPlatformCsvSharer(platformContext: Any?): PlatformCsvSharer {
    return DesktopPlatformCsvSharer()
}
