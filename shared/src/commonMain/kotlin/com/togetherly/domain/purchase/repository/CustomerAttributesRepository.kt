package com.togetherly.domain.purchase.repository

import com.togetherly.domain.family.DurationBand

/**
 * Provider-neutral: nothing here mentions RevenueCat, matching [EntitlementRepository]'s own
 * discipline. Deliberately narrow — exactly the three low-risk, predefined, paywall-targeting
 * signals `docs/revenuecat-posthog-integration.md` reviews and justifies against its own
 * required/predefined/non-sensitive/low-cardinality/consent/needed-for-targeting checklist. This is
 * not a generic "set any attribute" escape hatch; a new attribute needs its own reviewed method
 * added here, never a passthrough key/value call from feature code.
 *
 * Every method is a no-op unless analytics consent is currently
 * [com.togetherly.domain.telemetry.ConsentDecision.Granted] (see
 * [com.togetherly.data.purchase.RevenueCatCustomerAttributesRepository]'s own KDoc) and never
 * throws — a RevenueCat SDK failure here must never break onboarding or quest completion.
 */
interface CustomerAttributesRepository {

    /** Fired once, at the moment [com.togetherly.domain.family.usecase.CreateFamilyProfile] first succeeds. */
    suspend fun markOnboardingCompleted()

    /** Idempotent — safe to call after every quest completion, not only the first. RevenueCat keeps only the latest value, so "has this family ever completed a quest" needs no separate first-time detection. */
    suspend fun markFirstQuestCompleted()

    /** Set once, from the family's onboarding duration selection — never re-synced from later preference edits (see this repository's own KDoc for why that's a deliberate, documented scope boundary). */
    suspend fun setPreferredDurationBucket(bucket: DurationBand)
}
