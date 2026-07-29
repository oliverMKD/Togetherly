package com.togetherly.core.telemetry

/**
 * The literal property-name strings this app is allowed to send, shared between each
 * [AnalyticsEvent]'s own [AnalyticsEvent.properties] and [TelemetryEventRegistry]'s schema for
 * that event — one constant per name, never a duplicated string literal on both sides that could
 * silently drift apart. Every name here is a broad, predefined category (a bucket, a count, a type
 * name, an id) — see `docs/analytics-event-taxonomy.md` for the full per-event property list and
 * `docs/telemetry.md`'s privacy principles for what may and may not be sent. `access_state`/
 * `app_version`/`build_number`/`platform`/`environment` are deliberately not here — those five are
 * [PostHogCommonProperties][com.togetherly.data.telemetry.PostHogCommonProperties] common
 * properties, attached to every event outside any single event's own schema.
 */
internal object TelemetryPropertyNames {
    const val QUEST_CATEGORY = "quest_category"
    const val DURATION_BUCKET = "duration_bucket"
    const val ENERGY_LEVEL = "energy_level"
    const val ACCESS_REQUIRED = "access_required"
    const val PAYWALL_CONTEXT = "paywall_context"
    const val PACKAGE_TYPE = "package_type"
    const val QUEST_ID = "quest_id"
    const val PACK_ID = "pack_id"
    const val SOURCE = "source"
    const val IS_SAVED = "is_saved"
    const val USED_TIMER = "used_timer"
    const val USED_PHONE_DOWN = "used_phone_down"
    const val HAS_NOTE = "has_note"
    const val HAS_PHOTO = "has_photo"
    const val HAS_VOICE = "has_voice"
    const val LOCATION = "location"
    const val ACCESS_FILTER = "access_filter"
    const val RESULT = "result"
    const val REMINDER_ENABLED = "reminder_enabled"
    const val SELECTED_DAY_COUNT = "selected_day_count"
    const val ONBOARDING_STEP = "onboarding_step"
    const val SOURCE_SCREEN = "source_screen"
    const val OFFERING_IDENTIFIER = "offering_identifier"
    const val AVAILABLE_PACKAGE_TYPES = "available_package_types"
}
