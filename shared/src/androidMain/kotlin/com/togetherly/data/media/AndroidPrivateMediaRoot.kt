package com.togetherly.data.media

import android.content.Context
import java.io.File
import org.koin.mp.KoinPlatform

/**
 * `<filesDir>/media` — internal app-specific storage, never `MediaStore`, never a broad storage
 * permission, mirroring how [com.togetherly.data.local.database.createDatabaseBuilder] resolves
 * the database's own private directory via [KoinPlatform] rather than constructor injection.
 */
internal class AndroidPrivateMediaRoot : PrivateMediaRoot {
    override fun rootPath(): String {
        val appContext = KoinPlatform.getKoin().get<Context>().applicationContext
        val root = File(appContext.filesDir, "media")
        root.mkdirs()
        return root.absolutePath
    }
}
