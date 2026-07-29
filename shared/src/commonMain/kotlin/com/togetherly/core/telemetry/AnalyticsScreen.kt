package com.togetherly.core.telemetry

/**
 * A closed vocabulary for [ProductAnalytics.screen], the same "no raw strings" discipline
 * [AnalyticsEvent] applies to events — extend this enum as new screens need tracking rather than
 * passing a screen name/route as a string.
 *
 * Only property-free "viewed" moments belong here — [ProductAnalytics.screen] carries no
 * properties (see that method's own signature). A screen-entry moment that needs a property (Quest
 * Detail's `source`, the paywall's `paywall_context`, Pack Details' `access_required`) is a typed
 * [AnalyticsEvent] ([QuestViewed]/[PaywallPresented]/[PackViewed]) instead, captured via
 * [ProductAnalytics.capture] — see `docs/analytics-event-taxonomy.md` for which screens use which
 * mechanism and why.
 */
enum class AnalyticsScreen {
    TODAY,
    EXPLORE,
    JOURNEY,
    FAMILY,
    ONBOARDING,
    QUEST_MODE,
    COMPLETION,
    REMINDER,
    PAYWALL,
    FAMILY_PLUS_MANAGEMENT,
}
