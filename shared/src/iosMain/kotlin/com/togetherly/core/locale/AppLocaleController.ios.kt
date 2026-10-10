package com.togetherly.core.locale

import platform.Foundation.NSUserDefaults

/**
 * `AppleLanguages` is the standard iOS preference for a preferred-language override — the same
 * key iOS itself reads at process start to resolve `NSBundle`/`NSLocale` preferred languages. It
 * only takes effect on the *next* launch (see [applyPlatformLocale]'s own KDoc for why there is no
 * reliable in-process hot-swap), which is why [requiresRestartForLocaleChange] is `true` here.
 */
private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

internal actual fun applyPlatformLocale(code: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (code == null) {
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    } else {
        defaults.setObject(listOf(code), forKey = APPLE_LANGUAGES_KEY)
    }
}

internal actual val requiresRestartForLocaleChange: Boolean = true
