package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import com.togetherly.feature.onboarding.presentation.OnboardingFieldError
import com.togetherly.feature.onboarding.presentation.label
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_continue
import togetherly.shared.generated.resources.onboarding_reminder_choose
import togetherly.shared.generated.resources.onboarding_reminder_choose_time
import togetherly.shared.generated.resources.onboarding_reminder_days_label
import togetherly.shared.generated.resources.onboarding_reminder_not_now
import togetherly.shared.generated.resources.onboarding_reminder_time_label
import togetherly.shared.generated.resources.onboarding_reminder_time_selected
import togetherly.shared.generated.resources.onboarding_reminder_title

/**
 * Preference only — no OS notification-permission request happens here (or anywhere in
 * onboarding). That request belongs after the first completed quest, once Togetherly has actually
 * demonstrated value; asking here would be asking before showing why it matters.
 */
@Composable
internal fun ReminderStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_reminder_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
            TogetherlyChoiceChip(
                label = stringResource(Res.string.onboarding_reminder_not_now),
                selected = !state.reminderEnabled,
                onClick = { onAction(OnboardingAction.ReminderEnabledChanged(false)) },
            )
            TogetherlyChoiceChip(
                label = stringResource(Res.string.onboarding_reminder_choose),
                selected = state.reminderEnabled,
                onClick = { onAction(OnboardingAction.ReminderEnabledChanged(true)) },
            )
        }

        if (state.reminderEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                Text(
                    text = stringResource(Res.string.onboarding_reminder_days_label),
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
                TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        TogetherlyChoiceChip(
                            label = day.label(),
                            selected = day in state.reminderDays,
                            onClick = { onAction(OnboardingAction.ReminderDayToggled(day)) },
                        )
                    }
                }
                OnboardingFieldError(state, OnboardingField.REMINDER_DAYS)
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                Text(
                    text = stringResource(Res.string.onboarding_reminder_time_label),
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
                ReminderTimePicker(
                    time = state.reminderTime,
                    onTimeChanged = { onAction(OnboardingAction.ReminderTimeChanged(it)) },
                )
                OnboardingFieldError(state, OnboardingField.REMINDER_TIME)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(time: LocalTime?, onTimeChanged: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    TogetherlySecondaryButton(
        label = time?.let { stringResource(Res.string.onboarding_reminder_time_selected, it.formatted()) }
            ?: stringResource(Res.string.onboarding_reminder_choose_time),
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val state = rememberTimePickerState(
            initialHour = time?.hour ?: 18,
            initialMinute = time?.minute ?: 0,
        )
        TimePickerDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.onboarding_reminder_time_label)) },
            confirmButton = {
                TogetherlyTextButton(
                    label = stringResource(Res.string.onboarding_continue),
                    onClick = {
                        onTimeChanged(LocalTime(state.hour, state.minute))
                        showDialog = false
                    },
                )
            },
        ) {
            TimePicker(state = state)
        }
    }
}

private fun LocalTime.formatted(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
