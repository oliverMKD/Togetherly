package com.togetherly.feature.reminder.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.notification.NotificationPermissionState
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.onboarding.presentation.label
import com.togetherly.feature.reminder.model.ReminderAction
import com.togetherly.feature.reminder.model.ReminderField
import com.togetherly.feature.reminder.model.ReminderUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.family_discard_dialog_body
import togetherly.shared.generated.resources.family_discard_dialog_cancel_action
import togetherly.shared.generated.resources.family_discard_dialog_confirm_action
import togetherly.shared.generated.resources.family_discard_dialog_title
import togetherly.shared.generated.resources.reminder_choose_time
import togetherly.shared.generated.resources.reminder_days_label
import togetherly.shared.generated.resources.reminder_enable_toggle
import togetherly.shared.generated.resources.reminder_explanation
import togetherly.shared.generated.resources.reminder_open_settings_action
import togetherly.shared.generated.resources.reminder_permission_denied
import togetherly.shared.generated.resources.reminder_permission_denied_explanation
import togetherly.shared.generated.resources.reminder_permission_granted
import togetherly.shared.generated.resources.reminder_permission_not_determined
import togetherly.shared.generated.resources.reminder_permission_not_required
import togetherly.shared.generated.resources.reminder_permission_status_label
import togetherly.shared.generated.resources.reminder_save_action
import togetherly.shared.generated.resources.reminder_time_label
import togetherly.shared.generated.resources.reminder_time_selected
import togetherly.shared.generated.resources.reminder_title

@Composable
internal fun ReminderScreen(
    state: ReminderUiState,
    onAction: (ReminderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.reminder_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(ReminderAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            else -> ReminderContent(state = state, onAction = onAction)
        }
    }

    if (state.showDiscardDialog) {
        ReminderDiscardDialog(onAction = onAction)
    }
}

@Composable
private fun ReminderContent(
    state: ReminderUiState,
    onAction: (ReminderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Text(
            text = stringResource(Res.string.reminder_explanation),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )

        if (state.error != null) {
            TogetherlyInlineError(message = state.error.asString())
        }

        TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
            TogetherlyChoiceChip(
                label = stringResource(Res.string.reminder_enable_toggle),
                selected = state.enabled,
                onClick = { onAction(ReminderAction.EnabledChanged(!state.enabled)) },
            )
        }

        if (state.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                Text(
                    text = stringResource(Res.string.reminder_days_label),
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
                TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        TogetherlyChoiceChip(
                            label = day.label(),
                            selected = day in state.days,
                            onClick = { onAction(ReminderAction.DayToggled(day)) },
                        )
                    }
                }
                state.validationErrors[ReminderField.DAYS]?.let { error ->
                    Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                Text(
                    text = stringResource(Res.string.reminder_time_label),
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
                ReminderTimePicker(time = state.time, onTimeChanged = { onAction(ReminderAction.TimeChanged(it)) })
                state.validationErrors[ReminderField.TIME]?.let { error ->
                    Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
                }
            }

            PermissionStatusSection(state = state, onAction = onAction)
        }

        TogetherlyPrimaryButton(
            label = stringResource(Res.string.reminder_save_action),
            onClick = { onAction(ReminderAction.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isSaving,
        )
    }
}

@Composable
private fun PermissionStatusSection(
    state: ReminderUiState,
    onAction: (ReminderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.reminder_permission_status_label) + ": " + state.permissionState.label(),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        val blocked = state.permissionState == NotificationPermissionState.Denied ||
            state.permissionState == NotificationPermissionState.PermanentlyDenied
        if (blocked) {
            Text(
                text = stringResource(Res.string.reminder_permission_denied_explanation),
                style = MaterialTheme.togetherlyTypography.bodyS,
                color = MaterialTheme.togetherlyColors.error,
            )
            TogetherlySecondaryButton(
                label = stringResource(Res.string.reminder_open_settings_action),
                onClick = { onAction(ReminderAction.OpenSettingsClicked) },
            )
        }
    }
}

@Composable
private fun NotificationPermissionState.label(): String = stringResource(
    when (this) {
        NotificationPermissionState.NotDetermined -> Res.string.reminder_permission_not_determined
        NotificationPermissionState.Granted -> Res.string.reminder_permission_granted
        NotificationPermissionState.Denied, NotificationPermissionState.PermanentlyDenied -> Res.string.reminder_permission_denied
        NotificationPermissionState.NotRequired -> Res.string.reminder_permission_not_required
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(time: LocalTime?, onTimeChanged: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    TogetherlySecondaryButton(
        label = time?.let { stringResource(Res.string.reminder_time_selected, it.formatted()) }
            ?: stringResource(Res.string.reminder_choose_time),
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val pickerState = rememberTimePickerState(initialHour = time?.hour ?: 18, initialMinute = time?.minute ?: 0)
        TimePickerDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.reminder_time_label)) },
            confirmButton = {
                TogetherlyTextButton(
                    label = stringResource(Res.string.reminder_save_action),
                    onClick = {
                        onTimeChanged(LocalTime(pickerState.hour, pickerState.minute))
                        showDialog = false
                    },
                )
            },
        ) {
            TimePicker(state = pickerState)
        }
    }
}

private fun LocalTime.formatted(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
private fun ReminderDiscardDialog(onAction: (ReminderAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ReminderAction.DismissDiscardDialog) },
        title = { Text(stringResource(Res.string.family_discard_dialog_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.family_discard_dialog_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(ReminderAction.DiscardConfirmed) }) {
                Text(stringResource(Res.string.family_discard_dialog_confirm_action), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ReminderAction.DismissDiscardDialog) }) {
                Text(stringResource(Res.string.family_discard_dialog_cancel_action))
            }
        },
    )
}
