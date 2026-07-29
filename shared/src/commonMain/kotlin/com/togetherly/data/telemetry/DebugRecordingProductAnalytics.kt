package com.togetherly.data.telemetry

import com.togetherly.core.telemetry.AnalyticsEvent
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryDebugRecorder
import com.togetherly.core.telemetry.TelemetryPrivacyValidator
import com.togetherly.core.telemetry.TelemetryValidationResult

/**
 * Debug-build-only decorator around the real [delegate] (see `app/di/TelemetryModule.kt` — never
 * constructed at all on a release build). Every call is forwarded to [delegate] completely
 * unchanged first — this class never alters what a real provider actually receives or whether it
 * receives it, only *additionally* records what already passed validation into [recorder] for
 * Step 14.6's debug telemetry screen, and flags a same-event repeat via [duplicateDetector] as a
 * development-time signal only.
 *
 * [collectionEnabled] gates [recorder] the same way [DebugProductAnalytics]'s own `collectionEnabled`
 * gates its local-only debug logging — "no analytics event leaves the device before analytics
 * consent" applies here too, even though this history never leaves the device either: nothing is
 * recorded before [setCollectionEnabled] `true` has been called by [com.togetherly.core.telemetry.TelemetryCoordinator],
 * and everything already recorded stays visible after a later revocation (this class has no
 * `reset`-triggered clear of its own — only [DebugTelemetryAction.ClearHistoryClicked][com.togetherly.feature.debug.model.DebugTelemetryAction.ClearHistoryClicked]
 * clears it, matching "Clear local debug history" being its own explicit action in the spec, not
 * an implicit side effect of consent revocation). [duplicateDetector] is never gated the same way —
 * it only logs a warning about a call *pattern* (never event content) via [com.togetherly.core.logging.AppLogger],
 * a signal just as useful before consent as after.
 *
 * [validator] runs a second time here, independent of whatever [delegate] itself does internally —
 * this class has no access to a real provider's own private validation result, and re-running the
 * same, side-effect-free, stateless validator is cheap and exactly reproduces what [delegate] would
 * have decided.
 */
internal class DebugRecordingProductAnalytics(
    private val delegate: ProductAnalytics,
    private val recorder: TelemetryDebugRecorder,
    private val duplicateDetector: DuplicateSignalDetector,
    private val validator: TelemetryPrivacyValidator = TelemetryPrivacyValidator.default(),
) : ProductAnalytics {

    private var collectionEnabled = false

    override fun capture(event: AnalyticsEvent) {
        duplicateDetector.check(event.name)
        if (collectionEnabled) {
            when (val result = validator.validateEvent(event)) {
                is TelemetryValidationResult.Accepted -> recorder.recordEvent(event.name, result.sanitizedProperties)
                is TelemetryValidationResult.Rejected -> Unit
            }
        }
        delegate.capture(event)
    }

    override fun screen(screen: AnalyticsScreen) = delegate.screen(screen)
    override fun flush() = delegate.flush()

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        delegate.setCollectionEnabled(enabled)
    }

    override fun reset() = delegate.reset()
    override fun configurationStatus(): ProviderConfigurationStatus = delegate.configurationStatus()
}
