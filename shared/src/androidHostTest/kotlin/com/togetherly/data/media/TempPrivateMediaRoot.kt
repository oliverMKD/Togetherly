package com.togetherly.data.media

import java.io.File
import kotlin.io.path.createTempDirectory

/** A real, disposable temp directory — exercises real `java.io.File` I/O without touching a device. */
internal class TempPrivateMediaRoot : PrivateMediaRoot {
    val directory: File = createTempDirectory(prefix = "togetherly-media-test").toFile()

    override fun rootPath(): String = directory.absolutePath

    fun deleteRecursively() {
        directory.deleteRecursively()
    }
}
