package com.togetherly.feature.language.model

/**
 * [displayName] is each language's own name for itself (its endonym) — "Deutsch", not "German" —
 * the same convention every platform's own language picker uses, and deliberately a plain Kotlin
 * string rather than a translated resource: a language's name for itself doesn't change depending
 * on which language the app is currently displayed in. [code] is the ISO 639-1 tag
 * [com.togetherly.core.locale.AppLocaleController.select] expects.
 */
enum class SupportedLanguage(val code: String, val flag: String, val displayName: String) {
    ENGLISH(code = "en", flag = "🇬🇧", displayName = "English"),
    MACEDONIAN(code = "mk", flag = "🇲🇰", displayName = "Македонски"),
    GERMAN(code = "de", flag = "🇩🇪", displayName = "Deutsch"),
    FRENCH(code = "fr", flag = "🇫🇷", displayName = "Français"),
    SPANISH(code = "es", flag = "🇪🇸", displayName = "Español"),
}
