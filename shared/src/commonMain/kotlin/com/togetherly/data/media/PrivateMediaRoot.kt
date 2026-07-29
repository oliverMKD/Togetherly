package com.togetherly.data.media

/**
 * Resolves (and ensures the existence of) the app-private directory all memory media lives
 * under — `<app-private-storage>/media` on both platforms, mirroring how
 * [com.togetherly.data.local.database.createDatabaseBuilder] resolves the database's own private
 * directory. Never exposed to domain or UI code; only [PrivateMediaStorage] implementations use it.
 */
interface PrivateMediaRoot {
    fun rootPath(): String
}
