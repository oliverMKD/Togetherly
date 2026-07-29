package com.togetherly.domain.telemetry.repository

import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for a family's telemetry consent — device-level state, not tied to
 * [com.togetherly.domain.family.FamilyProfile]'s own lifecycle (it must be readable before
 * onboarding, and survives independently of whether a profile currently exists). Deliberately not
 * part of [com.togetherly.domain.family.PrivacyPreferences]: that type's own
 * `diagnosticsEnabled` field was an inert Step 13.1 placeholder with nothing reading it (see its
 * own KDoc) — a plain `Boolean` also cannot represent [ConsentDecision.NotAsked] distinctly from
 * [ConsentDecision.Denied], which this repository's whole contract depends on.
 *
 * No method here returns `DataResult` — this is local, always-available key-value state (the same
 * simplification [com.togetherly.data.purchase.EntitlementCache]'s own `load`/`save` already make
 * for the same reason: a local cache read/write has no caller-meaningful failure mode worth
 * surfacing). An implementation must still never let an underlying storage exception propagate to
 * a caller; see [com.togetherly.data.telemetry.DefaultTelemetryConsentRepository]'s own KDoc.
 *
 * [updateAnalyticsConsent]/[updateDiagnosticsConsent] are independent — updating one never touches
 * the other. [resetConsent] is a distinct operation from setting both decisions to
 * [ConsentDecision.NotAsked] one at a time: it also clears whatever locally-persisted telemetry
 * state this repository owns (see `docs/telemetry.md`), and exists specifically for
 * [com.togetherly.domain.localdata.usecase.DeleteAllLocalData] to call as one atomic "erase what I
 * own" step, the same way [com.togetherly.data.purchase.EntitlementCache.clear] serves
 * [com.togetherly.domain.purchase.repository.EntitlementRepository.clearCache].
 */
interface TelemetryConsentRepository {

    fun observeConsent(): Flow<TelemetryConsent>

    suspend fun updateAnalyticsConsent(decision: ConsentDecision)

    suspend fun updateDiagnosticsConsent(decision: ConsentDecision)

    suspend fun resetConsent()
}
