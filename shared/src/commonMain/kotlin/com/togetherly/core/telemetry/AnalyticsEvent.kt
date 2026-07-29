package com.togetherly.core.telemetry

import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.paywall.model.PaywallContext
import com.togetherly.feature.questdetail.model.QuestOpenSource

/**
 * A closed, centrally-declared vocabulary of everything Togetherly may ever send — feature code
 * calls [ProductAnalytics.capture] with one of these, never a raw event-name string. Every
 * subtype must live in this file (Kotlin's own sealed-hierarchy rule); that is deliberate, not a
 * limitation — it keeps every event this app can possibly emit reviewable in one place, the same
 * "typed, not stringly-typed" discipline [com.togetherly.core.error.AppError] already applies to
 * error handling.
 *
 * [properties] never returns a domain model, a platform object, or free text — every value is
 * translated into an [AnalyticsValue] scalar (a bucket, a count, a type name, a catalogue id) at
 * the point each event is constructed, so nothing richer than that can ever reach
 * [TelemetryPrivacyValidator] in the first place. See `docs/analytics-event-taxonomy.md` for every
 * event's own trigger, purpose, and forbidden-content reasoning — this file is the typed
 * implementation of that document, not a duplicate of its prose.
 */
sealed interface AnalyticsEvent {
    val name: String
    fun properties(): Map<String, AnalyticsValue>
}

// ==================== Onboarding ====================

/** [step] is the destination step reached — never a field value the family entered (see [OnboardingStep]'s own KDoc: no profile answers are ever sent). */
data class OnboardingStepViewed(
    val step: OnboardingStep,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.ONBOARDING_STEP to AnalyticsValue.Text(step.name.lowercase()),
    )

    companion object {
        const val EVENT_NAME = "onboarding_step_viewed"
    }
}

/** No properties — never the family's chosen name, age bands, interests, or preferences. */
data object OnboardingCompleted : AnalyticsEvent {
    const val EVENT_NAME = "onboarding_completed"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

// ==================== Today ====================

data class DailyQuestRevealed(
    val category: QuestCategory,
    val durationBucket: DurationBand,
    val energyLevel: EnergyLevel,
    val accessRequired: QuestAccess,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = questPropertiesOf(category, durationBucket, energyLevel, accessRequired)

    companion object {
        const val EVENT_NAME = "daily_quest_revealed"
    }
}

data class DailyQuestRerolled(
    val category: QuestCategory,
    val durationBucket: DurationBand,
    val energyLevel: EnergyLevel,
    val accessRequired: QuestAccess,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = questPropertiesOf(category, durationBucket, energyLevel, accessRequired)

    companion object {
        const val EVENT_NAME = "daily_quest_rerolled"
    }
}

/** No properties — fired whenever a reroll attempt is blocked by the free-tier allowance, regardless of which of the two call sites blocked it (see `TodayViewModel`'s own KDoc). */
data object DailyQuestRerollBlocked : AnalyticsEvent {
    const val EVENT_NAME = "daily_quest_reroll_blocked"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

// ==================== Quest detail / quest lifecycle ====================

/** [questId] is always a bundled catalogue quest id — never a completion id or any local database row id. */
data class QuestViewed(
    val questId: QuestId,
    val source: QuestOpenSource,
    val category: QuestCategory,
    val accessRequired: QuestAccess,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = questPropertiesOf(category, null, null, accessRequired) +
        mapOf(
            TelemetryPropertyNames.QUEST_ID to AnalyticsValue.Text(questId.value),
            TelemetryPropertyNames.SOURCE to AnalyticsValue.Text(source.name.lowercase()),
        )

    companion object {
        const val EVENT_NAME = "quest_viewed"
    }
}

/** [isSaved] is the new state after the toggle — never the whole saved-quest list. */
data class QuestSaved(
    val questId: QuestId,
    val isSaved: Boolean,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.QUEST_ID to AnalyticsValue.Text(questId.value),
        TelemetryPropertyNames.IS_SAVED to AnalyticsValue.BooleanValue(isSaved),
    )

