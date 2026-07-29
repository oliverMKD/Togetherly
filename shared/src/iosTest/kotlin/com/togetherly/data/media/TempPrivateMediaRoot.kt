package com.togetherly.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/** A real, disposable temp directory on the simulator — exercises real `NSFileManager` I/O. */
@OptIn(ExperimentalForeignApi::class)
internal class TempPrivateMediaRoot : PrivateMediaRoot {
    private val path: String = "${NSTemporaryDirectory()}togetherly-media-test-${NSUUID().UUIDString}"

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }

    override fun rootPath(): String = path

    fun deleteRecursively() {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}
