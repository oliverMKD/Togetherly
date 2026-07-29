package com.togetherly.core.telemetry

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.paywall.model.PaywallContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs

private val REGISTRY = mapOf(
    QuestCompleted.EVENT_NAME to TelemetryEventSchema(
        eventName = QuestCompleted.EVENT_NAME,
        allowedProperties = setOf(
            TelemetryPropertyNames.QUEST_ID,
            TelemetryPropertyNames.QUEST_CATEGORY,
            TelemetryPropertyNames.DURATION_BUCKET,
            TelemetryPropertyNames.ENERGY_LEVEL,
            TelemetryPropertyNames.ACCESS_REQUIRED,
            TelemetryPropertyNames.USED_TIMER,
            TelemetryPropertyNames.USED_PHONE_DOWN,
        ),
    ),
)

private fun validQuestCompletedEvent() = QuestCompleted(
    questId = QuestId("quest-1"),
    category = QuestCategory.DISCOVER,
    durationBucket = DurationBand.TEN_MINUTES,
    energyLevel = EnergyLevel.MODERATE,
    accessRequired = QuestAccess.Free,
    usedTimer = false,
    usedPhoneDown = false,
)

class TelemetryPrivacyValidatorTest {

    private val validator = TelemetryPrivacyValidator(REGISTRY)

    @Test
    fun registeredEventWithRegisteredPropertiesIsAccepted() {
        val result = validator.validateEvent(validQuestCompletedEvent())

        assertIs<TelemetryValidationResult.Accepted>(result)
    }

    @Test
    fun unregisteredEventNameIsRejected() {
        val result = validator.validateEvent(
            PaywallPresented(PaywallContext.PREMIUM_REROLL, AnalyticsScreen.PAYWALL, "default", setOf(PurchasePackageType.MONTHLY)),
        )

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun eventWithAPropertyNotInItsOwnSchemaIsRejected() {
        // A schema for QuestCompleted that's missing one of the properties the real event always carries.
        val narrowSchema = mapOf(
            QuestCompleted.EVENT_NAME to TelemetryEventSchema(
                eventName = QuestCompleted.EVENT_NAME,
                allowedProperties = setOf(TelemetryPropertyNames.QUEST_CATEGORY),
            ),
        )
        val narrowValidator = TelemetryPrivacyValidator(narrowSchema)

        val result = narrowValidator.validateEvent(validQuestCompletedEvent())

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun forbiddenPropertyKeyIsRejectedEvenIfSchemaWouldAllowIt() {
        val schemaWithForbiddenKey = mapOf(
            "leaky_event" to TelemetryEventSchema(eventName = "leaky_event", allowedProperties = setOf("email")),
        )
        val leakyValidator = TelemetryPrivacyValidator(schemaWithForbiddenKey)
        val leakyEvent = TestAnalyticsEvent(name = "leaky_event", rawProperties = mapOf("email" to AnalyticsValue.Text("a@b.com")))

        val result = leakyValidator.validateEvent(leakyEvent)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun emailLikeTextValueIsRejected() {
        val schema = mapOf("event_with_text" to TelemetryEventSchema("event_with_text", setOf("free_text")))
        val emailValidator = TelemetryPrivacyValidator(schema)
        val event = TestAnalyticsEvent(name = "event_with_text", rawProperties = mapOf("free_text" to AnalyticsValue.Text("parent@example.com")))

        val result = emailValidator.validateEvent(event)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun filePathLikeTextValueIsRejected() {
        val schema = mapOf("event_with_text" to TelemetryEventSchema("event_with_text", setOf("free_text")))
        val pathValidator = TelemetryPrivacyValidator(schema)
        val event = TestAnalyticsEvent(
            name = "event_with_text",
            rawProperties = mapOf("free_text" to AnalyticsValue.Text("/data/user/0/com.togetherly.app/files/media/photo.jpg")),
        )

        val result = pathValidator.validateEvent(event)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun overlyLongFreeTextValueIsRejected() {
        val schema = mapOf("event_with_text" to TelemetryEventSchema("event_with_text", setOf("free_text")))
        val textValidator = TelemetryPrivacyValidator(schema)
        val event = TestAnalyticsEvent(name = "event_with_text", rawProperties = mapOf("free_text" to AnalyticsValue.Text("x".repeat(500))))

        val result = textValidator.validateEvent(event)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun newlineContainingTextValueIsRejected() {
        val schema = mapOf("event_with_text" to TelemetryEventSchema("event_with_text", setOf("free_text")))
        val textValidator = TelemetryPrivacyValidator(schema)
        val event = TestAnalyticsEvent(name = "event_with_text", rawProperties = mapOf("free_text" to AnalyticsValue.Text("line one\nline two")))

        val result = textValidator.validateEvent(event)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun blankPropertyKeyIsRejected() {
        val schema = mapOf("event_with_text" to TelemetryEventSchema("event_with_text", setOf("")))
        val blankKeyValidator = TelemetryPrivacyValidator(schema)
        val event = TestAnalyticsEvent(name = "event_with_text", rawProperties = mapOf("" to AnalyticsValue.Text("value")))

        val result = blankKeyValidator.validateEvent(event)

        assertIs<TelemetryValidationResult.Rejected>(result)
    }

    @Test
    fun defaultValidatorAcceptsThePurchaseStartedEvent() {
        val event = PurchaseStarted(
            context = PaywallContext.PREMIUM_REROLL,
            sourceScreen = AnalyticsScreen.PAYWALL,
            packageType = PurchasePackageType.ANNUAL,
            offeringIdentifier = "default",
            availablePackageTypes = setOf(PurchasePackageType.ANNUAL),
        )
        val result = TelemetryPrivacyValidator.default().validateEvent(event)

        assertIs<TelemetryValidationResult.Accepted>(result)
    }

    @Test
    fun isSafeTextRejectsUrls() {
        val urlValidator = TelemetryPrivacyValidator(emptyMap())

        assertFalse(urlValidator.isSafeText("https://example.com"))
        assertFalse(urlValidator.isSafeText("www.example.com"))
    }
}
