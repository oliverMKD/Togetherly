package com.togetherly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.mp.KoinPlatform

private const val DATABASE_FILE_NAME = "togetherly.db"

/**
 * Room's Android builder requires a [Context], but this function's signature (shared with iOS)
 * can't take one directly. Rather than capturing an Activity or reaching for a hand-rolled global
 * holder, this resolves the application [Context] from the already-running Koin graph —
 * registered once by the Android app entry point using its own `applicationContext`, never an
 * Activity — via Koin's own supported service-locator entry point
 * ([KoinPlatform.getKoin]), not ad-hoc mutable global state.
 */
internal actual fun createDatabaseBuilder(): RoomDatabase.Builder<TogetherlyDatabase> {
    val appContext = KoinPlatform.getKoin().get<Context>().applicationContext
    val databaseFile = appContext.getDatabasePath(DATABASE_FILE_NAME)
    return Room.databaseBuilder<TogetherlyDatabase>(
        context = appContext,
        name = databaseFile.absolutePath,
    )
}
