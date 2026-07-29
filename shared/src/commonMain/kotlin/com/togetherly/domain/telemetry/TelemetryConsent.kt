package com.togetherly.domain.telemetry

/**
 * A family's choice for one telemetry category — never conflated with "off"/`false`.
 * [NotAsked] is the only default state (see [TelemetryConsent]'s own KDoc) and must never be
 * reinterpreted as [Denied] by any reader; the two mean different things ("we haven't asked yet"
 * versus "the family said no") and drive different UI/behavior in a future consent prompt.
 */
enum class ConsentDecision {
    NotAsked,
    Granted,
    Denied,
}

/**
 * Analytics and diagnostics are independent choices — granting one must never imply or require
 * granting the other. Both default to [ConsentDecision.NotAsked] (see
 * [com.togetherly.domain.telemetry.repository.TelemetryConsentRepository]'s own KDoc for exactly
 * where that default is enforced), which this app also treats as "disabled" for collection
 * purposes: nothing is ever collected unless a decision is explicitly [ConsentDecision.Granted].
 */
data class TelemetryConsent(
    val analytics: ConsentDecision,
    val diagnostics: ConsentDecision,
) {
    companion object {
        fun default(): TelemetryConsent = TelemetryConsent(
            analytics = ConsentDecision.NotAsked,
            diagnostics = ConsentDecision.NotAsked,
        )
    }
}
