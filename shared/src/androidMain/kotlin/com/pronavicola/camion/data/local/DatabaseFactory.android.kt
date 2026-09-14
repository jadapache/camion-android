package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.Dispatchers

actual fun createFurgonesDatabase(platformContext: Any?): FurgonesDatabase {
    val context = platformContext as? Context ?: error("Context required for Android FurgonesDatabase")
    val dbFile = context.getDatabasePath(FurgonesDatabase.DB_NAME)

    if (!dbFile.exists() || dbFile.length() == 0L) {
        dbFile.parentFile?.mkdirs()
        try {
            context.assets.open(FurgonesDatabase.DB_NAME).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i("DatabaseFactory", "Extracted initial seed database from assets to ${dbFile.absolutePath}")
        } catch (e: Exception) {
            Log.w("DatabaseFactory", "Could not copy initial seed database from assets: ${e.message}")
        }
    }

    return Room.databaseBuilder<FurgonesDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*FurgonesDatabase.getMigrations())
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
}
