package com.togetherly.core.locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Koin singleton bridging the Language screen (which selects a language) to
 * [com.togetherly.app.application.App] (which must recompose its whole tree when the language
 * changes) — the same bridge-two-screens-without-nav-args shape as
 * [com.togetherly.navigation.shell.RequestedTabStore]. [select] both persists the choice (via
 * [store], so it survives restart) and applies it to the current process via [applyPlatformLocale] —
 * see that function's own KDoc for why Android takes effect immediately while iOS needs a restart
 * (surfaced to the user via [requiresRestartForLocaleChange]).
 */
class AppLocaleController(private val store: AppLocaleStore) {
    private val _language = MutableStateFlow(store.getSelectedLanguage())
    val language: StateFlow<String?> = _language.asStateFlow()

    init {
        applyPlatformLocale(_language.value)
    }

    fun select(code: String?) {
        store.setSelectedLanguage(code)
        _language.value = code
        applyPlatformLocale(code)
    }
}

/**
 * Applies [code] (an ISO 639-1 language tag, e.g. `"mk"`, or `null` for "follow system") to the
 * current process.
 *
 * Android: [java.util.Locale.setDefault] — Compose Resources' `stringResource()` resolves against
 * the JVM default locale on Android, so this takes effect on the very next recomposition (see
 * [com.togetherly.app.application.App]'s `key(language)` wrapper).
 *
 * iOS: there is no reliable, public way to hot-swap Compose Multiplatform's resource locale at
 * runtime — `LocalComposeEnvironment`/`ResourceEnvironment` are `internal` to the
 * `components-resources` library (confirmed by inspecting the actual 1.11.1 dependency), and
 * JetBrains' own tracker is titled "Lack of reliable localization switching on Web and iOS in
 * Compose Multiplatform" (CMP-8376; the public-API request is
 * https://github.com/JetBrains/compose-multiplatform/issues/4197, never resolved). The iOS actual
 * instead writes the standard `AppleLanguages` preference iOS itself reads at process start, and
 * [requiresRestartForLocaleChange] tells the Language screen to prompt the user to restart the app.
 */
internal expect fun applyPlatformLocale(code: String?)

/** `false` on Android (switches live), `true` on iOS (see [applyPlatformLocale]'s own KDoc). */
internal expect val requiresRestartForLocaleChange: Boolean
