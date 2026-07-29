package com.togetherly.app.di

import com.togetherly.domain.completion.usecase.CompleteQuest
import com.togetherly.domain.completion.usecase.DeleteCompletion
import com.togetherly.domain.completion.usecase.DiscardCompletionMemoryDraft
import com.togetherly.domain.completion.usecase.PrepareQuestStartUseCase
import com.togetherly.domain.completion.usecase.ReplaceActiveQuestSession
import com.togetherly.domain.completion.usecase.ResolveCompletionTransition
import com.togetherly.domain.completion.usecase.SaveCompletionMemory
import com.togetherly.domain.completion.usecase.StartQuest
import com.togetherly.domain.daily.DefaultRerollAllowancePolicy
import com.togetherly.domain.daily.RerollAllowancePolicy
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.daily.usecase.SelectDailyQuestForContext
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.FilterQuestsUseCase
import com.togetherly.domain.explore.usecase.GetQuestPackUseCase
import com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestsUseCase
import com.togetherly.domain.explore.usecase.SearchQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.family.usecase.DeleteAllFamilyData
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateFamilyProfile
import com.togetherly.domain.family.usecase.UpdateMemoryPreferences
import com.togetherly.domain.family.usecase.UpdatePrivacyPreferences
import com.togetherly.domain.family.usecase.UpdateQuestPreferences
import com.togetherly.domain.family.usecase.UpdateReminderPreference
import com.togetherly.domain.journey.JourneyConstellationPolicy
import com.togetherly.domain.journey.JourneyStarPolicy
import com.togetherly.domain.journey.usecase.GetJourneySummary
import com.togetherly.domain.localdata.usecase.DeleteAllLocalData
import com.togetherly.domain.localdata.usecase.DeleteMemories
import com.togetherly.domain.localdata.usecase.ResetQuestHistory
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.usecase.PurchaseFamilyPlus
import com.togetherly.domain.purchase.usecase.RefreshFamilyAccess
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.domain.questmode.DefaultQuestCountdownEngine
import com.togetherly.domain.questmode.QuestCountdownEngine
import com.togetherly.domain.questmode.QuestTimerPolicy
import com.togetherly.domain.questmode.usecase.AbandonQuest
import com.togetherly.domain.questmode.usecase.LoadQuestMode
import com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationPolicy
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.saved.usecase.SetQuestSaved
import org.koin.dsl.module

/**
 * Use cases whose complete production dependency graph exists now that [FamilyRepository][com.togetherly.domain.family.repository.FamilyRepository],
 * [DailyQuestRepository][com.togetherly.domain.daily.repository.DailyQuestRepository],
 * [SavedQuestRepository][com.togetherly.domain.saved.repository.SavedQuestRepository],
 * [QuestRepository][com.togetherly.domain.quest.repository.QuestRepository],
 * [CompletionRepository][com.togetherly.domain.completion.repository.CompletionRepository],
 * [JourneyRepository][com.togetherly.domain.journey.repository.JourneyRepository],
 * [QuestSessionTransaction][com.togetherly.domain.completion.repository.QuestSessionTransaction]
 * and [FamilyDataCleaner][com.togetherly.domain.family.repository.FamilyDataCleaner] are all real
 * (Steps 5.4, 6.4, 6.5, 6.6) — part of [appModules] starting Step 6.4.
 *
 * [StartQuest] and [CompleteQuest] depend on [QuestSessionTransaction][com.togetherly.domain.completion.repository.QuestSessionTransaction],
 * not a plain [CompletionRepository][com.togetherly.domain.completion.repository.CompletionRepository]
 * read-then-write, for their active-session check-and-write (Step 6.6) — see that interface's own
 * KDoc for why.
 *
 * [GetOrSelectDailyQuest]/[RerollDailyQuest] graduated here in Step 8.1 alongside
 * [DeterministicQuestRecommendationPolicy] — the only production [QuestRecommendationPolicy].
 * [RecommendationConfig.DEFAULT] is the single source of truth for scoring weights and the
 * dismissal cooldown window, shared by the policy and [RecommendationHistoryBuilder].
 * [SelectDailyQuestForContext] joined in Step 8.2 alongside [DefaultRerollAllowancePolicy] — the
 * only production [RerollAllowancePolicy], free-tier only (`hasFamilyPlus` is hardcoded `false` at
 * every call site until premium entitlement resolution is wired up; see that class's own KDoc).
 *
 * [SaveCompletionMemory]/[DiscardCompletionMemoryDraft] graduated here in Step 10.3 once
 * [PrivateMediaCommitterImpl][com.togetherly.data.media.PrivateMediaCommitterImpl] became a fully
 * real [PrivateMediaCommitter][com.togetherly.domain.completion.repository.PrivateMediaCommitter]
 * (photo since Step 10.2, voice since Step 10.3).
 *
 * [RefreshFamilyAccess]/[PurchaseFamilyPlus]/[RestoreFamilyPlus] graduated here in Step 11 once
 * [RevenueCatEntitlementRepository][com.togetherly.data.purchase.RevenueCatEntitlementRepository]
 * became the real [EntitlementRepository][com.togetherly.domain.purchase.repository.EntitlementRepository] —
 * the last use case family waiting on a missing repository, so there is no longer a
 * `domainUseCaseModule` at all (every use case in this codebase now has a complete production
 * dependency graph). A future step needing the same "not yet ready" staging pattern can reintroduce
 * that module then.
 */
