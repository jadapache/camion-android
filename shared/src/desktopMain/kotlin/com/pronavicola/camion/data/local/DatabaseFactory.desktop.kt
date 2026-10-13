package com.pronavicola.camion.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun createFurgonesDatabase(
    platformContext: Any?
): FurgonesDatabase {
    val dataDir = File(
        System.getProperty("user.home"),
        ".camion"
    )

    check(dataDir.exists() || dataDir.mkdirs()) {
        "Could not create database directory: ${dataDir.absolutePath}"
    }

    val dbFile = File(dataDir, FurgonesDatabase.DB_NAME)

    if (!dbFile.exists()) {
        val classLoader =
            Thread.currentThread().contextClassLoader
                ?: FurgonesDatabase::class.java.classLoader

        val resource =
            classLoader.getResourceAsStream(FurgonesDatabase.DB_NAME)
                ?: FurgonesDatabase::class.java
                    .getResourceAsStream("/${FurgonesDatabase.DB_NAME}")
                ?: error(
                    "Database resource ${FurgonesDatabase.DB_NAME} not found"
                )

        val tempFile = File(dataDir, "${dbFile.name}.tmp")

        try {
            resource.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            check(tempFile.renameTo(dbFile)) {
                "Could not install initial database at ${dbFile.absolutePath}"
            }
        } finally {
            tempFile.delete()
        }
    }

    return Room.databaseBuilder<FurgonesDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}