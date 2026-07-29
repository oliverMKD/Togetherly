package com.togetherly.feature.reminder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.notification.NotificationPermissionStatusProvider
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.ReminderPreferenceChanged
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateReminderPreference
import com.togetherly.feature.family.presentation.FamilySaveMessageStore
import com.togetherly.feature.reminder.model.ReminderAction
import com.togetherly.feature.reminder.model.ReminderEvent
import com.togetherly.feature.reminder.model.ReminderField
import com.togetherly.feature.reminder.model.ReminderUiState
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.reminder_missing_profile_error
import togetherly.shared.generated.resources.reminder_updated_message

/**
 * Loads the current [ReminderPreference] once ([onScreenStarted], same "load a draft once, edit
 * locally" shape as [com.togetherly.feature.family.presentation.FamilyProfileEditorViewModel]) and
 * the current [com.togetherly.core.notification.NotificationPermissionState] alongside it — the
 * latter is only ever *read* here via [NotificationPermissionStatusProvider], never requested;
 * requesting only ever happens in response to [onAction] turning reminders on (see [onEnabledChanged]),
 * matching this feature's own requirement to never prompt for permission except after an explicit
 * enable.
 *
 * Saving never touches [com.togetherly.domain.daily.DailyQuest] or reroll allowance — this
 * ViewModel only writes the reminder preference and (best-effort) asks [ReminderScheduler] to
 * apply it. A scheduling failure never blocks a successful preference save: the preference itself
 * is the source of truth, and [com.togetherly.navigation.state.BootstrapViewModel]'s own
 * [ReminderScheduler.refresh] call on every app start is the self-healing path for a schedule that
 * silently failed or was cleared by the OS (e.g. an Android reboot).
 */
class ReminderViewModel(
    private val observeFamilySettings: ObserveFamilySettings,
    private val updateReminderPreference: UpdateReminderPreference,
    private val notificationPermissionStatusProvider: NotificationPermissionStatusProvider,
    private val reminderScheduler: ReminderScheduler,
    private val saveMessageStore: FamilySaveMessageStore,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReminderEvent>(Channel.BUFFERED)
    val events: Flow<ReminderEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var original: ReminderPreference? = null

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.REMINDER)

        viewModelScope.launch {
            when (val result = observeFamilySettings().first()) {
                is DataResult.Success -> {
                    val settings = result.value
                    if (settings != null) {
                        val preference = settings.reminderPreference
                        original = preference
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                enabled = preference != null,
                                days = preference?.enabledDays.orEmpty().toPersistentSet(),
                                time = preference?.localTime,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = UiText.Resource(Res.string.reminder_missing_profile_error)) }
                    }
                }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }

        viewModelScope.launch {
            val status = notificationPermissionStatusProvider.currentStatus()
            _uiState.update { it.copy(permissionState = status) }
        }
    }

    fun onAction(action: ReminderAction) {
        when (action) {
            is ReminderAction.EnabledChanged -> onEnabledChanged(action.enabled)
            is ReminderAction.DayToggled -> updateDraft(ReminderField.DAYS) {
                val days = it.days
                it.copy(days = if (action.day in days) days.removing(action.day) else days.adding(action.day))
            }
            is ReminderAction.TimeChanged -> updateDraft(ReminderField.TIME) { it.copy(time = action.time) }
            is ReminderAction.PermissionResultReceived -> _uiState.update { it.copy(permissionState = action.state) }
            ReminderAction.OpenSettingsClicked -> viewModelScope.launch { _events.send(ReminderEvent.OpenSystemSettings) }
            ReminderAction.SaveClicked -> onSave()
            ReminderAction.BackClicked -> onBack()
            ReminderAction.DiscardConfirmed -> onDiscardConfirmed()
            ReminderAction.DismissDiscardDialog -> _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun onEnabledChanged(enabled: Boolean) {
        updateDraft(ReminderField.DAYS, ReminderField.TIME) { it.copy(enabled = enabled) }
        if (enabled) {
            viewModelScope.launch { _events.send(ReminderEvent.RequestNotificationPermission) }
        }
    }

    private fun updateDraft(vararg fields: ReminderField, transform: (ReminderUiState) -> ReminderUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val remainingErrors = fields.fold(next.validationErrors) { errors, field -> errors.removing(field) }
            next.copy(
                validationErrors = remainingErrors,
                error = null,
                hasUnsavedChanges = next.differsFromOriginal(original),
            )
        }
    }

    private fun onBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _events.send(ReminderEvent.NavigatedBackWithoutSaving) }
        }
    }

    private fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { _events.send(ReminderEvent.NavigatedBackWithoutSaving) }
    }

    private fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = ReminderValidator.validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        val preference = if (current.enabled) ReminderPreference(current.days, requireNotNull(current.time)) else null
        val changed = current.hasUnsavedChanges
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            when (val result = updateReminderPreference(preference)) {
                is DataResult.Success -> {
                    if (preference != null) reminderScheduler.schedule(preference) else reminderScheduler.cancel()
                    original = preference
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    if (changed) {
                        analytics.capture(ReminderPreferenceChanged(enabled = current.enabled, selectedDayCount = current.days.size))
                    }
                    saveMessageStore.publish(UiText.Resource(Res.string.reminder_updated_message))
                    _events.send(ReminderEvent.SaveCompleted)
                }
                is DataResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}

private fun ReminderUiState.differsFromOriginal(original: ReminderPreference?): Boolean {
    val currentlyEnabled = enabled
    val originallyEnabled = original != null
    if (currentlyEnabled != originallyEnabled) return true
    if (!currentlyEnabled) return false
    return days.toSet() != original?.enabledDays || time != original.localTime
}
