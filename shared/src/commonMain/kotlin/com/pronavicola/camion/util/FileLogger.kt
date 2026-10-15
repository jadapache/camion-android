package com.pronavicola.camion.util

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogger(
    private val logDirectory: File,
    private val baseFileName: String = "camion.log",
    private val maxFileSizeBytes: Long = MAX_FILE_SIZE_BYTES,
    private val maxFiles: Int = MAX_FILES
) {
    companion object {
        const val MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L // 10 MB
        const val MAX_FILES = 5

        @Volatile
        var instance: FileLogger? = null
    }

    private val lock = Any()
    val currentLogFile: File = File(logDirectory, baseFileName)

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) = synchronized(lock) {
        try {
            rotateIfNeeded()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            FileWriter(currentLogFile, true).use { fw ->
                fw.write("[$timestamp] [$level/$tag]: $message\n")
                if (throwable != null) {
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    fw.write(sw.toString())
                    fw.write("\n")
                }
            }
        } catch (_: Exception) {
        }
    }

    fun rotateIfNeeded() {
        if (currentLogFile.exists() && currentLogFile.length() >= maxFileSizeBytes) {
            rotateFiles()
        }
    }

    fun rotateFiles() {
        val oldestFile = File(logDirectory, "$baseFileName.$maxFiles")
        if (oldestFile.exists()) {
            oldestFile.delete()
        }

        for (i in (maxFiles - 1) downTo 1) {
            val fileToShift = File(logDirectory, "$baseFileName.$i")
            if (fileToShift.exists()) {
                val nextFile = File(logDirectory, "$baseFileName.${i + 1}")
                fileToShift.renameTo(nextFile)
            }
        }

        if (currentLogFile.exists()) {
            val firstRotated = File(logDirectory, "$baseFileName.1")
            currentLogFile.renameTo(firstRotated)
        }
    }
}
