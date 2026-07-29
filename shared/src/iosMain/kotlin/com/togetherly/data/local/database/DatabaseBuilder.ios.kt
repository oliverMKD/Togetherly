package com.togetherly.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE_FILE_NAME = "togetherly.db"

/**
 * See [applicationSupportDirectoryPath]'s own KDoc for why Application Support, not Documents.
 */
internal actual fun createDatabaseBuilder(): RoomDatabase.Builder<TogetherlyDatabase> {
    val databaseDirectory = applicationSupportDirectoryPath()
    return Room.databaseBuilder<TogetherlyDatabase>(
        name = "$databaseDirectory/$DATABASE_FILE_NAME",
    )
}
