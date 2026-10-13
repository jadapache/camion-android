package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.Dispatchers

actual fun createFurgonesDatabase(platformContext: Any?): FurgonesDatabase {
    val context = platformContext as? Context ?: error("Context required for Android FurgonesDatabase")
    val dbFile = context.getDatabasePath(FurgonesDatabase.DB_NAME)

    var needsSeed = !dbFile.exists() || dbFile.length() < 100_000L
    if (!needsSeed) {
        try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val cursor = db.rawQuery("SELECT count(*) FROM Definiciones", null)
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()
                if (count == 0) {
                    Log.w("DatabaseFactory", "Database exists but 'Definiciones' table is empty. Re-extracting from assets.")
                    needsSeed = true
                }
            }
        } catch (e: Exception) {
            Log.w("DatabaseFactory", "Check failed on existing database (${e.message}). Re-extracting seed database from assets.")
            needsSeed = true
        }
    }

    if (needsSeed) {
        dbFile.parentFile?.mkdirs()
        try {
            if (dbFile.exists()) {
                dbFile.delete()
            }
            java.io.File("${dbFile.absolutePath}-wal").delete()
            java.io.File("${dbFile.absolutePath}-shm").delete()
            java.io.File("${dbFile.absolutePath}-journal").delete()

            context.assets.open(FurgonesDatabase.DB_NAME).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i("DatabaseFactory", "Extracted initial seed database from assets to ${dbFile.absolutePath} (${dbFile.length()} bytes)")
        } catch (e: Exception) {
            Log.e("DatabaseFactory", "Could not copy initial seed database from assets: ${e.message}")
        }
    }

    return Room.databaseBuilder<FurgonesDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
