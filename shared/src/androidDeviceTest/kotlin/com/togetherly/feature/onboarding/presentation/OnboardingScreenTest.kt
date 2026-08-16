package com.togetherly.feature.onboarding.presentation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.purchase.repository.FakeCustomerAttributesRepository
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingUiState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/**
 * Two testing styles, deliberately: [OnboardingScreen] renders directly against a hand-built
 * [OnboardingUiState] for pure "does the right thing show" checks (fast, no ViewModel); the real
 * [OnboardingRoute] + [OnboardingViewModel] (backed by the same [FakeFamilyRepository] the
 * ViewModel's own unit tests use) drives the flow-dependent ones, where state genuinely has to
 * change across several user interactions.
 */
@RunWith(AndroidJUnit4::class)
internal class OnboardingScreenTest {

    private fun viewModel(repository: FakeFamilyRepository = FakeFamilyRepository()) = OnboardingViewModel(
        CreateFamilyProfile(
            familyRepository = repository,
            clock = TestAppClock(NOW),
            idGenerator = SequentialIdGenerator(prefix = "family"),
        ),
        FakeProductAnalytics().apply { setCollectionEnabled(true) },
        FakeCustomerAttributesRepository(),
    )

    // -- Stateless snapshot checks -----------------------------------------------------------

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun multipleAgeBandsCanBeSelected() = runComposeUiTest {
        val actions = mutableListOf<OnboardingAction>()
        setContent {
            TogetherlyTheme {
                OnboardingScreen(state = OnboardingUiState(step = OnboardingStep.AGE_BANDS), onAction = actions::add)
            }
        }

        onNodeWithText("6–8").performClick()
        onNodeWithText("9–11").performClick()

        assertEquals(
            listOf<OnboardingAction>(OnboardingAction.AgeBandToggled(AgeBand.AGE_6_TO_8), OnboardingAction.AgeBandToggled(AgeBand.AGE_9_TO_11)),
            actions,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun interestCanBeSelected() = runComposeUiTest {
        val actions = mutableListOf<OnboardingAction>()
        setContent {
            TogetherlyTheme {
                OnboardingScreen(state = OnboardingUiState(step = OnboardingStep.INTERESTS), onAction = actions::add)
            }
        }

        onNodeWithText("Create").performClick()

        assertEquals(listOf<OnboardingAction>(OnboardingAction.InterestToggled(QuestCategory.CREATE)), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun missingChoiceShowsValidationError() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingScreen(
                    state = OnboardingUiState(
                        step = OnboardingStep.AGE_BANDS,
                        validationErrors = persistentMapOf(
                            OnboardingField.AGE_BANDS to UiText.Dynamic("Choose at least one age group."),
                        ),
                    ),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Choose at least one age group.").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reminderDisabledShowsNoDaysOrTimeControls() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingScreen(state = OnboardingUiState(step = OnboardingStep.REMINDER, reminderEnabled = false), onAction = {})
            }
        }

        onNodeWithText("Choose time").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reminderEnabledShowsDaysAndTimeControls() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingScreen(state = OnboardingUiState(step = OnboardingStep.REMINDER, reminderEnabled = true), onAction = {})
            }
        }

        onNodeWithText("Mon").assertExists()
        onNodeWithText("Choose time").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reviewDisplaysAllSelections() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingScreen(
                    state = OnboardingUiState(
                        step = OnboardingStep.REVIEW,
                        familyName = "Team Firefly",
                        selectedAgeBands = persistentSetOf(AgeBand.AGE_6_TO_8),
                        selectedInterests = persistentSetOf(QuestCategory.CREATE),
                        selectedDurations = persistentSetOf(DurationBand.TEN_MINUTES),
                        locationPreference = LocationPreference.OUTDOOR,
                        preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
                        reminderEnabled = false,
                    ),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Team Firefly").assertExists()
        onNodeWithText("6–8").assertExists()
        onNodeWithText("Create").assertExists()
        onNodeWithText("10 minutes").assertExists()
        onNodeWithText("Outdoors").assertExists()
        onNodeWithText("Simple materials are fine").assertExists()
        onNodeWithText("No reminder").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessibilityLabelsAndSelectedSemanticsArePresent() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingScreen(
                    state = OnboardingUiState(step = OnboardingStep.AGE_BANDS, selectedAgeBands = persistentSetOf(AgeBand.AGE_6_TO_8)),
                    onAction = {},
                )
            }
        }

