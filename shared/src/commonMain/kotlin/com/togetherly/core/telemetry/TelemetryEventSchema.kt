package com.togetherly.core.telemetry

/** One [AnalyticsEvent]'s own allowlist — its registered name plus the exact set of property names it may carry. Anything not listed here is rejected by [TelemetryPrivacyValidator], never silently passed through. */
data class TelemetryEventSchema(
    val eventName: String,
    val allowedProperties: Set<String>,
)

/**
 * Every [AnalyticsEvent] this app is allowed to actually send, keyed by [AnalyticsEvent.name].
 * [TelemetryPrivacyValidator.default] is the one production validator built from this — a new
 * event must be added here (with its own explicit property allowlist) before
 * [ProductAnalytics.capture] will ever accept it; there is no implicit "allow whatever the event
 * class itself claims." See `docs/analytics-event-taxonomy.md` for the full per-event
 * trigger/purpose/funnel documentation this schema list only encodes the mechanical shape of.
 */
internal object TelemetryEventRegistry {
    private val questProperties = setOf(
        TelemetryPropertyNames.QUEST_CATEGORY,
        TelemetryPropertyNames.DURATION_BUCKET,
        TelemetryPropertyNames.ENERGY_LEVEL,
        TelemetryPropertyNames.ACCESS_REQUIRED,
    )

    private val paywallContextProperties = setOf(
        TelemetryPropertyNames.PAYWALL_CONTEXT,
        TelemetryPropertyNames.SOURCE_SCREEN,
        TelemetryPropertyNames.OFFERING_IDENTIFIER,
        TelemetryPropertyNames.AVAILABLE_PACKAGE_TYPES,
    )

    private val registered: List<TelemetryEventSchema> = listOf(
        // Onboarding
        TelemetryEventSchema(OnboardingStepViewed.EVENT_NAME, setOf(TelemetryPropertyNames.ONBOARDING_STEP)),
        TelemetryEventSchema(OnboardingCompleted.EVENT_NAME, emptySet()),

        // Today
        TelemetryEventSchema(DailyQuestRevealed.EVENT_NAME, questProperties),
        TelemetryEventSchema(DailyQuestRerolled.EVENT_NAME, questProperties),
        TelemetryEventSchema(DailyQuestRerollBlocked.EVENT_NAME, emptySet()),

        // Quest detail / lifecycle
        TelemetryEventSchema(QuestViewed.EVENT_NAME, questProperties + TelemetryPropertyNames.QUEST_ID + TelemetryPropertyNames.SOURCE),
        TelemetryEventSchema(QuestSaved.EVENT_NAME, setOf(TelemetryPropertyNames.QUEST_ID, TelemetryPropertyNames.IS_SAVED)),
        TelemetryEventSchema(QuestStarted.EVENT_NAME, questProperties + TelemetryPropertyNames.QUEST_ID + TelemetryPropertyNames.SOURCE),
        TelemetryEventSchema(
            QuestCompleted.EVENT_NAME,
            questProperties + TelemetryPropertyNames.QUEST_ID + TelemetryPropertyNames.USED_TIMER + TelemetryPropertyNames.USED_PHONE_DOWN,
        ),
        TelemetryEventSchema(
            QuestAbandoned.EVENT_NAME,
            setOf(TelemetryPropertyNames.QUEST_ID, TelemetryPropertyNames.USED_PHONE_DOWN),
        ),

        // Memories / Journey
        TelemetryEventSchema(
            MemorySaved.EVENT_NAME,
            setOf(TelemetryPropertyNames.HAS_NOTE, TelemetryPropertyNames.HAS_PHOTO, TelemetryPropertyNames.HAS_VOICE),
        ),
        TelemetryEventSchema(MemoryDeleted.EVENT_NAME, emptySet()),
        TelemetryEventSchema(MemoryOpened.EVENT_NAME, emptySet()),

        // Explore
        TelemetryEventSchema(ExploreSearched.EVENT_NAME, emptySet()),
        TelemetryEventSchema(
            ExploreFiltered.EVENT_NAME,
            setOf(
                TelemetryPropertyNames.QUEST_CATEGORY,
                TelemetryPropertyNames.DURATION_BUCKET,
                TelemetryPropertyNames.ENERGY_LEVEL,
                TelemetryPropertyNames.LOCATION,
                TelemetryPropertyNames.ACCESS_FILTER,
            ),
        ),
        TelemetryEventSchema(PackViewed.EVENT_NAME, setOf(TelemetryPropertyNames.PACK_ID, TelemetryPropertyNames.ACCESS_REQUIRED)),
        TelemetryEventSchema(
            PremiumContentViewed.EVENT_NAME,
            setOf(TelemetryPropertyNames.QUEST_ID, TelemetryPropertyNames.PACK_ID),
        ),
        TelemetryEventSchema(
            PremiumContentUnlocked.EVENT_NAME,
            setOf(TelemetryPropertyNames.QUEST_ID, TelemetryPropertyNames.PACK_ID),
        ),

        // Paywall / purchases
        TelemetryEventSchema(PaywallPresented.EVENT_NAME, paywallContextProperties),
        TelemetryEventSchema(PaywallDismissed.EVENT_NAME, paywallContextProperties),
        TelemetryEventSchema(PurchaseStarted.EVENT_NAME, paywallContextProperties + TelemetryPropertyNames.PACKAGE_TYPE),
        TelemetryEventSchema(
            PurchaseOutcome.EVENT_NAME,
            paywallContextProperties + TelemetryPropertyNames.PACKAGE_TYPE + TelemetryPropertyNames.RESULT,
        ),
        TelemetryEventSchema(RestoreStarted.EVENT_NAME, setOf(TelemetryPropertyNames.SOURCE_SCREEN)),
        TelemetryEventSchema(
            RestoreOutcome.EVENT_NAME,
            setOf(TelemetryPropertyNames.SOURCE_SCREEN, TelemetryPropertyNames.RESULT),
        ),

        // Family settings
        TelemetryEventSchema(
            ReminderPreferenceChanged.EVENT_NAME,
            setOf(TelemetryPropertyNames.REMINDER_ENABLED, TelemetryPropertyNames.SELECTED_DAY_COUNT),
        ),

        // Debug
        TelemetryEventSchema(DebugTestEvent.EVENT_NAME, emptySet()),
    )

    /**
     * Built with an explicit duplicate check rather than a plain `associateBy` — that would
     * silently keep only the last of two schemas sharing an [TelemetryEventSchema.eventName],
     * quietly dropping the other's property allowlist. Failing fast here means a copy-pasted
     * [TelemetryEventSchema.EVENT_NAME] mistake crashes the moment this object is first touched,
     * not "the second registration's properties are inexplicably rejected."
     */
    val schemas: Map<String, TelemetryEventSchema> = registered
        .also { list ->
            val duplicates = list.groupBy { it.eventName }.filterValues { it.size > 1 }.keys
            require(duplicates.isEmpty()) { "Duplicate TelemetryEventSchema.eventName(s): $duplicates" }
        }
        .associateBy { it.eventName }
}