    companion object {
        const val EVENT_NAME = "quest_saved"
    }
}

data class QuestStarted(
    val questId: QuestId,
    val source: QuestOpenSource,
    val category: QuestCategory,
    val accessRequired: QuestAccess,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = questPropertiesOf(category, null, null, accessRequired) +
        mapOf(
            TelemetryPropertyNames.QUEST_ID to AnalyticsValue.Text(questId.value),
            TelemetryPropertyNames.SOURCE to AnalyticsValue.Text(source.name.lowercase()),
        )

    companion object {
        const val EVENT_NAME = "quest_started"
    }
}

/**
 * [usedTimer] is true only when the quest had a timer *and* completion happened after it finished
 * (see `QuestModeViewModel`'s own KDoc on why "still running" at completion means the timer wasn't
 * waited out) — never the exact elapsed seconds. [usedPhoneDown] reflects whatever
 * `QuestModeUiState.Content.phoneDown` was at the moment of completion.
 */
data class QuestCompleted(
    val questId: QuestId,
    val category: QuestCategory,
    val durationBucket: DurationBand,
    val energyLevel: EnergyLevel,
    val accessRequired: QuestAccess,
    val usedTimer: Boolean,
    val usedPhoneDown: Boolean,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = questPropertiesOf(category, durationBucket, energyLevel, accessRequired) +
        mapOf(
            TelemetryPropertyNames.QUEST_ID to AnalyticsValue.Text(questId.value),
            TelemetryPropertyNames.USED_TIMER to AnalyticsValue.BooleanValue(usedTimer),
            TelemetryPropertyNames.USED_PHONE_DOWN to AnalyticsValue.BooleanValue(usedPhoneDown),
        )

    companion object {
        const val EVENT_NAME = "quest_completed"
    }
}

data class QuestAbandoned(
    val questId: QuestId,
    val usedPhoneDown: Boolean,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.QUEST_ID to AnalyticsValue.Text(questId.value),
        TelemetryPropertyNames.USED_PHONE_DOWN to AnalyticsValue.BooleanValue(usedPhoneDown),
    )

    companion object {
        const val EVENT_NAME = "quest_abandoned"
    }
}

// ==================== Memories / Journey ====================

/** Never [com.togetherly.domain.completion.MemoryNote]'s own text, and never a media file reference — only whether each kind of content is present. */
data class MemorySaved(
    val hasNote: Boolean,
    val hasPhoto: Boolean,
    val hasVoice: Boolean,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.HAS_NOTE to AnalyticsValue.BooleanValue(hasNote),
        TelemetryPropertyNames.HAS_PHOTO to AnalyticsValue.BooleanValue(hasPhoto),
        TelemetryPropertyNames.HAS_VOICE to AnalyticsValue.BooleanValue(hasVoice),
    )

    companion object {
        const val EVENT_NAME = "memory_saved"
    }
}

/** No properties — never the completion id (a local database row id, not a catalogue id) or what the deleted memory contained. */
data object MemoryDeleted : AnalyticsEvent {
    const val EVENT_NAME = "memory_deleted"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

/**
 * Journey entries are already fully visible inline in the list — there is no separate "open" gesture
 * for a note or photo, only for a voice clip's playback (see `JourneyViewModel.onPlayVoiceClicked`).
 * No properties — never the completion id or media reference.
 */
data object MemoryOpened : AnalyticsEvent {
    const val EVENT_NAME = "memory_opened"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

// ==================== Explore ====================

/** A boolean marker only — never the search query text. */
data object ExploreSearched : AnalyticsEvent {
    const val EVENT_NAME = "explore_searched"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

/** Every field is a predefined filter category; an absent filter dimension is simply omitted from [properties], never sent as a null/empty-string placeholder. */
data class ExploreFiltered(
    val category: QuestCategory?,
    val durationBucket: DurationBand?,
    val energyLevel: EnergyLevel?,
    val location: QuestLocation?,
    val accessFilter: QuestAccessFilter,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = buildMap {
        category?.let { put(TelemetryPropertyNames.QUEST_CATEGORY, AnalyticsValue.Text(it.name.lowercase())) }
        durationBucket?.let { put(TelemetryPropertyNames.DURATION_BUCKET, AnalyticsValue.Text(it.name.lowercase())) }
        energyLevel?.let { put(TelemetryPropertyNames.ENERGY_LEVEL, AnalyticsValue.Text(it.name.lowercase())) }
        location?.let { put(TelemetryPropertyNames.LOCATION, AnalyticsValue.Text(it.name.lowercase())) }
        put(TelemetryPropertyNames.ACCESS_FILTER, AnalyticsValue.Text(accessFilter.name.lowercase()))
    }

    companion object {
        const val EVENT_NAME = "explore_filtered"
    }
}

data class PackViewed(
    val packId: QuestPackId,
    val accessRequired: QuestAccess,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.PACK_ID to AnalyticsValue.Text(packId.value),
        TelemetryPropertyNames.ACCESS_REQUIRED to AnalyticsValue.Text(accessRequired.toAccessRequiredLabel()),
    )

