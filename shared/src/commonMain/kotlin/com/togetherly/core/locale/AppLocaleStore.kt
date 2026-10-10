package com.togetherly.core.locale

/**
 * Persists the user's explicit in-app language choice, independent of the OS system locale —
 * `null` means "follow system". Deliberately not backed by Room/
 * [com.togetherly.domain.family.repository.FamilySettingsRepository]: that repository requires an
 * existing family profile row and is only readable asynchronously via a `Flow`, but the selected
 * language must be readable synchronously, before the first Compose frame, to avoid a flash of the
 * wrong language, and must exist even pre-onboarding. Backed by `SharedPreferences` on Android and
 * `NSUserDefaults` on iOS — see each platform's `platformModule` for the implementation.
 */
interface AppLocaleStore {
    fun getSelectedLanguage(): String?
    fun setSelectedLanguage(code: String?)
}
