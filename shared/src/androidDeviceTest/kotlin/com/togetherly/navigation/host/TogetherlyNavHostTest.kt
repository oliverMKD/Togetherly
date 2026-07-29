package com.togetherly.navigation.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.notification.FakeReminderScheduler
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.core.result.DataResult
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
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.recommendation.FakeQuestRecommendationPolicy
import com.togetherly.domain.recommendation.QuestRecommendationResult
import com.togetherly.domain.recommendation.RecommendationConfig
import com.togetherly.domain.recommendation.RecommendationHistoryBuilder
import com.togetherly.domain.saved.repository.FakeSavedQuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.usecase.SetQuestSaved
import com.togetherly.feature.onboarding.presentation.OnboardingViewModel
import com.togetherly.feature.today.presentation.TodayViewModel
import com.togetherly.data.media.PendingMediaOrphanCleaner
import com.togetherly.integration.testFamilyProfile
import com.togetherly.navigation.state.BootstrapViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/**
 * Every test wraps [TogetherlyNavHost] in its own isolated [KoinApplication] instance — scoped to
 * that composable subtree only, never the global Koin context — providing just [presentationModule]
 * plus a [FamilyRepository] test double, rather than resolving the real, database-backed
 * production graph (`appModules()`) a pure navigation test has no need for.
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlyNavHostTest {

    private fun koinTestModule(repository: FamilyRepository): org.koin.core.module.Module {
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
        val pendingMediaOrphanCleaner = object : PendingMediaOrphanCleaner {
            override suspend fun deleteExpiredPending(now: Instant, thresholdAge: kotlin.time.Duration) =
                DataResult.Success(0)
        }

        return module {
            single { repository }
            single<AppClock> { clock }
            single<AppTimeZoneProvider> { timeZoneProvider }
            single<IdGenerator> { SequentialIdGenerator(prefix = "family") }
            single<SavedQuestRepository> { savedQuestRepository }
            single<CompletionRepository> { completionRepository }
            single<PendingMediaOrphanCleaner> { pendingMediaOrphanCleaner }
            single<ReminderScheduler> { FakeReminderScheduler() }
            factory { BootstrapViewModel(get(), get(), get(), get()) }
            factory { CreateFamilyProfile(get(), get(), get()) }
            factory { OnboardingViewModel(get()) }
            factory {
                GetOrSelectDailyQuest(
                    repository, questRepository, dailyQuestRepository, recommendationPolicy,
                    historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory {
                SelectDailyQuestForContext(
                    repository, questRepository, dailyQuestRepository, transaction,
                    recommendationPolicy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory {
                RerollDailyQuest(
                    repository, questRepository, dailyQuestRepository, transaction,
                    recommendationPolicy, historyBuilder, allowancePolicy, entitlementRepository, questAccessPolicy, clock,
                )
            }
            factory { SetQuestSaved(savedQuestRepository, questRepository, clock) }
            factory { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun missingProfileRoutesToOnboarding() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule(FakeFamilyRepository())) }) {
                TogetherlyTheme { TogetherlyNavHost() }
            }
        }
        waitForIdle()

        onNodeWithText("One small family adventure every day.").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun existingProfileRoutesToMain() = runComposeUiTest {
        val repository = FakeFamilyRepository()
        setContent {
            KoinApplication(application = { modules(koinTestModule(repository)) }) {
                TogetherlyTheme { TogetherlyNavHost() }
            }
        }
        // Save the profile from inside the composition's own coroutine context isn't needed —
        // FakeFamilyRepository.observeProfile() is a hot StateFlow, so seeding it before the
        // ViewModel subscribes (i.e. before waitForIdle) is equivalent to "a profile already
        // exists" at bootstrap time.
        runBlocking { repository.saveProfile(testFamilyProfile()) }
        waitForIdle()

        onNodeWithText("Reveal quest").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadingIsShownBeforeAnyOtherRouteEverAppears() = runComposeUiTest {
        // A repository whose Flow never emits keeps BootstrapViewModel in BootstrapUiState.Loading
        // indefinitely — proving nothing else renders while genuinely stuck loading.
        val neverEmitting = object : FamilyRepository {
            override fun observeProfile(): Flow<DataResult<FamilyProfile?>> = flowOf()
            override suspend fun getProfile(): DataResult<FamilyProfile?> = DataResult.Success(null)
            override suspend fun saveProfile(profile: FamilyProfile): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun deleteProfile(): DataResult<Unit> = DataResult.Success(Unit)
        }

        setContent {
            KoinApplication(application = { modules(koinTestModule(neverEmitting)) }) {
                TogetherlyTheme { TogetherlyNavHost() }
            }
        }
        waitForIdle()

        onNodeWithText("One small family adventure every day.").assertDoesNotExist()
        onNodeWithText("Reveal quest").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun retryShowsSuccessAfterBootstrapError() = runComposeUiTest {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val repository = ObserveOnceFamilyRepository(
            mutableListOf(DataResult.Error(error), DataResult.Success(null)),
        )

        setContent {
            KoinApplication(application = { modules(koinTestModule(repository)) }) {
                TogetherlyTheme { TogetherlyNavHost() }
            }
        }
        waitForIdle()

        onNodeWithText("Retry").performClick()
        waitForIdle()

        onNodeWithText("One small family adventure every day.").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onboardingNeverShowsBottomNavigation() = runComposeUiTest {
        setContent {
            KoinApplication(application = { modules(koinTestModule(FakeFamilyRepository())) }) {
                TogetherlyTheme { TogetherlyNavHost() }
            }
        }
        waitForIdle()

        onNodeWithText("One small family adventure every day.").assertExists()
        onNodeWithText("Explore").assertDoesNotExist()
        onNodeWithText("Journey").assertDoesNotExist()
        onNodeWithText("Family").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backFromOnboardingFirstStepExitsSinceBootstrapWasAlreadyRemoved() = runComposeUiTest {
        lateinit var navController: NavHostController

        setContent {
            KoinApplication(application = { modules(koinTestModule(FakeFamilyRepository())) }) {
                TogetherlyTheme {
                    navController = rememberNavController()
                    TogetherlyNavHost(navController = navController)
                }
            }
        }
        waitForIdle()
        onNodeWithText("One small family adventure every day.").assertExists()

        // Onboarding's own internal Back (its top-bar icon) on the first internal step
        // (WELCOME) emits OnboardingEvent.NavigateBack, which TogetherlyNavHost turns into a
        // plain navController.popBackStack() — see its own KDoc.
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()

        val poppedAgain = navController.popBackStack()
        assert(!poppedAgain) {
            "Back from onboarding's first step should have nothing left to pop (Bootstrap was already removed) — the platform's own exit/dismiss behavior takes over from here."
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completingOnboardingReplacesTheGraphSoBackCannotReturnToIt() = runComposeUiTest {
        lateinit var navController: NavHostController

        setContent {
            KoinApplication(application = { modules(koinTestModule(FakeFamilyRepository())) }) {
                TogetherlyTheme {
                    navController = rememberNavController()
                    TogetherlyNavHost(navController = navController)
                }
            }
        }
        waitForIdle()
        completeOnboarding()

        onNodeWithText("Reveal quest").assertExists()

        val poppedBackToOnboarding = navController.popBackStack()

        assert(!poppedBackToOnboarding) {
            "Onboarding must be fully removed from the back stack once finished — Back from Main should have nothing to return to."
        }
        onNodeWithText("One small family adventure every day.").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dataDeletionReturnsToOnboardingOnNextBootstrapObservation() = runComposeUiTest {
        val repository = FakeFamilyRepository()
        runBlocking { repository.saveProfile(testFamilyProfile()) }
        var relaunched by mutableStateOf(false)

        setContent {
            KoinApplication(application = { modules(koinTestModule(repository)) }) {
                TogetherlyTheme {
                    // A fresh TogetherlyNavHost() call site per branch — toggling `relaunched`
                    // disposes the old composition (old NavController/Bootstrap ViewModel) and
                    // creates a brand new one, simulating the app being killed and reopened.
                    if (relaunched) {
                        TogetherlyNavHost()
                    } else {
                        TogetherlyNavHost()
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText("Reveal quest").assertExists()

        runBlocking { repository.deleteProfile() }
        relaunched = true
        waitForIdle()

        onNodeWithText("One small family adventure every day.").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeUiTest.completeOnboarding() {
        onNodeWithText("Create our family").performClick()
        waitForIdle()
        onNodeWithText("Skip for now").performClick()
        waitForIdle()
        onNodeWithText("6–8").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Create").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("10 minutes").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Not now").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Start our first adventure").performClick()
        waitForIdle()
    }
}

/**
 * A [FamilyRepository] double whose [observeProfile] returns a *fresh*, single-value Flow on
 * every call and never updates on its own, mirroring the real `RoomFamilyRepository`'s Flow
 * terminating after an error. See the identically-named private class in
 * `BootstrapViewModelTest` (`commonTest`) — duplicated here only because that one is `private` to
 * its own file, not because of any source-set visibility boundary (plain public/internal
 * `commonTest` symbols, like [FakeFamilyRepository] and [testFamilyProfile] above, are reachable
 * from here directly).
 */
private class ObserveOnceFamilyRepository(
    private val results: MutableList<DataResult<FamilyProfile?>>,
) : FamilyRepository {

    override fun observeProfile(): Flow<DataResult<FamilyProfile?>> = flowOf(results.removeAt(0))

    override suspend fun getProfile(): DataResult<FamilyProfile?> = results.first()

    override suspend fun saveProfile(profile: FamilyProfile): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun deleteProfile(): DataResult<Unit> = DataResult.Success(Unit)
}
