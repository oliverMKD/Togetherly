package com.togetherly.core.telemetry

import com.togetherly.core.datetime.AppClock
import kotlin.time.Instant

private const val MAX_HISTORY = 20

/** A sanitized analytics event that already passed [TelemetryPrivacyValidator] — never the event's own raw, pre-validation properties. */
data class RecordedAnalyticsEvent(
    val name: String,
    val properties: Map<String, AnalyticsValue>,
    val recordedAt: Instant,
)

/** A breadcrumb message that already passed [DiagnosticSanitizer.sanitizeBreadcrumbMessage] — never a dropped/unsafe one. */
data class RecordedBreadcrumb(
    val message: String,
    val category: String?,
    val recordedAt: Instant,
)

/** Only the exception's class name — see [OperationalDiagnostics.captureHandledException]'s own KDoc on why a message is never guaranteed safe to display. */
data class RecordedProviderError(
    val category: String,
    val recordedAt: Instant,
)

/**
 * Debug-only, in-memory, bounded history of what actually passed validation/sanitization and would
 * be sent to a real provider — the data source behind Step 14.6's debug telemetry screen. Never
 * written to outside [com.togetherly.data.telemetry.DebugRecordingProductAnalytics]/
 * [com.togetherly.data.telemetry.DebugRecordingOperationalDiagnostics], which are themselves only
 * ever constructed in a debug build (see `app/di/TelemetryModule.kt`) — a release build's
 * `ProductAnalytics`/`OperationalDiagnostics` bindings never touch this class at all, so it always
 * stays empty in production even though the binding itself is harmless to leave in the graph.
 *
 * Each of the three histories is independently bounded to [MAX_HISTORY] and backed by
 * [BoundedHistory]'s compare-and-swap loop, so concurrent recordings from different threads (a
 * real provider's own SDK callback thread, a feature ViewModel's coroutine, a debug screen reading
 * a snapshot) can never corrupt or lose an entry.
 */
class TelemetryDebugRecorder(private val clock: AppClock) {

    private val events = BoundedHistory<RecordedAnalyticsEvent>(MAX_HISTORY)
    private val breadcrumbs = BoundedHistory<RecordedBreadcrumb>(MAX_HISTORY)
    private val providerErrors = BoundedHistory<RecordedProviderError>(MAX_HISTORY)

    fun recordEvent(name: String, properties: Map<String, AnalyticsValue>) {
        events.record(RecordedAnalyticsEvent(name, properties, clock.now()))
    }

    fun recordBreadcrumb(message: String, category: String?) {
        breadcrumbs.record(RecordedBreadcrumb(message, category, clock.now()))
    }

    fun recordProviderError(category: String) {
        providerErrors.record(RecordedProviderError(category, clock.now()))
    }

    fun recentEvents(): List<RecordedAnalyticsEvent> = events.snapshot()
    fun recentBreadcrumbs(): List<RecordedBreadcrumb> = breadcrumbs.snapshot()
    fun recentProviderErrors(): List<RecordedProviderError> = providerErrors.snapshot()

    /** "Clear local debug history" — never touches consent, provider state, or anything a real provider itself holds. */
    fun clear() {
        events.clear()
        breadcrumbs.clear()
        providerErrors.clear()
    }
}
