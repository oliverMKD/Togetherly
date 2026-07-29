package com.togetherly.navigation.destination

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.paywall.model.PaywallContext
import com.togetherly.feature.questdetail.model.QuestOpenSource
import kotlinx.serialization.Serializable

/**
 * The app's top-level destinations. [Bootstrap] decides, from the family-profile source of
 * truth (never a stored "has onboarded" boolean — see [com.togetherly.navigation.state.BootstrapViewModel]'s
 * own KDoc), whether the user lands on [Onboarding] or [Main]; it is never itself returned to once
 * that decision is made.
 *
 * Each is a plain `@Serializable` destination rather than a string route — see
 * [com.togetherly.navigation.host.TogetherlyNavHost] for how these back a type-safe `NavHost`.
 * [Onboarding] is a single destination hosting the whole onboarding state machine (see
 * [com.togetherly.feature.onboarding.model.OnboardingStep]'s own KDoc for why there is no
 * per-step nested graph); [Main] nests [com.togetherly.navigation.destination.MainDestination].
 *
 * [QuestDetail] and [QuestMode] (Step 8.6) live here, not on [MainDestination], deliberately:
 * both are pushed *above* [Main]'s own tab bar (a screen inside one tab reaches them by climbing
 * out to the root `NavHost`, never through a nested tab graph — see
 * [com.togetherly.navigation.shell.MainShell]'s own KDoc), and [QuestMode] must render with no
 * bottom navigation at all, which a root-level destination gets for free simply by not being
 * composed inside [com.togetherly.navigation.shell.MainShell]'s own [com.togetherly.designsystem.component.layout.TogetherlyScreen].
 * Both carry only a typed ID ([QuestId]/[CompletionId]), never a domain model — see each
 * constructor's own KDoc for why.
 */
sealed interface RootDestination {

    @Serializable
    data object Bootstrap : RootDestination

    @Serializable
    data object Onboarding : RootDestination

    @Serializable
    data object Main : RootDestination

    /**
     * The Family Plus paywall (Step 11.5/11.6, extended Step 12.1) — a single, always-current-offering
     * destination reached from multiple triggering contexts. [context] only ever changes intro
     * copy/analytics — purchases always use the current offering regardless of why the paywall
     * opened (see [PaywallContext]'s own KDoc). [questId]/[packId] are populated only for
     * [PaywallContext.PREMIUM_QUEST]/[PaywallContext.PREMIUM_PACK] respectively (plain `String`s,
     * not [com.togetherly.domain.quest.QuestId]/[QuestPackId], to avoid a second/third nullable
     * [androidx.navigation.NavType] just for these optional arguments — converted back to the
     * typed id at the presentation boundary). Together they double as "where to return after a
     * successful purchase" — an explicit `returnDestination: RootDestination` field was considered
     * and rejected: [RootDestination] itself isn't `@Serializable` as a whole (only each concrete
     * case is), so embedding one destination inside another would need polymorphic serialization
     * support this project's navigation conventions don't otherwise use anywhere. [context] plus
     * the relevant id is sufficient to know the return target (a future Explore screen resolves
     * "return to `PackDetails(packId)`" from [PaywallContext.PREMIUM_PACK] itself) without it.
     */
    @Serializable
    data class FamilyPlusPaywall(val context: PaywallContext, val questId: String? = null, val packId: String? = null) : RootDestination

    /**
     * RevenueCat's own Customer Center UI (Step 11.7), reached only from the Family Plus
     * management screen's own "Manage subscription" action — never opened automatically, never
     * offered when [com.togetherly.domain.purchase.repository.EntitlementRepository.isReady] is
     * false. Renders with no bottom navigation, the same reason [QuestMode] is root-level rather
     * than nested inside [com.togetherly.navigation.shell.MainShell].
     */
    @Serializable
    data object CustomerCenter : RootDestination

    /**
     * [questId] only — never the [com.togetherly.domain.quest.FamilyQuest] itself; the detail
     * screen resolves it fresh from the catalogue. [source] (Step 12.1) records where the user
     * navigated here *from*, defaulted to [QuestOpenSource.TODAY] so every pre-Explore call site
     * keeps compiling and behaving unchanged; Explore's own future screens are the first to pass
     * [QuestOpenSource.EXPLORE]/[QuestOpenSource.PACK]/[QuestOpenSource.SAVED] explicitly.
     */
    @Serializable
    data class QuestDetail(val questId: QuestId, val source: QuestOpenSource = QuestOpenSource.TODAY) : RootDestination

