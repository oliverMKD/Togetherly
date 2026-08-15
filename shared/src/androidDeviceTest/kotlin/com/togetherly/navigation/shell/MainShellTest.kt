package com.togetherly.navigation.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.daily.DefaultRerollAllowancePolicy
import com.togetherly.domain.daily.RerollAllowancePolicy
import com.togetherly.domain.daily.repository.FakeDailyQuestRepository
import com.togetherly.domain.daily.repository.FakeDailyQuestTransaction
import com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest
import com.togetherly.domain.daily.usecase.RerollDailyQuest
import com.togetherly.domain.daily.usecase.SelectDailyQuestForContext
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.explore.usecase.EvaluatePackAccessUseCase
import com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase
import com.togetherly.domain.explore.usecase.FilterQuestsUseCase
import com.togetherly.domain.explore.usecase.ObserveExploreCatalogueUseCase
import com.togetherly.domain.explore.usecase.ObserveSavedQuestIdsUseCase
import com.togetherly.domain.explore.usecase.SearchQuestsUseCase
import com.togetherly.domain.explore.usecase.ToggleSavedQuestUseCase
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.purchase.repository.FakeCustomerAttributesRepository
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.explore.presentation.ExploreFilterStore
import com.togetherly.feature.explore.presentation.ExploreViewModel
import com.togetherly.feature.familyplus.presentation.FamilyPlusManagementViewModel
import com.togetherly.feature.today.presentation.TodayViewModel
import com.togetherly.navigation.shell.RequestedTabStore
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/** [validFamilyQuest]'s own default title — the one quest [MainShellTest.koinTestModule] seeds the fake catalogue with, and so what Explore's own minimal Step 12.1 content renders as its only quest row. */
private const val EXPLORE_QUEST_TITLE = "Backyard Scavenger Hunt"

/**
 * [MainShell] itself has no Koin dependency (unlike [com.togetherly.navigation.host.BootstrapScreen]),
 * so these render it directly with no test Koin module — except for its Today, Explore, and Family
 * tabs, which now host the real [com.togetherly.feature.today.presentation.TodayRoute]/
 * [com.togetherly.feature.explore.presentation.ExploreRoute]/[com.togetherly.feature.familyplus.presentation.FamilyPlusManagementRoute]
 * and therefore need a full [TodayViewModel]/[ExploreViewModel]/[FamilyPlusManagementViewModel]
 * dependency graph, purely to satisfy `koinViewModel<...>()`. The quest catalogue/recommendation
 * plumbing here is faked to always resolve successfully — these tests care about tab switching,
 * not each tab's own content, so "Reveal quest" (Today's Mystery-state primary action),
 * [EXPLORE_QUEST_TITLE] (the one fake quest Explore's own minimal Step 12.1 content lists), and
 * "Family Plus" (the management screen's free-view title) each stand in for "Main was reached" the
 * way "Welcome to Togetherly" used to before Today got a real ViewModel (Step 8.3).
 */
@RunWith(AndroidJUnit4::class)
internal class MainShellTest {

