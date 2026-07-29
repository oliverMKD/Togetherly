package com.togetherly.data.purchase

import com.togetherly.core.logging.AppLogger
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.repository.CustomerAttributesRepository
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

private const val TAG = "RevenueCatCustomerAttributesRepository"

internal const val ATTRIBUTE_ONBOARDING_COMPLETED = "onboarding_completed"
internal const val ATTRIBUTE_FIRST_QUEST_COMPLETED = "first_quest_completed"
internal const val ATTRIBUTE_PREFERRED_DURATION_BUCKET = "preferred_duration_bucket"

/**
 * The one implementation of [CustomerAttributesRepository] — every write is gated on the current
 * analytics consent decision (read fresh via [TelemetryConsentRepository.observeConsent] on each
 * call, never cached), matching this attribute list's own "Is it covered by consent?" review
 * question in `docs/revenuecat-posthog-integration.md`. A family that has denied or not yet
 * answered the analytics consent prompt never has any of these three attributes written to
 * RevenueCat at all.
 *
 * Every method is wrapped in [runCatching] — a consent-read failure or a
 * [RevenueCatDataSource.setCustomerAttributes] failure (itself already exception-safe, see that
 * method's own KDoc) must never break onboarding completion or quest completion, only ever get
 * logged.
 */
internal class RevenueCatCustomerAttributesRepository(
    private val dataSource: RevenueCatDataSource,
    private val consentRepository: TelemetryConsentRepository,
    private val logger: AppLogger,
) : CustomerAttributesRepository {

    override suspend fun markOnboardingCompleted() {
        setIfConsented(ATTRIBUTE_ONBOARDING_COMPLETED, "true")
    }

    override suspend fun markFirstQuestCompleted() {
        setIfConsented(ATTRIBUTE_FIRST_QUEST_COMPLETED, "true")
    }

    override suspend fun setPreferredDurationBucket(bucket: DurationBand) {
        setIfConsented(ATTRIBUTE_PREFERRED_DURATION_BUCKET, bucket.name.lowercase())
    }

    private suspend fun setIfConsented(key: String, value: String) {
        try {
            val decision = consentRepository.observeConsent().first().analytics
            if (decision != ConsentDecision.Granted) return
            dataSource.setCustomerAttributes(mapOf(key to value))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.warn(TAG, "Failed to set the '$key' RevenueCat customer attribute", throwable)
        }
    }
}
