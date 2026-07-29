package com.togetherly.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.OnboardingCompleted
import com.togetherly.core.telemetry.OnboardingStepViewed
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.family.usecase.CreateFamilyProfileCommand
import com.togetherly.domain.purchase.repository.CustomerAttributesRepository
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.navigation.next
import com.togetherly.feature.onboarding.navigation.previous
import com.togetherly.feature.onboarding.validation.OnboardingValidator
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Onboarding's single state machine — [uiState] is the whole draft, [onAction] is the only way in,
 * [events] is strictly for one-off effects ([OnboardingEvent.NavigateBack]/[OnboardingEvent.FamilyCreated]).
 * No Composable API, `NavController`, or Koin lookup appears here — see this feature's own package
 * KDocs for the boundary this maintains.
 */
class OnboardingViewModel(
    private val createFamilyProfile: CreateFamilyProfile,
    private val analytics: ProductAnalytics,
    private val customerAttributesRepository: CustomerAttributesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    /** Guards against a re-triggered [LaunchedEffect]/recomposition re-entering the same navigation visit — the screen view and its first step-viewed event must fire exactly once. */
    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.ONBOARDING)
        analytics.capture(OnboardingStepViewed(_uiState.value.step))
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.ContinueClicked -> onContinue()
            OnboardingAction.BackClicked -> onBack()
            OnboardingAction.SkipNameClicked -> onSkipName()
            is OnboardingAction.FamilyNameChanged -> updateDraft(OnboardingField.FAMILY_NAME) {
                it.copy(familyName = action.value)
            }
            is OnboardingAction.AgeBandToggled -> updateDraft(OnboardingField.AGE_BANDS) {
                val bands = it.selectedAgeBands
                it.copy(selectedAgeBands = if (action.value in bands) bands.removing(action.value) else bands.adding(action.value))
            }
            is OnboardingAction.InterestToggled -> updateDraft(OnboardingField.INTERESTS) {
                val interests = it.selectedInterests
                it.copy(selectedInterests = if (action.value in interests) interests.removing(action.value) else interests.adding(action.value))
            }
            is OnboardingAction.DurationToggled -> updateDraft(OnboardingField.DURATIONS) {
                val durations = it.selectedDurations
                it.copy(selectedDurations = if (action.value in durations) durations.removing(action.value) else durations.adding(action.value))
            }
            is OnboardingAction.LocationSelected -> updateDraft { it.copy(locationPreference = action.value) }
            is OnboardingAction.PreparationSelected -> updateDraft { it.copy(preparationPreference = action.value) }
            is OnboardingAction.ReminderEnabledChanged -> updateDraft(
                OnboardingField.REMINDER_DAYS,
                OnboardingField.REMINDER_TIME,
            ) { it.copy(reminderEnabled = action.enabled) }
            is OnboardingAction.ReminderDayToggled -> updateDraft(OnboardingField.REMINDER_DAYS) {
                val days = it.reminderDays
                it.copy(reminderDays = if (action.day in days) days.removing(action.day) else days.adding(action.day))
            }
            is OnboardingAction.ReminderTimeChanged -> updateDraft(OnboardingField.REMINDER_TIME) {
                it.copy(reminderTime = action.time)
            }
            OnboardingAction.CreateFamilyClicked -> onCreateFamily()
            OnboardingAction.RetrySaveClicked -> onCreateFamily()
        }
    }

    /**
     * Applies [transform], then clears [fields] from [OnboardingUiState.validationErrors] and any
     * stale [OnboardingUiState.saveError] — every ordinary input change invalidates a previous save
     * attempt's error, and clears the specific field(s) it just corrected rather than the whole map
     * (an unrelated field's still-outstanding error must survive).
     */
    private fun updateDraft(vararg fields: OnboardingField, transform: (OnboardingUiState) -> OnboardingUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val remainingErrors = fields.fold(next.validationErrors) { errors, field -> errors.removing(field) }
            next.copy(validationErrors = remainingErrors, saveError = null)
        }
    }

    private fun onContinue() {
        val current = _uiState.value
        val errors = OnboardingValidator.validateStep(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }
        val next = current.step.next() ?: return
        // A freshly-arrived-at step never shows errors before the user has had a chance to
        // interact with it — errors reappear only on the next failed Continue/Create attempt.
        _uiState.update { it.copy(step = next, validationErrors = persistentMapOf()) }
        analytics.capture(OnboardingStepViewed(next))
    }

    private fun onBack() {
        val previous = _uiState.value.step.previous()
        if (previous != null) {
            _uiState.update { it.copy(step = previous, validationErrors = persistentMapOf()) }
            analytics.capture(OnboardingStepViewed(previous))
        } else {
            viewModelScope.launch { _events.send(OnboardingEvent.NavigateBack) }
        }
    }

    private fun onSkipName() {
        var advanced = false
        _uiState.update { current ->
            val next = current.step.next() ?: current.step
            advanced = next != current.step
            current.copy(step = next, familyName = "", validationErrors = current.validationErrors.removing(OnboardingField.FAMILY_NAME))
        }
        if (advanced) analytics.capture(OnboardingStepViewed(_uiState.value.step))
    }

    private fun onCreateFamily() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = OnboardingValidator.validateAll(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            when (val result = createFamilyProfile(current.toCreateFamilyProfileCommand())) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    analytics.capture(OnboardingCompleted)
                    customerAttributesRepository.markOnboardingCompleted()
                    current.selectedDurations.minOrNull()?.let { customerAttributesRepository.setPreferredDurationBucket(it) }
                    _events.send(OnboardingEvent.FamilyCreated)
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, saveError = result.error.toUiText()) }
                }
            }
        }
    }
}

/**
 * Deliberately not a public function of [OnboardingUiState] itself — building the persistence
 * command is this ViewModel's responsibility alone, never something a screen or another feature
 * reaches for. Blank family name → `null`; non-blank → a validated [FamilyDisplayName], matching
 * [OnboardingValidator]'s own trim-before-validate rule so a save can never fail on a family name
 * [OnboardingValidator] had already accepted.
 */
private fun OnboardingUiState.toCreateFamilyProfileCommand(): CreateFamilyProfileCommand {
    val trimmedName = familyName.trim()
    return CreateFamilyProfileCommand(
        displayName = trimmedName.takeIf { it.isNotEmpty() }?.let { FamilyDisplayName(it) },
        childAgeBands = selectedAgeBands,
        interests = selectedInterests,
        preferredDurations = selectedDurations,
        locationPreference = locationPreference,
        preparationPreference = preparationPreference,
        // Onboarding doesn't collect an energy preference yet — Step 13.3's own quest preferences
        // screen is where a family first sets it.
        preferredEnergyLevels = emptySet(),
        reminderPreference = reminderTime?.takeIf { reminderEnabled }?.let { time ->
            ReminderPreference(enabledDays = reminderDays, localTime = time)
        },
    )
}