    companion object {
        const val EVENT_NAME = "pack_viewed"
    }
}

/** Exactly one of [questId]/[packId] is non-null — whichever piece of locked premium content was viewed. */
data class PremiumContentViewed(
    val questId: QuestId?,
    val packId: QuestPackId?,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = buildMap {
        questId?.let { put(TelemetryPropertyNames.QUEST_ID, AnalyticsValue.Text(it.value)) }
        packId?.let { put(TelemetryPropertyNames.PACK_ID, AnalyticsValue.Text(it.value)) }
    }

    companion object {
        const val EVENT_NAME = "premium_content_viewed"
    }
}

/**
 * Fired alongside the reactive entitlement collector already running on Quest Detail/Pack Details
 * (`QuestDetailViewModel.onScreenStarted`/`PackDetailsViewModel.onScreenStarted`) the moment
 * previously-locked content the family is still looking at becomes accessible — a purchase or
 * restore completing while this screen is open, never a guess at *why* access changed. Exactly one
 * of [questId]/[packId] is non-null, mirroring [PremiumContentViewed].
 */
data class PremiumContentUnlocked(
    val questId: QuestId?,
    val packId: QuestPackId?,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = buildMap {
        questId?.let { put(TelemetryPropertyNames.QUEST_ID, AnalyticsValue.Text(it.value)) }
        packId?.let { put(TelemetryPropertyNames.PACK_ID, AnalyticsValue.Text(it.value)) }
    }

    companion object {
        const val EVENT_NAME = "premium_content_unlocked"
    }
}

// ==================== Paywall / purchases ====================

/** [context] is already provider-neutral (see [PaywallContext]'s own KDoc) — sent as-is, lowercased. [offeringIdentifier]/[availablePackageTypes] are omitted/empty when packages haven't finished loading yet at capture time — never guessed. */
data class PaywallPresented(
    val context: PaywallContext,
    val sourceScreen: AnalyticsScreen,
    val offeringIdentifier: String?,
    val availablePackageTypes: Set<PurchasePackageType>,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> =
        paywallContextPropertiesOf(context, sourceScreen, offeringIdentifier, availablePackageTypes)

    companion object {
        const val EVENT_NAME = "paywall_presented"
    }
}

data class PaywallDismissed(
    val context: PaywallContext,
    val sourceScreen: AnalyticsScreen,
    val offeringIdentifier: String?,
    val availablePackageTypes: Set<PurchasePackageType>,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> =
        paywallContextPropertiesOf(context, sourceScreen, offeringIdentifier, availablePackageTypes)

    companion object {
        const val EVENT_NAME = "paywall_dismissed"
    }
}

/** Never a price, a product ID, a receipt, or a purchase token — only the package's own broad type. */
data class PurchaseStarted(
    val context: PaywallContext,
    val sourceScreen: AnalyticsScreen,
    val packageType: PurchasePackageType,
    val offeringIdentifier: String,
    val availablePackageTypes: Set<PurchasePackageType>,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> =
        paywallContextPropertiesOf(context, sourceScreen, offeringIdentifier, availablePackageTypes) +
            mapOf(TelemetryPropertyNames.PACKAGE_TYPE to AnalyticsValue.Text(packageType.name.lowercase()))

    companion object {
        const val EVENT_NAME = "purchase_started"
    }
}

/** Every value a real store/RevenueCat outcome can produce — [Failure][com.togetherly.domain.purchase.PurchaseResult.Failure]'s own [com.togetherly.domain.purchase.PurchaseError] is bucketed into one of the five granular failure reasons via [toPurchaseOutcomeResult], never left as a generic `FAILURE` catch-all. */
enum class PurchaseOutcomeResult {
    SUCCESS, CANCELLED, PENDING, ALREADY_OWNED, STORE_UNAVAILABLE, PRODUCT_UNAVAILABLE, NETWORK_ERROR, UNKNOWN_ERROR
}

/** [packageType] is omitted when not known at the outcome point (e.g. a failure before a package could be resolved) rather than guessed. Never the underlying [com.togetherly.domain.purchase.PurchaseError] message. */
data class PurchaseOutcome(
    val context: PaywallContext,
    val sourceScreen: AnalyticsScreen,
    val packageType: PurchasePackageType?,
    val result: PurchaseOutcomeResult,
    val offeringIdentifier: String?,
    val availablePackageTypes: Set<PurchasePackageType>,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> =
        paywallContextPropertiesOf(context, sourceScreen, offeringIdentifier, availablePackageTypes) +
            buildMap {
                put(TelemetryPropertyNames.RESULT, AnalyticsValue.Text(result.name.lowercase()))
                packageType?.let { put(TelemetryPropertyNames.PACKAGE_TYPE, AnalyticsValue.Text(it.name.lowercase())) }
            }

    companion object {
        const val EVENT_NAME = "purchase_result"
    }
}

/** No properties beyond [sourceScreen] — restore has no package-selection step or offering to attach a package type to. */
data class RestoreStarted(
    val sourceScreen: AnalyticsScreen,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.SOURCE_SCREEN to AnalyticsValue.Text(sourceScreen.name.lowercase()),
    )