val readyUseCaseModule = module {
    factory { CreateFamilyProfile(get(), get(), get()) }
    factory { UpdateFamilyProfile(get(), get()) }
    factory { DeleteAllFamilyData(get()) }
    factory { DeleteMemories(get(), get(), get()) }
    factory { ResetQuestHistory(get(), get(), get()) }
    factory { DeleteAllLocalData(get(), get(), get(), get(), get(), get()) }
    factory { ObserveFamilySettings(get()) }
    factory { UpdateQuestPreferences(get()) }
    factory { UpdateReminderPreference(get()) }
    factory { UpdateMemoryPreferences(get()) }
    factory { UpdatePrivacyPreferences(get()) }
    factory { SetQuestSaved(get(), get(), get()) }
    factory { StartQuest(get(), get(), get(), get(), get(), get(), get()) }
    factory { PrepareQuestStartUseCase(get(), get(), get(), get()) }
    factory { ReplaceActiveQuestSession(get(), get(), get(), get(), get()) }
    factory { CompleteQuest(get(), get(), get()) }
    factory { ResolveCompletionTransition(get()) }
    factory { DeleteCompletion(get(), get()) }
    factory { GetJourneySummary(get()) }
    single { JourneyStarPolicy() }
    single { JourneyConstellationPolicy(get()) }
    factory { SaveCompletionMemory(get(), get(), get()) }
    factory { DiscardCompletionMemoryDraft(get()) }
    single { RecommendationConfig.DEFAULT }
    single<QuestRecommendationPolicy> { DeterministicQuestRecommendationPolicy(get()) }
    single<RerollAllowancePolicy> { DefaultRerollAllowancePolicy() }
    single { QuestAccessPolicy() }
    factory { RecommendationHistoryBuilder(get(), get(), get(), get()) }
    factory { GetOrSelectDailyQuest(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SelectDailyQuestForContext(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RerollDailyQuest(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { QuestTimerPolicy() }
    factory { LoadQuestMode(get(), get(), get(), get()) }
    factory { AbandonQuest(get()) }
    single<QuestCountdownEngine> { DefaultQuestCountdownEngine(clock = get(), timerPolicy = get(), dispatchers = get()) }
    factory { RefreshFamilyAccess(get()) }
    factory { PurchaseFamilyPlus(get()) }
    factory { RestoreFamilyPlus(get()) }
    factory { ObserveExploreCatalogueUseCase(get()) }
    factory { SearchQuestsUseCase() }
    factory { FilterQuestsUseCase() }
    factory { GetQuestPackUseCase(get()) }
    factory { ObserveSavedQuestIdsUseCase(get()) }
    factory { ObserveSavedQuestsUseCase(get(), get()) }
    factory { ToggleSavedQuestUseCase(get(), get()) }
    factory { EvaluateQuestAccessUseCase(get(), get(), get()) }
    factory { EvaluatePackAccessUseCase(get(), get(), get()) }
}
