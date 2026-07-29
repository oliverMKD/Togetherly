package com.togetherly.data.media

import com.togetherly.data.local.database.applicationSupportDirectoryPath
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

/**
 * `<Application Support>/media` — never Documents (user-visible) and never a public Photos-library
 * writeback, mirroring [com.togetherly.data.local.database.createDatabaseBuilder]'s own directory
 * choice. Application Support is *not* automatically excluded from iCloud/iTunes backup the way
 * `Library/Caches` is — only [NSURLIsExcludedFromBackupKey] actually keeps a family's private
 * photo/voice memories from leaving the device via a backup (Step 10.7's privacy audit), which is
 * why this sets it explicitly rather than relying on the directory choice alone.
 */
internal class IosPrivateMediaRoot : PrivateMediaRoot {
    @OptIn(ExperimentalForeignApi::class)
    override fun rootPath(): String {
        val directory = "${applicationSupportDirectoryPath()}/media"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        NSURL.fileURLWithPath(directory).setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
        return directory
    }
}
