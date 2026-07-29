package com.togetherly.core.logging

import platform.Foundation.NSLog

private class IosAppLogger : AppLogger {
    override fun debug(tag: String, message: String) {
        NSLog("[$tag] $message")
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] WARN $message ${throwable?.message.orEmpty()}")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] ERROR $message ${throwable?.message.orEmpty()}")
    }
}

actual fun createPlatformLogger(): AppLogger = IosAppLogger()
