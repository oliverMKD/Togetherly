package com.togetherly.data.local.database

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Application Support, not Documents — Documents is meant for user-visible/exportable files and
 * is included in iCloud/iTunes file-sharing backups by default; private local app data (the
 * database, and private memory media) belongs in Application Support instead. `create = true` has
 * [NSFileManager] create the directory (and any missing parent) if it doesn't exist yet, rather
 * than failing or needing a separate directory-creation call.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun applicationSupportDirectoryPath(): String {
    val directoryUrl = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(directoryUrl?.path) { "Could not resolve the application support directory" }
}