        onNodeWithText("6–8").assertIsSelected()
        onNodeWithText("9–11").assertIsNotSelected()
        onNodeWithContentDescription("Back").assertExists()
    }

    // -- Flow-dependent checks (real ViewModel) -----------------------------------------------

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun entireHappyPathCreatesFamily() = runComposeUiTest {
        var familyCreated = false
        setContent {
            TogetherlyTheme {
                OnboardingRoute(viewModel = viewModel(), onNavigateBack = {}, onFamilyCreated = { familyCreated = true })
            }
        }
        waitForIdle()

        onNodeWithText("Create our family").performClick() // Welcome -> Family name
        waitForIdle()
        onNodeWithText("Skip for now").performClick() // Family name -> Age bands
        waitForIdle()
        onNodeWithText("6–8").performClick()
        onNodeWithText("Continue").performClick() // Age bands -> Interests
        waitForIdle()
        onNodeWithText("Create").performClick()
        onNodeWithText("Continue").performClick() // Interests -> Preferences
        waitForIdle()
        onNodeWithText("10 minutes").performClick()
        onNodeWithText("Continue").performClick() // Preferences -> Reminder
        waitForIdle()
        onNodeWithText("Not now").performClick()
        onNodeWithText("Continue").performClick() // Reminder -> Review
        waitForIdle()
        onNodeWithText("Start our first adventure").performClick()
        waitForIdle()

        assertTrue(familyCreated)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backPreservesData() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                OnboardingRoute(viewModel = viewModel(), onNavigateBack = {}, onFamilyCreated = {})
            }
        }
        waitForIdle()

        onNodeWithText("Create our family").performClick() // Welcome -> Family name
        waitForIdle()
        onNodeWithText("Family or team name").performTextInput("Riverside Crew")
        onNodeWithText("Continue").performClick() // Family name -> Age bands
        waitForIdle()
        onNodeWithText("6–8").performClick()

        onNodeWithContentDescription("Back").performClick() // Age bands -> Family name
        waitForIdle()

        onNodeWithText("Riverside Crew").assertExists()

        onNodeWithText("Continue").performClick() // Family name -> Age bands again
        waitForIdle()
        onNodeWithText("6–8").assertIsSelected()
    }

    // "Duplicate click while a save is in flight" is deliberately not tested here: under this
    // API, performClick() drains to full idle (including the whole fake save, since neither
    // CreateFamilyProfile nor FakeFamilyRepository ever genuinely suspend) before returning, so a
    // second performClick() right after can only ever observe a fresh, already-completed, re-
    // enabled button — never the mid-save state the name would suggest. Confirmed empirically:
    // even wrapping the repository with a real suspension point before saveProfile() didn't
    // change the outcome, because the click action itself drains that suspension too. The guard
    // OnboardingViewModel.onCreateFamily() relies on (isSaving checked synchronously before the
    // save coroutine launches) is what actually matters, and it's already verified precisely,
    // with real coroutine-dispatch control, by OnboardingViewModelTest.duplicateSavingIsIgnored.

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun saveErrorShowsRetryAndRecovers() = runComposeUiTest {
        val repository = FakeFamilyRepository()
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))
        var familyCreated = false
        setContent {
            TogetherlyTheme {
                OnboardingRoute(viewModel = viewModel(repository), onNavigateBack = {}, onFamilyCreated = { familyCreated = true })
            }
        }
        waitForIdle()
        advanceToReview()

        onNodeWithText("Start our first adventure").performClick()
        waitForIdle()

        onNodeWithText("Retry").assertExists()
        assertFalse(familyCreated)

        onNodeWithText("Retry").performClick()
        waitForIdle()

        assertTrue(familyCreated)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeUiTest.advanceToReview() {
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
    }
}
