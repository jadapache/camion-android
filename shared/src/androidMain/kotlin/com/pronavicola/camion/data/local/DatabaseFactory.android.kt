package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun createFurgonesDatabase(platformContext: Any?): FurgonesDatabase {
    val context = platformContext as? Context ?: error("Context required for Android FurgonesDatabase")
    val dbFile = context.getDatabasePath(FurgonesDatabase.DB_NAME)

    return Room.databaseBuilder<FurgonesDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
        .createFromAsset(FurgonesDatabase.DB_NAME)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*FurgonesDatabase.getMigrations())
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
}
