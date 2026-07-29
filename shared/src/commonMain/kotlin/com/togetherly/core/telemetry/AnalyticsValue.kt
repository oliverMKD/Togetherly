package com.togetherly.core.telemetry

/**
 * The only shapes an [AnalyticsEvent] property may hold — a closed hierarchy so a domain model, a
 * platform object, or an arbitrary `Any` can never reach a telemetry provider by construction, not
 * by convention. There is deliberately no `List`/`Map`/nested-object variant: every property this
 * app ever sends is a single scalar, matching the "broad, predefined category" shape
 * `docs/telemetry.md` documents (a bucket, a count, a type name) — never a structure rich enough to
 * fingerprint a family.
 */
sealed interface AnalyticsValue {
    data class Text(val value: String) : AnalyticsValue
    data class Number(val value: Long) : AnalyticsValue
    data class Decimal(val value: Double) : AnalyticsValue
    data class BooleanValue(val value: Boolean) : AnalyticsValue
}