    companion object {
        const val EVENT_NAME = "restore_started"
    }
}

/** [RestoreOutcomeResult.NO_PURCHASES] aside, the remaining failure buckets reuse the same [com.togetherly.domain.purchase.PurchaseError] mapping [PurchaseOutcomeResult] uses (see [toRestoreOutcomeResult]) — restore and purchase share the same underlying error type. */
enum class RestoreOutcomeResult {
    SUCCESS, NO_PURCHASES, ALREADY_OWNED, STORE_UNAVAILABLE, PRODUCT_UNAVAILABLE, NETWORK_ERROR, UNKNOWN_ERROR
}

/** [RestoreOutcomeResult.NO_PURCHASES] is distinct from [RestoreOutcomeResult.SUCCESS] — a restore call that completed without error but found nothing to restore is not the same product outcome as one that actually reinstated Family Plus access (see `FamilyPlusPaywallViewModel.restore()`'s own `isPlus` check). */
data class RestoreOutcome(
    val sourceScreen: AnalyticsScreen,
    val result: RestoreOutcomeResult,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.SOURCE_SCREEN to AnalyticsValue.Text(sourceScreen.name.lowercase()),
        TelemetryPropertyNames.RESULT to AnalyticsValue.Text(result.name.lowercase()),
    )

    companion object {
        const val EVENT_NAME = "restore_result"
    }
}

// ==================== Family settings ====================

/** [selectedDayCount] is a count, never which specific days or what time was chosen (see `docs/analytics-event-taxonomy.md`: exact reminder time/days are explicitly forbidden). */
data class ReminderPreferenceChanged(
    val enabled: Boolean,
    val selectedDayCount: Int,
) : AnalyticsEvent {
    override val name: String = EVENT_NAME

    override fun properties(): Map<String, AnalyticsValue> = mapOf(
        TelemetryPropertyNames.REMINDER_ENABLED to AnalyticsValue.BooleanValue(enabled),
        TelemetryPropertyNames.SELECTED_DAY_COUNT to AnalyticsValue.Number(selectedDayCount.toLong()),
    )

    companion object {
        const val EVENT_NAME = "reminder_preference_changed"
    }
}

// ==================== Shared helpers ====================

private fun questPropertiesOf(
    category: QuestCategory,
    durationBucket: DurationBand?,
    energyLevel: EnergyLevel?,
    accessRequired: QuestAccess,
): Map<String, AnalyticsValue> = buildMap {
    put(TelemetryPropertyNames.QUEST_CATEGORY, AnalyticsValue.Text(category.name.lowercase()))
    durationBucket?.let { put(TelemetryPropertyNames.DURATION_BUCKET, AnalyticsValue.Text(it.name.lowercase())) }
    energyLevel?.let { put(TelemetryPropertyNames.ENERGY_LEVEL, AnalyticsValue.Text(it.name.lowercase())) }
    put(TelemetryPropertyNames.ACCESS_REQUIRED, AnalyticsValue.Text(accessRequired.toAccessRequiredLabel()))
}

/**
 * Shared by every paywall-lifecycle event ([PaywallPresented]/[PaywallDismissed]/[PurchaseStarted]/
 * [PurchaseOutcome]) — [offeringIdentifier] is omitted and [availablePackageTypes] sent as nothing
 * (never a null/empty placeholder) when packages haven't finished loading yet at capture time.
 * [availablePackageTypes] is joined into one comma-separated, sorted [AnalyticsValue.Text] since
 * [AnalyticsValue] has no list variant — still a closed, predefined vocabulary (at most
 * `"annual,lifetime,monthly"`), never free text.
 */
