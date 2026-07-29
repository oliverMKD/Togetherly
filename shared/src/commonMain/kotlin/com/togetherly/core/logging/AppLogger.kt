package com.togetherly.core.logging

/**
 * The one place technical failures (RevenueCat configuration/purchase/restore errors, and any
 * future infrastructure failure) get logged — never a raw `println`/`Log.`/`NSLog` call scattered
 * through feature or data code. Never log a complete secret/key, a receipt, a purchase token, or
 * anything that identifies a specific purchase or person; a message here is a short, developer-
 * facing technical description, not user-facing copy (that's [com.togetherly.core.ui.UiText]'s
 * job) and not a place to reconstruct what a family bought.
 */
interface AppLogger {
    fun debug(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/** Platform-appropriate [AppLogger] — `android.util.Log` on Android, `NSLog` on iOS. */
expect fun createPlatformLogger(): AppLogger
