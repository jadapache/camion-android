package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun createFurgonesDatabase(
    platformContext: Any?
): FurgonesDatabase {
    val context = platformContext as? Context
        ?: error("Context required for Android FurgonesDatabase")

    val dbFile = context.getDatabasePath(FurgonesDatabase.DB_NAME)

    // Inicializar desde assets únicamente en la primera instalación.
    if (!dbFile.exists()) {
        dbFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) {
                "Could not create database directory: ${parent.absolutePath}"
            }
        }

        val tempFile = File(dbFile.parentFile, "${dbFile.name}.tmp")

        try {
            context.assets.open(FurgonesDatabase.DB_NAME).use { input ->
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
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
}