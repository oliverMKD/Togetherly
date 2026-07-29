package com.togetherly.core.telemetry

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.logging.AppLogger
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

private const val TAG = "DuplicateSignalDetector"
private val DEFAULT_THRESHOLD = 500.milliseconds

/**
 * Development-time-only heuristic (Step 14.6) — warns, never suppresses, when the same
 * [operationId] fires again within [threshold] of its own last occurrence. This is a strong signal
 * of a Compose recomposition re-triggering a capture call, or a reactive collector re-emitting the
 * same handled error on every retry — never of genuine repeated user activity (a family completing
 * the same quest category twice in one day is completely normal and must never be flagged, which is
 * exactly why this only compares a call against *its own* last occurrence, never against any
 * broader history). [check] never alters what its caller does next — both
 * [com.togetherly.data.telemetry.DebugRecordingProductAnalytics] and
 * [com.togetherly.data.telemetry.DebugRecordingOperationalDiagnostics] still forward every call to
 * their real delegate regardless of what this logs.
 *
 * The per-[operationId] map is never pruned, but this is bounded in practice: an "operation id" is
 * always drawn from a small, closed vocabulary (a registered [AnalyticsEvent.name], or an exception
 * class name plus a `DiagnosticContext` `operation`/`feature` tag) — never a value with unbounded
 * cardinality like a user-entered string or a database row id.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class DuplicateSignalDetector(
    private val clock: AppClock,
    private val logger: AppLogger,
    private val threshold: Duration = DEFAULT_THRESHOLD,
) {
    private val lastSeenAt = AtomicReference<Map<String, Instant>>(emptyMap())

    fun check(operationId: String) {
        val now = clock.now()
        while (true) {
            val current = lastSeenAt.load()
            val previous = current[operationId]
            val updated = current + (operationId to now)
            if (lastSeenAt.compareAndSet(current, updated)) {
                if (previous != null && (now - previous) < threshold) {
                    val elapsedMillis = (now - previous).inWholeMilliseconds
                    logger.warn(
                        TAG,
                        "Possible duplicate: '$operationId' fired again ${elapsedMillis}ms after its last occurrence — " +
                            "check for a recomposition or a repeated collector error.",
                    )
                }
                return
            }
        }
    }
}