    private fun koinTestModule(): org.koin.core.module.Module {
        val familyRepository: FamilyRepository = FakeFamilyRepository()
        val quest = validFamilyQuest()
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val dailyQuestRepository = FakeDailyQuestRepository()
        val completionRepository = FakeCompletionRepository()
        val savedQuestRepository: SavedQuestRepository = FakeSavedQuestRepository()
        val transaction = FakeDailyQuestTransaction(dailyQuestRepository)
        val clock = TestAppClock(NOW)
        val recommendationPolicy = FakeQuestRecommendationPolicy(
            result = QuestRecommendationResult.Success(quest, score = 10, reasons = emptyList()),
        )
        val historyBuilder = RecommendationHistoryBuilder(completionRepository, dailyQuestRepository, clock, RecommendationConfig.DEFAULT)
        val allowancePolicy: RerollAllowancePolicy = DefaultRerollAllowancePolicy()
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val questAccessPolicy = QuestAccessPolicy()
        val timeZoneProvider = object : AppTimeZoneProvider {
            override fun current(): TimeZone = TimeZone.UTC
        }
        val productAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val customerAttributesRepository = FakeCustomerAttributesRepository()

        return module {
            single<FamilyRepository> { familyRepository }
            single<AppClock> { clock }
            single<AppTimeZoneProvider> { timeZoneProvider }
            single<SavedQuestRepository> { savedQuestRepository }
            single<CompletionRepository> { completionRepository }
            single { productAnalytics }
            single { customerAttributesRepository }
            factory {
                GetOrSelectDailyQuest(
                    familyRepository, questRepository, dailyQuestRepository, recommendationPolicy,
                    historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory {
                SelectDailyQuestForContext(
                    familyRepository, questRepository, dailyQuestRepository, transaction,
                    recommendationPolicy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory {
                RerollDailyQuest(
                    familyRepository, questRepository, dailyQuestRepository, transaction,
                    recommendationPolicy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory { SetQuestSaved(savedQuestRepository, questRepository, clock) }
            factory { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            single { RequestedTabStore() }
            single<EntitlementRepository> { entitlementRepository }
            factory { RestoreFamilyPlus(entitlementRepository) }
            factory { FamilyPlusManagementViewModel(entitlementRepository, get(), get(), get()) }
            factory { ObserveExploreCatalogueUseCase(questRepository) }
            factory { SearchQuestsUseCase() }
            factory { FilterQuestsUseCase() }
            factory { ObserveSavedQuestIdsUseCase(savedQuestRepository) }
            factory { ToggleSavedQuestUseCase(savedQuestRepository, get()) }
            factory { EvaluateQuestAccessUseCase(entitlementRepository, questAccessPolicy, clock) }
            factory { EvaluatePackAccessUseCase(entitlementRepository, questAccessPolicy, clock) }
            single { ExploreFilterStore() }
            factory { ExploreViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun startsOnTodayWithAllFourDestinationsInTheBottomBar() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule()) }) {
                TogetherlyTheme { MainShell() }
            }
        }
        waitForIdle()

        onNodeWithText("Today").assertExists()
        onNodeWithText("Explore").assertExists()
        onNodeWithText("Journey").assertExists()
        onNodeWithText("Family").assertExists()
        onNodeWithText("Reveal quest").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectingEachDestinationChangesTheDisplayedContent() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule()) }) {
                TogetherlyTheme { MainShell() }
            }
        }
        waitForIdle()

        onNodeWithText("Explore").performClick()
        waitForIdle()
        onNodeWithText(EXPLORE_QUEST_TITLE).assertExists()
        onNodeWithText("Reveal quest").assertDoesNotExist()

        onNodeWithText("Journey").performClick()
        waitForIdle()
        onNodeWithText("Journey is coming soon").assertExists()
        onNodeWithText(EXPLORE_QUEST_TITLE).assertDoesNotExist()

        onNodeWithText("Family").performClick()
        waitForIdle()
        onNodeWithText("Family Plus").assertExists()
        onNodeWithText("Journey is coming soon").assertDoesNotExist()

        onNodeWithText("Today").performClick()
        waitForIdle()
        onNodeWithText("Reveal quest").assertExists()
        onNodeWithText("Family Plus").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reselectingTheActiveDestinationStaysOnTheSameContent() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule()) }) {
                TogetherlyTheme { MainShell() }
            }
        }
        waitForIdle()

        onNodeWithText("Today").performClick()
        waitForIdle()

        onNodeWithText("Reveal quest").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun switchingTabsAndBackPreservesEachTabsOwnState() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule()) }) {
                TogetherlyTheme { MainShell() }
            }
        }
        waitForIdle()

        onNodeWithText("Explore").performClick()
        waitForIdle()
        onNodeWithText("Today").performClick()
        waitForIdle()
        onNodeWithText("Explore").performClick()
        waitForIdle()

        // Only one matching node exists — switching away and back didn't push a second copy of
        // the destination onto the back stack (launchSingleTop + restoreState).
        onNodeWithText(EXPLORE_QUEST_TITLE).assertExists()
    }
}