    /**
     * A quest pack's detail screen (Step 12.1) — [packId] only, never [com.togetherly.domain.quest.QuestPack]
     * itself, same reasoning as [QuestDetail]. Pushed above [Main]'s tab bar, same as [QuestDetail]/
     * [QuestMode] — reached from Explore climbing out of [com.togetherly.navigation.shell.MainShell],
     * never a destination nested inside its own tab graph.
     */
    @Serializable
    data class PackDetails(val packId: QuestPackId) : RootDestination

    /**
     * Explore's filter screen (Step 12.1, real content Step 12.4) — carries no arguments. The
     * committed filters it reads and writes live in [com.togetherly.feature.explore.presentation.ExploreFilterStore]
     * (a Koin singleton), not as nav arguments or a `SavedStateHandle` result — this destination's
     * own KDoc originally anticipated needing "the result to flow back to the Explore screen that
     * opened it"; a shared store is how that flow-back actually happens, since [RootDestination]
     * isn't polymorphically serializable (same reasoning [FamilyPlusPaywall]'s own KDoc gives for
     * not embedding a return destination directly).
     */
    @Serializable
    data object ExploreFilters : RootDestination

    /**
     * The Saved quests screen (Step 12.4) — carries no arguments; observes the same
     * [com.togetherly.domain.saved.repository.SavedQuestRepository] every other saved-state-aware
     * screen does, never a copy. Reached from Explore climbing out of [com.togetherly.navigation.shell.MainShell],
     * same as [PackDetails]/[QuestDetail].
     */
    @Serializable
    data object Saved : RootDestination

    /**
     * The Family Profile editor (Step 13.2) — carries no arguments, same reasoning as [Saved]/
     * [ExploreFilters]: it reads and writes the single [com.togetherly.domain.family.FamilyProfile]
     * source of truth itself via [com.togetherly.domain.family.repository.FamilyRepository], rather
     * than being handed a copy through nav args. Reached only from the parent-facing Family tab
     * root, never gated by [TogetherlyParentalGateDialog][com.togetherly.designsystem.component.gate.TogetherlyParentalGateDialog]
     * — that gate exists for purchase-capable paywalls reached from child-facing surfaces (see
     * [FamilyPlusPaywall]'s three trigger sites in [com.togetherly.navigation.host.TogetherlyNavHost]),
     * and editing preferences from an already parent-facing settings area follows the same
     * direct-navigation precedent [MainShell.onOpenFamilyPlusPaywall][com.togetherly.navigation.shell.MainShell] already established.
     */
    @Serializable
    data object FamilyProfileEditor : RootDestination

    /**
     * The quest preferences editor (Step 13.3) — carries no arguments, same reasoning as
     * [FamilyProfileEditor]: it reads and writes the single [com.togetherly.domain.family.FamilySettings.questPreferences]
     * itself via [com.togetherly.domain.family.repository.FamilySettingsRepository]. Reached only
     * from the parent-facing Family tab root — not gated, same precedent as [FamilyProfileEditor].
     */
    @Serializable
    data object QuestPreferences : RootDestination

    /**
     * The reminder settings editor (Step 13.4) — carries no arguments, same reasoning as
     * [QuestPreferences]: reads/writes [com.togetherly.domain.family.FamilySettings.reminderPreference]
     * via [com.togetherly.domain.family.repository.FamilySettingsRepository]. Reached only from the
     * parent-facing Family tab root — not gated, same precedent as [FamilyProfileEditor]/[QuestPreferences].
     */
    @Serializable
    data object Reminder : RootDestination

    /**
     * The memory settings editor (Step 13.5) — carries no arguments, same reasoning as
     * [Reminder]/[QuestPreferences]: reads/writes [com.togetherly.domain.family.FamilySettings.memoryPreferences]
     * via [com.togetherly.domain.family.repository.FamilySettingsRepository]. Its own "Manage
     * memories" action pops back to [Main] and requests the Journey tab via
     * [com.togetherly.navigation.shell.RequestedTabStore] rather than pushing a new destination —
     * see that store's own KDoc for why (Journey already is the memory-management experience; this
     * step connects to it rather than rebuilding it).
     */
    @Serializable
    data object MemorySettings : RootDestination

