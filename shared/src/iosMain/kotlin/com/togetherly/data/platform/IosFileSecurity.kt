package com.togetherly.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionComplete
import platform.Foundation.NSFileProtectionCompleteUntilFirstUserAuthentication
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

@OptIn(ExperimentalForeignApi::class)
internal fun protectUntilFirstUserAuthentication(path: String, excludeFromBackup: Boolean = false) {
    applyProtection(path = path, protection = NSFileProtectionCompleteUntilFirstUserAuthentication!!, excludeFromBackup = excludeFromBackup)
}

@OptIn(ExperimentalForeignApi::class)
internal fun protectComplete(path: String, excludeFromBackup: Boolean = false) {
    applyProtection(path = path, protection = NSFileProtectionComplete!!, excludeFromBackup = excludeFromBackup)
}

@OptIn(ExperimentalForeignApi::class)
private fun applyProtection(path: String, protection: String, excludeFromBackup: Boolean) {
    NSFileManager.defaultManager.setAttributes(
        attributes = mapOf(NSFileProtectionKey to protection),
        ofItemAtPath = path,
        error = null,
    )
    if (excludeFromBackup) {
        NSURL.fileURLWithPath(path).setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    }
}
