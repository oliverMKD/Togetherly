package com.togetherly.data.purchase

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import com.togetherly.core.logging.AppLogger
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.domain.purchase.PurchaseStartupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LOG_TAG = "RevenueCat"

/**
 * The one place [Purchases.configure] is ever called — exactly once, synchronously, from
 * [com.togetherly.app.di.initKoin] before `startKoin`'s modules can hand anything to a caller (see
 * that function's own KDoc), never from a Composable and never repeated on recomposition.
 *
 * No `appUserId` is ever passed to [Purchases.configure]: Togetherly has no account/login system,
 * so every family is identified by RevenueCat's own auto-generated anonymous app user ID, which
 * the native SDK persists locally and reuses across launches on its own — this class must never
 * generate or pass a custom ID, and must never call `logIn()`/`logOut()`. If Togetherly adds
 * accounts later, migrating means calling `Purchases.sharedInstance.logIn(accountId)` once at the
 * moment an account is created/signed into (RevenueCat then merges the anonymous history into the
 * new identity) — nothing here needs to change to support that; it is a future call site, not a
 * change to this configuration step.
 *
 * A missing key throws in debug builds ("fail clearly") but never in release — a release build
 * with a misconfigured key logs the problem and falls back to [PurchaseStartupState.NotConfigured],
 * keeping the app fully usable in free mode rather than crashing a family's install over a
 * packaging mistake. The key itself is never logged, complete or partial.
 *
 * [diagnostics] reports a real configuration failure (Step 14.5) — never the missing-key case
 * above, which is expected/handled, not unexpected. **Known timing limitation**: [configure] runs
 * synchronously during [com.togetherly.app.di.initKoin], strictly before
 * [com.togetherly.core.telemetry.TelemetryCoordinator.start] has had a chance to apply the
 * family's own diagnostics consent decision — [OperationalDiagnostics] starts every process
 * forced-disabled (see that coordinator's own KDoc), so a failure here can only ever reach a real
 * provider on a later, warm re-configuration, never on the very first cold-start failure. This is
 * a deliberate consequence of "never capture before consent, no exceptions," not a bug; see
 * `docs/sentry-setup.md`.
 */
class RevenueCatConfigurator(
    private val apiKeyProvider: RevenueCatApiKeyProvider,
    private val logger: AppLogger,
    private val diagnostics: OperationalDiagnostics,
) {
    private val _state = MutableStateFlow<PurchaseStartupState>(PurchaseStartupState.NotConfigured)
    val state: StateFlow<PurchaseStartupState> = _state.asStateFlow()

    fun configure(debug: Boolean) {
        _state.value = PurchaseStartupState.Initializing

        val apiKey = apiKeyProvider.apiKey()
        if (apiKey.isNullOrBlank()) {
            check(!debug) {
                "RevenueCat public SDK key is missing. See docs/revenuecat-setup.md for where to place it."
            }
            logger.warn(LOG_TAG, "No RevenueCat public SDK key found — continuing in free mode.")
            _state.value = PurchaseStartupState.NotConfigured
            return
        }

        _state.value = try {
            Purchases.logLevel = if (debug) LogLevel.DEBUG else LogLevel.ERROR
            Purchases.configure(apiKey = apiKey) { }
            logger.debug(LOG_TAG, "Configured with an anonymous app user id.")
            PurchaseStartupState.Ready
        } catch (throwable: Throwable) {
            logger.error(LOG_TAG, "Configuration failed.", throwable)
            diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "purchase", "operation" to "revenuecat_initialization")))
            PurchaseStartupState.Failed
        }
    }
}