    /**
     * A static, informational privacy summary (Step 13.5) — carries no arguments, reads nothing
     * (no [com.togetherly.domain.family.FamilySettings] dependency at all; see
     * `feature/family/presentation/PrivacyScreen.kt`'s own KDoc for why there is deliberately no
     * diagnostics toggle here).
     */
    @Serializable
    data object Privacy : RootDestination

    /**
     * A list of external legal-document links plus [OpenSourceLicenses] (Step 13.6) — carries no
     * arguments, same reasoning as [Privacy]: no gate needed, parent-facing already. Reads
     * [com.togetherly.app.application.LegalConfiguration] directly rather than
     * [com.togetherly.domain.family.repository.FamilySettingsRepository] — nothing here is
     * per-family state.
     */
    @Serializable
    data object Legal : RootDestination

    /**
     * A static, offline, hand-curated list of bundled open-source dependencies (Step 13.6) —
     * carries no arguments, same reasoning as [Legal]. See
     * `docs/open-source-licenses.md`/[com.togetherly.feature.family.model.OPEN_SOURCE_LICENSES]
     * for why this is hand-curated rather than a generated Compose resource.
     */
    @Serializable
    data object OpenSourceLicenses : RootDestination

    /**
     * Application name/version/build number plus an optional debug-only environment label and
     * support link (Step 13.6) — carries no arguments, same reasoning as [Legal]. Reads
     * [com.togetherly.app.application.AppConfiguration] and
     * [com.togetherly.app.foundation.VersionInfoProvider] directly, never `BuildConfig`/`Info.plist`
     * itself — see [com.togetherly.feature.family.presentation.AboutViewModel]'s own KDoc.
     */
    @Serializable
    data object About : RootDestination

    /**
     * Parent-facing "Delete memories" / "Reset quest history" / "Delete all local data" (Step
     * 13.7) — carries no arguments, same reasoning as [About]: no gate needed, parent-facing
     * already. Unlike every other destination in this list, a successful "Delete all local data"
     * here does not simply pop back — [com.togetherly.navigation.host.TogetherlyNavHost] instead
     * clears the entire back stack down through [Main] and navigates to [Onboarding], since the
     * family profile this whole back stack was built on top of no longer exists. See
     * `feature/family/presentation/DataManagementViewModel.kt`'s own KDoc for exactly what each
     * action deletes and `docs/local-data-deletion.md` for the full rationale.
     */
    @Serializable
    data object DataManagement : RootDestination

    /** [completionId] only — never the [com.togetherly.domain.completion.ActiveQuestSession] itself; Quest Mode resolves it fresh from the repository. */
    @Serializable
    data class QuestMode(val completionId: CompletionId) : RootDestination

    /**
     * [completionId] only — never [com.togetherly.domain.completion.QuestCompletion] itself; the
     * celebration screen resolves it (and its quest content) fresh (Step 9.6). Reached only after
     * [CompleteQuest][com.togetherly.domain.completion.usecase.CompleteQuest] has actually
     * persisted, with [QuestDetail]/[QuestMode] popped off the back stack — see
     * [com.togetherly.navigation.host.TogetherlyNavHost]'s own `composable<QuestMode>` block for
     * why Back from here must never be able to reopen the now-completed session.
     */
    @Serializable
    data class CompletionCelebration(val completionId: CompletionId) : RootDestination

    /**
     * [completionId] only — never a [com.togetherly.domain.completion.CompletionMemoryDraft]
     * itself; the memory-capture screen builds its own draft state fresh (Step 10.4). Reached only
     * from [CompletionCelebration]'s own "Add a memory" action; both Save and Skip from here pop
     * back down to [Main] — see [com.togetherly.navigation.host.TogetherlyNavHost]'s own
     * `composable<CompletionMemory>` block.
     */
    @Serializable
    data class CompletionMemory(val completionId: CompletionId) : RootDestination

    /**
     * Debug-only telemetry tooling (Step 14.6) — carries no arguments, same reasoning as [About].
     * Reachable only from [About]'s own debug-only "Open debug telemetry tools" action, itself
     * gated by [com.togetherly.app.application.AppConfiguration.debug] the same way [About]'s
     * existing test-diagnostic action already is — never reachable from anywhere else, and never
     * rendered at all on a release build (see
     * [com.togetherly.feature.debug.presentation.DebugTelemetryViewModel]'s own KDoc). See
     * `docs/debug-telemetry.md`.
     */
    @Serializable
    data object DebugTelemetry : RootDestination
}
