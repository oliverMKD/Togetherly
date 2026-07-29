package com.togetherly.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "database_metadata")
internal data class DatabaseMetadataEntity(
    @PrimaryKey
    val key: String,
    val value: String,
)
