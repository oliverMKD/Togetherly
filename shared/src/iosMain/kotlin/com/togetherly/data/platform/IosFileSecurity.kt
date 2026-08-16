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

/**
 * An `NSFileManager.createFileAtPath(path:contents:attributes:)` attributes dictionary requesting
 * [NSFileProtectionComplete] at creation time — pass this directly to `createFileAtPath` instead of
 * calling [protectComplete] as a follow-up step. A follow-up `setAttributes` call leaves the file on
 * disk at the default (weaker) protection class for the window between the write and that call; a
 * process kill in that window leaves private media protected less than intended. Creating the file
 * already-protected closes that window entirely.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun completeProtectionAttributes(): Map<Any?, Any?> = mapOf(NSFileProtectionKey to NSFileProtectionComplete!!)

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
