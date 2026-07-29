package com.togetherly.data.purchase

/**
 * Supplies this platform's RevenueCat **public** SDK key — never a secret key, and never a value
 * this interface's own implementation should read from committed Kotlin source. Each platform's
 * app-level bootstrap ([com.togetherly.app.di.initKoin]'s caller — `TogetherlyApplication` on
 * Android, `initializeTogetherlyIos` on iOS) supplies a real implementation via `additionalModules`,
 * reading from that platform's own local, gitignored configuration; see
 * `docs/revenuecat-setup.md` for exactly where each key goes. Returning `null`/blank means "not
 * configured yet" — [RevenueCatConfigurator] decides what that means, never this interface.
 */
fun interface RevenueCatApiKeyProvider {
    fun apiKey(): String?
}
