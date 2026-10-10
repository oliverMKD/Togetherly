package com.togetherly.core.locale

import java.util.Locale

/**
 * A plain (not `by lazy`) top-level property so it's captured as soon as this file's class is
 * loaded — the first reference to [applyPlatformLocale] itself, which always happens from
 * [AppLocaleController]'s own `init` block before any user-triggered override. `by lazy` would be
 * wrong here: if the very first call ever made were `applyPlatformLocale("mk")` (a persisted
 * non-null choice from a previous session), the `?: systemDefaultLocale` branch would never
 * evaluate on that call, so a lazy capture would stay uninitialized until the user later picked
 * "System default" — by which point [Locale.getDefault] would return the already-overridden value,
 * not the true original system locale.
 */
private val systemDefaultLocale: Locale = Locale.getDefault()

internal actual fun applyPlatformLocale(code: String?) {
    Locale.setDefault(code?.let { Locale.forLanguageTag(it) } ?: systemDefaultLocale)
}

internal actual val requiresRestartForLocaleChange: Boolean = false
