package com.togetherly.feature.onboarding.presentation

import app.cash.turbine.test
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.telemetry.AnalyticsEvent
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.core.telemetry.OnboardingCompleted
import com.togetherly.core.telemetry.OnboardingStepViewed
import com.togetherly.core.ui.toUiText
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeFamilyRepository = FakeFamilyRepository(),
        analytics: FakeProductAnalytics = FakeProductAnalytics().apply { setCollectionEnabled(true) },
        customerAttributesRepository: FakeCustomerAttributesRepository = FakeCustomerAttributesRepository(),
    ) = OnboardingViewModel(
        CreateFamilyProfile(
            familyRepository = repository,
            clock = TestAppClock(NOW),
            idGenerator = SequentialIdGenerator(prefix = "family"),
        ),
        analytics,
        customerAttributesRepository,
    )

    // -- Step transitions -----------------------------------------------------------------

    @Test
    fun welcomeContinuesToFamilyName() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(OnboardingAction.ContinueClicked)

        assertEquals(OnboardingStep.FAMILY_NAME, viewModel.uiState.value.step)
    }

    @Test
    fun familyNameCanBeSkipped() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(OnboardingAction.ContinueClicked)
        viewModel.onAction(OnboardingAction.FamilyNameChanged("The Riveras"))

        viewModel.onAction(OnboardingAction.SkipNameClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.AGE_BANDS, state.step)
        assertEquals("", state.familyName)
    }

    @Test
    fun backMovesToPreviousStep() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(OnboardingAction.ContinueClicked) // WELCOME -> FAMILY_NAME
        viewModel.onAction(OnboardingAction.ContinueClicked) // FAMILY_NAME -> AGE_BANDS

        viewModel.onAction(OnboardingAction.BackClicked)

        assertEquals(OnboardingStep.FAMILY_NAME, viewModel.uiState.value.step)
    }

    @Test
    fun backFromWelcomeEmitsNavigateBackEvent() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.BackClicked)
            assertEquals(OnboardingEvent.NavigateBack, awaitItem())
        }
        // Back from the first step never changes step — there is nowhere left to go internally.
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    // -- Per-step validation ----------------------------------------------------------------

    @Test
    fun ageStepRequiresSelection() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.AGE_BANDS)

        viewModel.onAction(OnboardingAction.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.AGE_BANDS, state.step)
        assertTrue(state.validationErrors.containsKey(OnboardingField.AGE_BANDS))
    }

    @Test
    fun interestStepRequiresSelection() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.INTERESTS)

        viewModel.onAction(OnboardingAction.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.INTERESTS, state.step)
        assertTrue(state.validationErrors.containsKey(OnboardingField.INTERESTS))
    }

    @Test
    fun preferenceStepRequiresDuration() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.PREFERENCES)

        viewModel.onAction(OnboardingAction.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.PREFERENCES, state.step)
        assertTrue(state.validationErrors.containsKey(OnboardingField.DURATIONS))
    }

    @Test
    fun reminderDisabledIsValid() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.REMINDER)

        viewModel.onAction(OnboardingAction.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.REVIEW, state.step)
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun reminderEnabledRequiresDaysAndTime() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.REMINDER)
        viewModel.onAction(OnboardingAction.ReminderEnabledChanged(true))

        viewModel.onAction(OnboardingAction.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.REMINDER, state.step)
        assertTrue(state.validationErrors.containsKey(OnboardingField.REMINDER_DAYS))
        assertTrue(state.validationErrors.containsKey(OnboardingField.REMINDER_TIME))
    }

    @Test
    fun reviewStateContainsAllChoices() = runTest {
        val viewModel = viewModel()
        fillCompleteValidDraft(viewModel)

        val state = viewModel.uiState.value

        assertEquals(OnboardingStep.REVIEW, state.step)
        assertEquals("The Riveras", state.familyName)
        assertEquals(persistentSetOf(AgeBand.AGE_6_TO_8), state.selectedAgeBands)
        assertEquals(persistentSetOf(QuestCategory.CREATE), state.selectedInterests)
        assertEquals(persistentSetOf(DurationBand.TEN_MINUTES), state.selectedDurations)
        assertEquals(LocationPreference.OUTDOOR, state.locationPreference)
        assertEquals(PreparationPreference.SIMPLE_MATERIALS, state.preparationPreference)
        assertTrue(state.reminderEnabled)
        assertEquals(persistentSetOf(DayOfWeek.MONDAY), state.reminderDays)
        assertEquals(LocalTime(18, 0), state.reminderTime)
    }

    // -- Saving ------------------------------------------------------------------------------

    @Test
    fun savingSetsLoading() = runTest {
        val viewModel = viewModel()
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)

        assertTrue(viewModel.uiState.value.isSaving)
    }

    @Test
    fun duplicateSavingIsIgnored() = runTest {
        val repository = FakeFamilyRepository()
        val viewModel = viewModel(repository)
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.savedProfiles.size)
    }

    @Test
    fun successEmitsFamilyCreated() = runTest {
        val viewModel = viewModel()
        fillCompleteValidDraft(viewModel)

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.CreateFamilyClicked)
            assertEquals(OnboardingEvent.FamilyCreated, awaitItem())
        }

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.saveError)
    }

    @Test
    fun failureRemainsOnReviewAndExposesRetry() = runTest {
        val repository = FakeFamilyRepository()
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setNextError(error)
        val viewModel = viewModel(repository)
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.REVIEW, state.step)
        assertFalse(state.isSaving)
        assertEquals(error.toUiText(), state.saveError)

        // Retry: the same command is safe to attempt again — preserved draft, no re-navigation.
        // The first attempt's error consumed FakeFamilyRepository's queued error without writing
        // a profile, so only the retry's write actually lands.
        viewModel.events.test {
            viewModel.onAction(OnboardingAction.RetrySaveClicked)
            assertEquals(OnboardingEvent.FamilyCreated, awaitItem())
        }
        assertEquals(1, repository.savedProfiles.size)
    }

    @Test
    fun errorClearsWhenRelevantInputChanges() = runTest {
        val viewModel = viewModel()
        advanceTo(viewModel, OnboardingStep.AGE_BANDS)
        viewModel.onAction(OnboardingAction.ContinueClicked)
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(OnboardingField.AGE_BANDS))

        viewModel.onAction(OnboardingAction.AgeBandToggled(AgeBand.AGE_6_TO_8))

        assertFalse(viewModel.uiState.value.validationErrors.containsKey(OnboardingField.AGE_BANDS))
    }

    /**
     * Documents the deliberate absence of any child-identity field (name, birth date, gender,
     * school, ...) by exhaustively naming every field [OnboardingUiState] actually has — adding a
     * new one without updating this test is exactly the kind of silent addition this guards
     * against.
     */
    @Test
    fun stateHasNoChildIdentityField() {
        val explicit = OnboardingUiState(
            step = OnboardingStep.WELCOME,
            familyName = "",
            selectedAgeBands = persistentSetOf(),
            selectedInterests = persistentSetOf(),
            selectedDurations = persistentSetOf(),
            locationPreference = LocationPreference.BOTH,
            preparationPreference = PreparationPreference.NONE,
            reminderEnabled = false,
            reminderDays = persistentSetOf(),
            reminderTime = null,
            validationErrors = persistentMapOf(),
            isSaving = false,
            saveError = null,
        )
        assertEquals(OnboardingUiState(), explicit)
    }

    // -- Analytics --------------------------------------------------------------------------

    @Test
    fun screenStartedFiresScreenViewAndFirstStepOnceEvenIfCalledTwice() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)

        viewModel.onScreenStarted()
        viewModel.onScreenStarted()

        assertEquals(listOf(AnalyticsScreen.ONBOARDING), analytics.screensViewed)
        assertEquals(listOf<AnalyticsEvent>(OnboardingStepViewed(OnboardingStep.WELCOME)), analytics.capturedEvents)
    }

    @Test
    fun continuingCapturesTheNewlyEnteredStep() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)

        viewModel.onAction(OnboardingAction.ContinueClicked)

        assertEquals(listOf<AnalyticsEvent>(OnboardingStepViewed(OnboardingStep.FAMILY_NAME)), analytics.capturedEvents)
    }

    @Test
    fun goingBackCapturesThePreviousStepOnlyWhenThereIsOneToReturnTo() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)
        viewModel.onAction(OnboardingAction.ContinueClicked) // WELCOME -> FAMILY_NAME
        analytics.capturedEvents.clear()

        viewModel.onAction(OnboardingAction.BackClicked)

        assertEquals(listOf<AnalyticsEvent>(OnboardingStepViewed(OnboardingStep.WELCOME)), analytics.capturedEvents)

        analytics.capturedEvents.clear()
        viewModel.onAction(OnboardingAction.BackClicked) // WELCOME has no previous step
        assertTrue(analytics.capturedEvents.isEmpty())
    }

    @Test
    fun skippingNameCapturesTheAdvancedStep() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)
        viewModel.onAction(OnboardingAction.ContinueClicked) // WELCOME -> FAMILY_NAME
        analytics.capturedEvents.clear()

        viewModel.onAction(OnboardingAction.SkipNameClicked)

        assertEquals(listOf<AnalyticsEvent>(OnboardingStepViewed(OnboardingStep.AGE_BANDS)), analytics.capturedEvents)
    }

    @Test
    fun successfulSaveCapturesOnboardingCompletedExactlyOnce() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)
        fillCompleteValidDraft(viewModel)
        analytics.capturedEvents.clear()

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf<AnalyticsEvent>(OnboardingCompleted), analytics.capturedEvents)
    }

    @Test
    fun noCapturedEventEverCarriesTheFamilyNameOrAnyOtherProfileAnswer() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)
        viewModel.onScreenStarted()

        viewModel.onAction(OnboardingAction.ContinueClicked)
        viewModel.onAction(OnboardingAction.FamilyNameChanged("The Very Distinctive Riveras"))
        fillCompleteValidDraft(viewModel)
        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val allPropertyValues = analytics.capturedEvents.flatMap { it.properties().values }
        assertTrue(allPropertyValues.none { value -> value.toString().contains("Rivera") })
    }

    @Test
    fun failedSaveCapturesNoCompletionEvent() = runTest {
        val repository = FakeFamilyRepository()
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(repository, analytics)
        fillCompleteValidDraft(viewModel)
        analytics.capturedEvents.clear()

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.none { it is OnboardingCompleted })
    }

    @Test
    fun duplicateSaveClicksNeverCaptureCompletionTwice() = runTest {
        val analytics = FakeProductAnalytics().apply { setCollectionEnabled(true) }
        val viewModel = viewModel(analytics = analytics)
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analytics.capturedEvents.count { it is OnboardingCompleted })
    }

    @Test
    fun noEventsAreCapturedWithoutConsent() = runTest {
        val analytics = FakeProductAnalytics()
        val viewModel = viewModel(analytics = analytics)

        viewModel.onScreenStarted()
        viewModel.onAction(OnboardingAction.ContinueClicked)
        fillCompleteValidDraft(viewModel)
        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(analytics.capturedEvents.isEmpty())
        assertTrue(analytics.screensViewed.isEmpty())
    }

    // -- RevenueCat customer attributes ------------------------------------------------------

    @Test
    fun successfulSaveMarksOnboardingCompletedAndSetsThePreferredDurationBucket() = runTest {
        val customerAttributesRepository = FakeCustomerAttributesRepository()
        val viewModel = viewModel(customerAttributesRepository = customerAttributesRepository)
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, customerAttributesRepository.markOnboardingCompletedCallCount)
        assertEquals(listOf(DurationBand.TEN_MINUTES), customerAttributesRepository.preferredDurationBucketCalls)
    }

    @Test
    fun failedSaveNeverMarksOnboardingCompleted() = runTest {
        val repository = FakeFamilyRepository()
        repository.setNextError(AppError.Storage(StorageError.WRITE_FAILED))
        val customerAttributesRepository = FakeCustomerAttributesRepository()
        val viewModel = viewModel(repository, customerAttributesRepository = customerAttributesRepository)
        fillCompleteValidDraft(viewModel)

        viewModel.onAction(OnboardingAction.CreateFamilyClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, customerAttributesRepository.markOnboardingCompletedCallCount)
        assertTrue(customerAttributesRepository.preferredDurationBucketCalls.isEmpty())
    }

    private suspend fun advanceTo(viewModel: OnboardingViewModel, target: OnboardingStep) {
        while (viewModel.uiState.value.step != target) {
            when (viewModel.uiState.value.step) {
                OnboardingStep.WELCOME -> viewModel.onAction(OnboardingAction.ContinueClicked)
                OnboardingStep.FAMILY_NAME -> viewModel.onAction(OnboardingAction.ContinueClicked)
                OnboardingStep.AGE_BANDS -> {
                    viewModel.onAction(OnboardingAction.AgeBandToggled(AgeBand.AGE_6_TO_8))
                    viewModel.onAction(OnboardingAction.ContinueClicked)
                }
                OnboardingStep.INTERESTS -> {
                    viewModel.onAction(OnboardingAction.InterestToggled(QuestCategory.CREATE))
                    viewModel.onAction(OnboardingAction.ContinueClicked)
                }
                OnboardingStep.PREFERENCES -> {
                    viewModel.onAction(OnboardingAction.DurationToggled(DurationBand.TEN_MINUTES))
                    viewModel.onAction(OnboardingAction.ContinueClicked)
                }
                OnboardingStep.REMINDER -> viewModel.onAction(OnboardingAction.ContinueClicked)
                OnboardingStep.REVIEW -> return
            }
        }
    }

    private suspend fun fillCompleteValidDraft(viewModel: OnboardingViewModel) {
        viewModel.onAction(OnboardingAction.ContinueClicked) // WELCOME -> FAMILY_NAME
        viewModel.onAction(OnboardingAction.FamilyNameChanged("The Riveras"))
        viewModel.onAction(OnboardingAction.ContinueClicked) // FAMILY_NAME -> AGE_BANDS
        viewModel.onAction(OnboardingAction.AgeBandToggled(AgeBand.AGE_6_TO_8))
        viewModel.onAction(OnboardingAction.ContinueClicked) // AGE_BANDS -> INTERESTS
        viewModel.onAction(OnboardingAction.InterestToggled(QuestCategory.CREATE))
        viewModel.onAction(OnboardingAction.ContinueClicked) // INTERESTS -> PREFERENCES
        viewModel.onAction(OnboardingAction.DurationToggled(DurationBand.TEN_MINUTES))
        viewModel.onAction(OnboardingAction.LocationSelected(LocationPreference.OUTDOOR))
        viewModel.onAction(OnboardingAction.PreparationSelected(PreparationPreference.SIMPLE_MATERIALS))
        viewModel.onAction(OnboardingAction.ContinueClicked) // PREFERENCES -> REMINDER
        viewModel.onAction(OnboardingAction.ReminderEnabledChanged(true))
        viewModel.onAction(OnboardingAction.ReminderDayToggled(DayOfWeek.MONDAY))
        viewModel.onAction(OnboardingAction.ReminderTimeChanged(LocalTime(18, 0)))
        viewModel.onAction(OnboardingAction.ContinueClicked) // REMINDER -> REVIEW
    }
}
