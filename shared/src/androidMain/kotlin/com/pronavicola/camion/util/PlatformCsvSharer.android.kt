package com.pronavicola.camion.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidPlatformCsvSharer(private val context: Context) : PlatformCsvSharer {
    override fun shareCsv(fileName: String, csvContent: String): Result<String> {
        return try {
            val exportDir = File(context.cacheDir, "csv_exports").apply {
                if (!exists()) mkdirs()
            }
            val cleanName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"
            val file = File(exportDir, cleanName)
            file.writeText(csvContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, cleanName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Abrir o guardar archivo CSV con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e("AndroidCsvSharer", "Error al exportar/compartir CSV: ${e.message}")
            Result.failure(e)
        }
    }
}

actual fun getPlatformCsvSharer(platformContext: Any?): PlatformCsvSharer {
    val context = platformContext as? Context ?: error("Context required for Android PlatformCsvSharer")
    return AndroidPlatformCsvSharer(context)
}
