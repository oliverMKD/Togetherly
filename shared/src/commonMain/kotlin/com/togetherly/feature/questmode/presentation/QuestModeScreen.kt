package com.togetherly.feature.questmode.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.questmode.model.QuestModeContentUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import com.togetherly.feature.today.presentation.color
import com.togetherly.feature.today.mapper.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.questmode_close_content_description
import togetherly.shared.generated.resources.questmode_complete_action
import togetherly.shared.generated.resources.questmode_error_close_action
import togetherly.shared.generated.resources.questmode_hints_action
import togetherly.shared.generated.resources.questmode_hints_collapsed_state
import togetherly.shared.generated.resources.questmode_hints_expanded_state
import togetherly.shared.generated.resources.questmode_phone_down_action
import togetherly.shared.generated.resources.questmode_phone_down_supporting_timed
import togetherly.shared.generated.resources.questmode_phone_down_supporting_untimed
import togetherly.shared.generated.resources.questmode_safety_title
import togetherly.shared.generated.resources.questmode_timer_content_description_finished
import togetherly.shared.generated.resources.questmode_timer_content_description_running
import togetherly.shared.generated.resources.questmode_timer_finished

/**
 * Stateless: every value comes from [state], every intent goes out through [onAction] —
 * [QuestModeRoute] is the only place that talks to [QuestModeViewModel] directly. Full-screen —
 * rendered as a root-level destination (see [com.togetherly.navigation.destination.RootDestination.QuestMode]'s
 * own KDoc), so it never shows Main's bottom navigation.
 */
@Composable
internal fun QuestModeScreen(
    state: QuestModeUiState,
    onAction: (QuestModeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        QuestModeUiState.Loading -> QuestModeLoadingScreen(modifier)
        is QuestModeUiState.Error -> QuestModeErrorScreen(state, onAction, modifier)
        is QuestModeUiState.Content -> {
            if (state.phoneDown) {
                QuestModePhoneDownScreen(quest = state.quest, onAction = onAction, modifier = modifier)
            } else {
                QuestModeActiveScreen(state = state, onAction = onAction, modifier = modifier)
            }
            if (state.showExitConfirmation) {
                ExitConfirmationDialog(onAction = onAction)
            }
            if (state.showAbandonConfirmation) {
                AbandonConfirmationDialog(onAction = onAction)
            }
        }
    }
}

@Composable
private fun QuestModeLoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TogetherlyLoadingIndicator()
    }
}

@Composable
private fun QuestModeErrorScreen(
    state: QuestModeUiState.Error,
    onAction: (QuestModeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TogetherlyInlineError(
                    message = state.message.asString(),
                    onRetry = if (state.canRetry) {
                        { onAction(QuestModeAction.RetryClicked) }
                    } else {
                        null
                    },
                )
                if (state.canClose) {
                    TogetherlyTextButton(
                        label = stringResource(Res.string.questmode_error_close_action),
                        onClick = { onAction(QuestModeAction.CloseClicked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestModeActiveScreen(
    state: QuestModeUiState.Content,
    onAction: (QuestModeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("✕", style = MaterialTheme.togetherlyTypography.titleL) },
                        contentDescription = stringResource(Res.string.questmode_close_content_description),
                        onClick = { onAction(QuestModeAction.BackClicked) },
                    )
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m)) {
                if (state.quest.phoneDownSupported) {
                    PhoneDownEntry(timer = state.quest.timer, onClick = { onAction(QuestModeAction.PhoneDownClicked) })
                }
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.questmode_complete_action),
                    loading = state.isCompleting,
                    onClick = { onAction(QuestModeAction.CompleteClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            QuestModeHeader(quest = state.quest)

            QuestModeInstructionsList(quest = state.quest)

            if (state.quest.hints.isNotEmpty()) {
                QuestModeHints(hints = state.quest.hints, expanded = state.hintsExpanded, onToggle = { onAction(QuestModeAction.HintsToggled) })
            }

            if (state.quest.safetyNote != null) {
                QuestModeSafetyNote(safetyNote = state.quest.safetyNote)
            }

            if (state.error != null) {
                TogetherlyInlineError(message = state.error.asString())
            }
        }
    }
}

@Composable
private fun QuestModeHeader(quest: QuestModeContentUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
            Box(modifier = Modifier.size(MaterialTheme.togetherlySpacing.s).background(quest.category.color(), CircleShape))
            Text(
                text = quest.category.label().asString(),
                style = MaterialTheme.togetherlyTypography.labelM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
        Text(text = quest.title, style = MaterialTheme.togetherlyTypography.headlineM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
        QuestModeTimer(timer = quest.timer)
    }
}

@Composable
private fun QuestModeTimer(timer: QuestTimerUi, modifier: Modifier = Modifier) {
    when (timer) {
        QuestTimerUi.Hidden -> Unit
        is QuestTimerUi.Running -> {
            val description = stringResource(Res.string.questmode_timer_content_description_running, timer.displayTime)
            Column(
                modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
            ) {
                Text(
                    text = timer.displayTime,
                    style = MaterialTheme.togetherlyTypography.titleL,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
                LinearProgressIndicator(
                    progress = { timer.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.togetherlyColors.actionPrimary,
                    trackColor = MaterialTheme.togetherlyColors.borderSubtle,
                )
            }
        }
        QuestTimerUi.Finished -> {
            val description = stringResource(Res.string.questmode_timer_content_description_finished)
            Text(
                text = stringResource(Res.string.questmode_timer_finished),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
                // liveRegion only here, never on the Running branch above: this Text enters
                // composition exactly once, on the Running-to-Finished transition, so TalkBack
                // announces it exactly once — a per-second Running announcement is what
                // deliberately has no liveRegion at all (see this function's own KDoc precedent).
                modifier = modifier.semantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
    }
}

@Composable
private fun QuestModeInstructionsList(quest: QuestModeContentUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m)) {
        quest.instructions.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                Text(
                    text = "${step.number}.",
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
                Text(
                    text = step.text,
                    style = MaterialTheme.togetherlyTypography.bodyL,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
            }
        }
    }
}

@Composable
private fun QuestModeHints(hints: List<String>, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val expandedState = stringResource(Res.string.questmode_hints_expanded_state)
    val collapsedState = stringResource(Res.string.questmode_hints_collapsed_state)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        TogetherlyTextButton(
            label = stringResource(Res.string.questmode_hints_action),
            onClick = onToggle,
            modifier = Modifier.semantics { stateDescription = if (expanded) expandedState else collapsedState },
        )
        if (expanded) {
            hints.forEach { hint ->
                Text(text = "•  $hint", style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
            }
        }
    }
}

@Composable
private fun QuestModeSafetyNote(safetyNote: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
        Text(
            text = stringResource(Res.string.questmode_safety_title),
            style = MaterialTheme.togetherlyTypography.labelL,
            color = MaterialTheme.togetherlyColors.warning,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = safetyNote, style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
    }
}

@Composable
private fun PhoneDownEntry(timer: QuestTimerUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val supportingText = if (timer == QuestTimerUi.Hidden) {
        stringResource(Res.string.questmode_phone_down_supporting_untimed)
    } else {
        stringResource(Res.string.questmode_phone_down_supporting_timed)
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TogetherlyTextButton(label = stringResource(Res.string.questmode_phone_down_action), onClick = onClick)
        Text(text = supportingText, style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.foregroundSecondary)
    }
}
