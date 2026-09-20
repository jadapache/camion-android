package com.pronavicola.camion.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun createFurgonesDatabase(platformContext: Any?): FurgonesDatabase {
    val dataDir = File(System.getProperty("user.home"), ".camion")
    if (!dataDir.exists()) {
        dataDir.mkdirs()
    }
    val dbFile = File(dataDir, FurgonesDatabase.DB_NAME)

    if (!dbFile.exists() || dbFile.length() == 0L) {
        Log.i("DatabaseFactory", "Extracting initial seed database to ${dbFile.absolutePath}")
        try {
            val classLoader = Thread.currentThread().contextClassLoader
                ?: FurgonesDatabase::class.java.classLoader
            val stream = classLoader.getResourceAsStream(FurgonesDatabase.DB_NAME)
                ?: FurgonesDatabase::class.java.getResourceAsStream("/" + FurgonesDatabase.DB_NAME)
            if (stream != null) {
                stream.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i("DatabaseFactory", "Database extracted successfully (${dbFile.length()} bytes)")
            } else {
                Log.w("DatabaseFactory", "Resource ${FurgonesDatabase.DB_NAME} not found in classpath. Room will create tables via migrations.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseFactory", "Failed to extract seed database", e)
        }
    }

    return Room.databaseBuilder<FurgonesDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
}
