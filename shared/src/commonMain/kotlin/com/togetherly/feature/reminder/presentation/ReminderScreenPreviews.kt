package com.togetherly.feature.reminder.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.notification.NotificationPermissionState
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.feature.reminder.model.ReminderField
import com.togetherly.feature.reminder.model.ReminderUiState
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private val ENABLED_STATE = ReminderUiState(
    isLoading = false,
    enabled = true,
    days = persistentSetOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
    time = LocalTime(18, 0),
    permissionState = NotificationPermissionState.Granted,
)

@Composable
private fun ReminderPreview(state: ReminderUiState) {
    TogetherlyTheme {
        ReminderScreen(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun ReminderDisabledPreview() {
    ReminderPreview(ReminderUiState(isLoading = false, enabled = false, permissionState = NotificationPermissionState.NotDetermined))
}

@Preview
@Composable
private fun ReminderEnabledGrantedPreview() {
    ReminderPreview(ENABLED_STATE)
}

@Preview
@Composable
private fun ReminderLoadingPreview() {
    ReminderPreview(ReminderUiState(isLoading = true))
}

@Preview
@Composable
private fun ReminderPermissionDeniedPreview() {
    ReminderPreview(ENABLED_STATE.copy(permissionState = NotificationPermissionState.PermanentlyDenied))
}

@Preview
@Composable
private fun ReminderValidationErrorsPreview() {
    ReminderPreview(
        ENABLED_STATE.copy(
            days = persistentSetOf(),
            time = null,
            validationErrors = persistentMapOf(
                ReminderField.DAYS to UiText.Dynamic("Choose at least one day."),
                ReminderField.TIME to UiText.Dynamic("Choose a reminder time."),
            ),
        ),
    )
}

@Preview
@Composable
private fun ReminderUnsavedChangesPreview() {
    ReminderPreview(ENABLED_STATE.copy(hasUnsavedChanges = true, showDiscardDialog = true))
}

@Preview
@Composable
private fun ReminderSavingPreview() {
    ReminderPreview(ENABLED_STATE.copy(isSaving = true))
}

@Preview
@Composable
private fun ReminderErrorPreview() {
    ReminderPreview(ENABLED_STATE.copy(error = UiText.Dynamic("Something went wrong. Please try again.")))
}

@Preview(fontScale = 2f)
@Composable
private fun ReminderLargeTextPreview() {
    ReminderPreview(ENABLED_STATE)
}
