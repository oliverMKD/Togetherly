package com.togetherly.data.media

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID

/** A real, disposable directory under the instrumented app's cache dir. */
internal class TempPrivateMediaRoot : PrivateMediaRoot {
    val directory: File = File(
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "media-test-${UUID.randomUUID()}",
    ).apply { mkdirs() }

    override fun rootPath(): String = directory.absolutePath

    fun deleteRecursively() {
        directory.deleteRecursively()
    }
}
