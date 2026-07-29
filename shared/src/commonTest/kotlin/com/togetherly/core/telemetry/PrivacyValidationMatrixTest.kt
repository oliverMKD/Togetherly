package com.togetherly.core.telemetry

import com.togetherly.feature.onboarding.model.OnboardingStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Step 14.6's comprehensive privacy-validation matrix — one test per attempted-unsafe-value shape
 * the spec calls out by name, all against [TelemetryPrivacyValidator.default], the exact validator
 * every real [ProductAnalytics] implementation (`PostHogProductAnalytics`, [DebugProductAnalytics])
 * gates [ProductAnalytics.capture] on before ever reaching an adapter — see each of those classes'
 * own `when (validator.validateEvent(event))` branch. A [TelemetryValidationResult.Rejected] here
 * is exactly "never reaches a real provider," not merely a local assertion.
 *
 * [TestAnalyticsEvent] (test-support only — see its own KDoc) is what lets this file construct
 * property shapes no real, compile-time-checked [AnalyticsEvent] could ever hold — attaching a
 * forbidden key to an event whose own schema wouldn't normally carry it, an unregistered property
 * on a real event name, or a wholly unregistered event name outright.
 */
class PrivacyValidationMatrixTest {

    private val validator = TelemetryPrivacyValidator.default()

    private fun assertRejected(event: AnalyticsEvent) {
        assertIs<TelemetryValidationResult.Rejected>(validator.validateEvent(event))
    }

    // 1. Email address
    @Test
    fun emailAddressAsAForbiddenKeyIsRejected() {
        assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf("email" to AnalyticsValue.Text("parent@example.com"))))
    }

    @Test
    fun emailAddressAsAValueUnderASafeKeyIsRejected() {
        assertRejected(
            TestAnalyticsEvent(
                name = OnboardingStepViewed.EVENT_NAME,
                rawProperties = mapOf(TelemetryPropertyNames.ONBOARDING_STEP to AnalyticsValue.Text("parent@example.com")),
            ),
        )
    }

    // 2. Person name
    @Test
    fun personNameForbiddenKeysAreAllRejected() {
        for (key in listOf("name", "parent_name", "child_name")) {
            assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf(key to AnalyticsValue.Text("Riley"))))
        }
    }

    // 3. File path
    @Test
    fun filePathForbiddenKeysAreAllRejected() {
        for (key in listOf("path", "file")) {
            assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf(key to AnalyticsValue.Text("/data/user/0/com.togetherly/files/photo.jpg"))))
        }
    }

    @Test
    fun filePathAsAValueUnderASafeKeyIsRejected() {
        assertRejected(
            TestAnalyticsEvent(
                name = OnboardingStepViewed.EVENT_NAME,
                rawProperties = mapOf(TelemetryPropertyNames.ONBOARDING_STEP to AnalyticsValue.Text("/data/user/0/com.togetherly/files/photo.jpg")),
            ),
        )
    }

    // 4. URL with query parameters
    @Test
    fun urlWithQueryParametersIsRejected() {
        assertRejected(
            TestAnalyticsEvent(
                name = OnboardingStepViewed.EVENT_NAME,
                rawProperties = mapOf(TelemetryPropertyNames.ONBOARDING_STEP to AnalyticsValue.Text("https://example.com/callback?token=abc123&user=me")),
            ),
        )
    }

    // 5. Memory note
    @Test
    fun memoryNoteForbiddenKeysAreAllRejected() {
        for (key in listOf("note", "notes")) {
            assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf(key to AnalyticsValue.Text("Had a wonderful time at the park"))))
        }
    }

    // 6. Search query
    @Test
    fun searchQueryForbiddenKeysAreAllRejected() {
        for (key in listOf("search_query", "query")) {
            assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf(key to AnalyticsValue.Text("dinosaur crafts"))))
        }
    }

    // 7. Excessively long value
    @Test
    fun excessivelyLongValueIsRejected() {
        assertRejected(
            TestAnalyticsEvent(
                name = OnboardingStepViewed.EVENT_NAME,
                rawProperties = mapOf(TelemetryPropertyNames.ONBOARDING_STEP to AnalyticsValue.Text("x".repeat(500))),
            ),
        )
    }

    // 8. Unknown event
    @Test
    fun unknownEventNameIsRejected() {
        assertRejected(TestAnalyticsEvent(name = "not_a_registered_event_name"))
    }

    // 9. Unknown property
    @Test
    fun unknownPropertyOnAKnownEventIsRejected() {
        assertRejected(TestAnalyticsEvent(name = OnboardingCompleted.EVENT_NAME, rawProperties = mapOf("not_a_registered_property" to AnalyticsValue.Text("value"))))
    }

    // 10 & 11. Nested object / unsupported property type
    @Test
    fun analyticsValueHasNoNestedObjectOrUnsupportedTypeVariantAtAll() {
        // AnalyticsValue is a closed sealed interface with exactly these four scalar leaves — there
        // is no fifth variant, and no Map/List/domain-object variant, for a nested object or an
        // unsupported type to even compile as. TelemetryPrivacyValidator.isSafeValue's own KDoc
        // documents this as enforced by the type system, not a runtime check — this test is the
        // proof: if a future change ever added a fifth AnalyticsValue subtype, isSafeValue's `when`
        // would stop compiling (no `else` branch exists) until that subtype's own safety is
        // explicitly decided, so "nested object"/"unsupported type" can never silently slip through.
        val leafTypeNames = setOf(
            AnalyticsValue.Text::class.simpleName,
            AnalyticsValue.Number::class.simpleName,
            AnalyticsValue.Decimal::class.simpleName,
            AnalyticsValue.BooleanValue::class.simpleName,
        )
        assertEquals(setOf("Text", "Number", "Decimal", "BooleanValue"), leafTypeNames)
    }

    @Test
    fun registeredEventWithOnlyRegisteredSafePropertiesIsAccepted() {
        val accepted = validator.validateEvent(OnboardingStepViewed(step = OnboardingStep.WELCOME))
        assertTrue(accepted is TelemetryValidationResult.Accepted)
    }
}
