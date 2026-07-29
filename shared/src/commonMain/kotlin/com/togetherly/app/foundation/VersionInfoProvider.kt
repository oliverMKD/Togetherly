package com.togetherly.app.foundation

/**
 * The platform-neutral model the About screen reads — backed by platform-specific configuration
 * (`BuildConfig` on Android, `Info.plist`/`NSBundle` on iOS), same [PlatformInfoProvider]
 * Koin-singleton convention. Both fields stay `String` — Android's own build number is an `Int`
 * (`versionCode`), but exposing that distinction here would leak a platform detail into shared
 * presentation code that only ever displays it as text.
 */
interface VersionInfoProvider {
    fun versionName(): String
    fun buildNumber(): String
}