private fun paywallContextPropertiesOf(
    context: PaywallContext,
    sourceScreen: AnalyticsScreen,
    offeringIdentifier: String?,
    availablePackageTypes: Set<PurchasePackageType>,
): Map<String, AnalyticsValue> = buildMap {
    put(TelemetryPropertyNames.PAYWALL_CONTEXT, AnalyticsValue.Text(context.name.lowercase()))
    put(TelemetryPropertyNames.SOURCE_SCREEN, AnalyticsValue.Text(sourceScreen.name.lowercase()))
    offeringIdentifier?.let { put(TelemetryPropertyNames.OFFERING_IDENTIFIER, AnalyticsValue.Text(it)) }
    if (availablePackageTypes.isNotEmpty()) {
        val joined = availablePackageTypes.map { it.name.lowercase() }.distinct().sorted().joinToString(",")
        put(TelemetryPropertyNames.AVAILABLE_PACKAGE_TYPES, AnalyticsValue.Text(joined))
    }
}

private fun QuestAccess.toAccessRequiredLabel(): String = when (this) {
    is QuestAccess.Free -> "free"
    is QuestAccess.Premium -> "premium"
}

/** Maps every [PurchaseError] variant onto the analytics-safe granular bucket [PurchaseOutcome]'s allowed [PurchaseOutcomeResult] values are drawn from — never the error's own message, code, or exception. [PurchaseError.ConfigurationProblem]/[PurchaseError.PurchaseNotAllowed] both fall back to [PurchaseOutcomeResult.UNKNOWN_ERROR] — the governing spec's own allowed-values list has no dedicated bucket for either. */
fun PurchaseError.toPurchaseOutcomeResult(): PurchaseOutcomeResult = when (this) {
    PurchaseError.AlreadyOwned -> PurchaseOutcomeResult.ALREADY_OWNED
    PurchaseError.StoreUnavailable -> PurchaseOutcomeResult.STORE_UNAVAILABLE
    PurchaseError.ProductUnavailable -> PurchaseOutcomeResult.PRODUCT_UNAVAILABLE
    PurchaseError.NetworkProblem -> PurchaseOutcomeResult.NETWORK_ERROR
    PurchaseError.ConfigurationProblem, PurchaseError.PurchaseNotAllowed, PurchaseError.Unknown -> PurchaseOutcomeResult.UNKNOWN_ERROR
}

/** Same bucketing as [toPurchaseOutcomeResult], for [RestoreOutcome]'s own allowed [RestoreOutcomeResult] values. */
fun PurchaseError.toRestoreOutcomeResult(): RestoreOutcomeResult = when (this) {
    PurchaseError.AlreadyOwned -> RestoreOutcomeResult.ALREADY_OWNED
    PurchaseError.StoreUnavailable -> RestoreOutcomeResult.STORE_UNAVAILABLE
    PurchaseError.ProductUnavailable -> RestoreOutcomeResult.PRODUCT_UNAVAILABLE
    PurchaseError.NetworkProblem -> RestoreOutcomeResult.NETWORK_ERROR
    PurchaseError.ConfigurationProblem, PurchaseError.PurchaseNotAllowed, PurchaseError.Unknown -> RestoreOutcomeResult.UNKNOWN_ERROR
}

// ==================== Debug ====================

/**
 * No properties — the one event this app ever deliberately sends as a manual, end-to-end pipeline
 * check (Step 14.6's debug telemetry screen), never a real product signal. Only reachable from
 * that debug-only screen, itself gated by `AppConfiguration.debug` the same way
 * [com.togetherly.feature.family.presentation.AboutViewModel]'s existing test-diagnostic action
 * already is.
 */
data object DebugTestEvent : AnalyticsEvent {
    const val EVENT_NAME = "debug_test_event"
    override val name: String = EVENT_NAME
    override fun properties(): Map<String, AnalyticsValue> = emptyMap()
}

/**
 * Test-support only — never called from feature or production code. Exists purely because
 * [AnalyticsEvent] being a `sealed interface` means only this file (not `commonTest`, a separate
 * Kotlin compilation) can add a new implementation; validator/debug-provider tests need one to
 * exercise "unregistered event"/"unregistered property" rejection scenarios against a validator
 * built from a test-local schema, without touching the real production [TelemetryEventRegistry].
 */
internal data class TestAnalyticsEvent(
    override val name: String,
    private val rawProperties: Map<String, AnalyticsValue> = emptyMap(),
) : AnalyticsEvent {
    override fun properties(): Map<String, AnalyticsValue> = rawProperties
}
