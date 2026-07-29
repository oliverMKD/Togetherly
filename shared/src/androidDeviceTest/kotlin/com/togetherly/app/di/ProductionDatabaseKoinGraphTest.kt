package com.togetherly.app.di

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.app.application.AppConfiguration
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.core.id.IdGenerator
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import com.togetherly.domain.completion.usecase.CompleteQuest
import com.togetherly.domain.completion.usecase.DeleteCompletion
import com.togetherly.domain.completion.usecase.PrepareQuestStartUseCase
import com.togetherly.domain.completion.usecase.StartQuest
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.questmode.QuestCountdownEngine
import com.togetherly.domain.questmode.QuestTimerPolicy
import com.togetherly.domain.questmode.usecase.AbandonQuest
import com.togetherly.domain.questmode.usecase.LoadQuestMode
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.daily.usecase.SelectDailyQuestForContext
import com.togetherly.domain.family.repository.FamilyDataCleaner
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.family.usecase.DeleteAllFamilyData
import com.togetherly.domain.family.usecase.UpdateFamilyProfile
import com.togetherly.domain.journey.repository.JourneyRepository
import com.togetherly.domain.journey.usecase.GetJourneySummary
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.data.media.PrivateMediaStorage
import com.togetherly.data.media.VoicePlaybackController
import com.togetherly.data.media.VoiceRecorder
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.completion.usecase.DiscardCompletionMemoryDraft
import com.togetherly.domain.completion.usecase.ReplaceActiveQuestSession
import com.togetherly.domain.completion.usecase.ResolveCompletionTransition
import com.togetherly.domain.completion.usecase.SaveCompletionMemory
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.FilterQuestsUseCase
import com.togetherly.domain.explore.usecase.GetQuestPackUseCase
import com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestsUseCase
import com.togetherly.domain.explore.usecase.SearchQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.purchase.usecase.PurchaseFamilyPlus
import com.togetherly.domain.purchase.usecase.RefreshFamilyAccess
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.data.purchase.RevenueCatApiKeyProvider
import com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel
import com.togetherly.feature.explore.presentation.ExploreFilterStore
import com.togetherly.feature.explore.presentation.ExploreFiltersViewModel
import com.togetherly.feature.explore.presentation.ExploreViewModel
import com.togetherly.feature.familyplus.presentation.FamilyPlusManagementViewModel
import com.togetherly.feature.onboarding.presentation.OnboardingViewModel
import com.togetherly.feature.packdetails.presentation.PackDetailsViewModel
import com.togetherly.feature.paywall.model.PaywallContext
import com.togetherly.feature.paywall.presentation.FamilyPlusPaywallViewModel
import com.togetherly.feature.questdetail.presentation.QuestDetailViewModel
import com.togetherly.feature.questmode.presentation.QuestModeViewModel
import com.togetherly.feature.saved.presentation.SavedViewModel
import com.togetherly.feature.today.presentation.TodayViewModel
import com.togetherly.navigation.state.BootstrapViewModel
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Verifies the real database-backed repositories and [readyUseCaseModule]'s use cases resolve
 * from the actual production graph — not just from a fake/in-memory substitute. This needs a real
 * [android.content.Context] (the whole reason database-touching Koin resolution can't run under
 * `:shared:testAndroidHostTest` — see [com.togetherly.data.local.database.DatabaseMetadataInstrumentedTest]'s
 * KDoc), so it's instrumented rather than living alongside [KoinGraphTest]. This is the one place
 * that opens the *real* file-backed database (`context.getDatabasePath("togetherly.db")`) — so
 * [tearDown] explicitly closes and deletes it, leaving no state a later test run could depend on.
 *
 * Uses [startKoin] (Koin's *global* context), not [org.koin.dsl.koinApplication]'s isolated
 * instance: Android's [createDatabaseBuilder][com.togetherly.data.local.database.createDatabaseBuilder]
 * resolves its [android.content.Context] via [org.koin.mp.KoinPlatform.getKoin], which only ever
 * reads the global context — the same one [com.togetherly.app.di.initKoin] starts in production.
 * An isolated [org.koin.dsl.koinApplication] instance is invisible to it and fails to resolve
 * [TogetherlyDatabase] the moment anything needs a [android.content.Context]. [stopKoin] in
 * [tearDown] is essential, not just tidiness: without it this test leaves the global context
 * started for every later test in the same instrumentation process.
 *
 * This calls [appModules] directly, not [com.togetherly.app.di.initKoin] — so
 * [com.togetherly.data.purchase.RevenueCatConfigurator.configure] never actually runs here, and a
 * fake [RevenueCatApiKeyProvider] is supplied purely so the graph *resolves*; it proves wiring,
 * never real RevenueCat behavior (see `RevenueCatEntitlementRepositoryTest`, `commonTest`, for that).
 *
 * [CompletionRepository] and [JourneyRepository] joined [FamilyRepository]/[DailyQuestRepository]/
 * [SavedQuestRepository] here in Step 6.5, along with the [readyUseCaseModule] use cases that
 * needed them. [QuestSessionTransaction] and [FamilyDataCleaner] joined in Step 6.6.
 *
 * [OnboardingViewModel]/[TodayViewModel] (Step 7.6) joined once they had a complete production
 * dependency graph — [CreateFamilyProfile] and [FamilyRepository] were already here. There is no
 * separate Koin entry to verify for "the error mapper": [com.togetherly.core.ui.toUiText] is a
 * stateless top-level function, not a Koin-managed dependency, so it needs no resolution proof
 * beyond its own unit test ([com.togetherly.core.ui.AppErrorMapperTest], `commonTest`).
 *
 * [GetOrSelectDailyQuest]/[RerollDailyQuest] joined in Step 8.1 once
 * [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy] gave them a
 * complete production dependency graph.
 *
 * The `domain.explore.usecase` use cases and [ExploreViewModel] joined in Step 12.1 — every one of
 * them resolves entirely from repositories/policies already proven above (no new database-backed
 * repository was introduced for Explore itself).
 */
@RunWith(AndroidJUnit4::class)
internal class ProductionDatabaseKoinGraphTest {

    private var koinApp: KoinApplication? = null
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        koinApp?.let { app ->
            runCatching { app.koin.get<TogetherlyDatabase>().close() }
        }
        stopKoin()
        context.deleteDatabase("togetherly.db")
    }

    @Test
    fun resolvesRoomBackedRepositoriesAndReadyUseCasesFromTheProductionGraph() {
        val contextModule = module { single<Context> { context } }
        val revenueCatKeyModule = module { single<RevenueCatApiKeyProvider> { RevenueCatApiKeyProvider { null } } }

        val application = startKoin {
            modules(
                appModules(AppConfiguration(applicationName = "Togetherly", debug = true)) + contextModule + revenueCatKeyModule,
            )
        }
        koinApp = application

        application.koin.get<FamilyRepository>()
        application.koin.get<DailyQuestRepository>()
        application.koin.get<SavedQuestRepository>()
        application.koin.get<CompletionRepository>()
        application.koin.get<JourneyRepository>()
        application.koin.get<QuestSessionTransaction>()
        application.koin.get<FamilyDataCleaner>()
        application.koin.get<CreateFamilyProfile>()
        application.koin.get<UpdateFamilyProfile>()
        application.koin.get<DeleteAllFamilyData>()
        application.koin.get<SetQuestSaved>()
        application.koin.get<StartQuest>()
        application.koin.get<PrepareQuestStartUseCase>()
        application.koin.get<ReplaceActiveQuestSession>()
        application.koin.get<CompleteQuest>()
        application.koin.get<DeleteCompletion>()
        application.koin.get<GetJourneySummary>()
        application.koin.get<QuestTimerPolicy>()
        application.koin.get<QuestCountdownEngine>()
        application.koin.get<LoadQuestMode>()
        application.koin.get<AbandonQuest>()
        application.koin.get<GetOrSelectDailyQuest>()
        application.koin.get<SelectDailyQuestForContext>()
        application.koin.get<RerollDailyQuest>()
        application.koin.get<AppClock>()
        application.koin.get<IdGenerator>()
        application.koin.get<OnboardingViewModel>()
        application.koin.get<TodayViewModel>()
        application.koin.get<BootstrapViewModel>()
        application.koin.get<QuestDetailViewModel> { parametersOf(QuestId("quest-1")) }
        application.koin.get<QuestModeViewModel> { parametersOf(CompletionId("completion-1")) }
        application.koin.get<QuestFeedbackController>()
        application.koin.get<ResolveCompletionTransition>()
        application.koin.get<CompletionCelebrationViewModel> { parametersOf(CompletionId("completion-1")) }
        application.koin.get<PrivateMediaStorage>()
        application.koin.get<PrivateMediaCommitter>()
        application.koin.get<VoiceRecorder>()
        application.koin.get<VoicePlaybackController>()
        application.koin.get<SaveCompletionMemory>()
        application.koin.get<DiscardCompletionMemoryDraft>()
        application.koin.get<EntitlementRepository>()
        application.koin.get<RefreshFamilyAccess>()
        application.koin.get<PurchaseFamilyPlus>()
        application.koin.get<RestoreFamilyPlus>()
        application.koin.get<FamilyPlusPaywallViewModel> { parametersOf(PaywallContext.FAMILY_PLUS_MANAGEMENT) }
        application.koin.get<FamilyPlusManagementViewModel>()
        application.koin.get<ObserveExploreCatalogueUseCase>()
        application.koin.get<SearchQuestsUseCase>()
        application.koin.get<FilterQuestsUseCase>()
        application.koin.get<GetQuestPackUseCase>()
        application.koin.get<ObserveSavedQuestIdsUseCase>()
        application.koin.get<ObserveSavedQuestsUseCase>()
        application.koin.get<ToggleSavedQuestUseCase>()
        application.koin.get<EvaluateQuestAccessUseCase>()
        application.koin.get<EvaluatePackAccessUseCase>()
        application.koin.get<ExploreFilterStore>()
        application.koin.get<ExploreViewModel>()
        application.koin.get<ExploreFiltersViewModel>()
        application.koin.get<SavedViewModel>()
        application.koin.get<PackDetailsViewModel> { parametersOf(QuestPackId("proof-pack")) }
    }
}
