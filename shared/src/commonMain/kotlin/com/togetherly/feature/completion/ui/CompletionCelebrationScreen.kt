package com.togetherly.feature.completion.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.motion.resolveMotionDuration
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyMotion
import com.togetherly.designsystem.theme.togetherlyReduceMotion
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.completion.model.CompletionCelebrationUiState
import com.togetherly.feature.completion.presentation.CompletionCelebrationAction
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.presentation.color
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.completion_body
import togetherly.shared.generated.resources.completion_close_action
import togetherly.shared.generated.resources.completion_continue_action
import togetherly.shared.generated.resources.completion_add_memory_action
import togetherly.shared.generated.resources.completion_neutral_quest_title
import togetherly.shared.generated.resources.completion_title

/**
 * Stateless: every value comes from [state], every intent goes out through [onAction]. The star
 * animation (see [CelebrationStar]) is a one-shot appear, never a loop — "does not repeat
 * endlessly" is satisfied simply by never restarting it after its single `LaunchedEffect` run.
 */
@Composable
internal fun CompletionCelebrationScreen(
    state: CompletionCelebrationUiState,
    onAction: (CompletionCelebrationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                CompletionCelebrationUiState.Loading -> TogetherlyLoadingIndicator()
                is CompletionCelebrationUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TogetherlyInlineError(message = state.message.asString())
                    TogetherlyTextButton(
                        label = stringResource(Res.string.completion_close_action),
                        onClick = { onAction(CompletionCelebrationAction.ContinueClicked) },
                    )
                }
                is CompletionCelebrationUiState.Content -> CompletionCelebrationContent(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun CompletionCelebrationContent(
    state: CompletionCelebrationUiState.Content,
    onAction: (CompletionCelebrationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.togetherlySpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        CelebrationStar(color = state.quest?.category?.color() ?: MaterialTheme.togetherlyColors.actionPrimary)

        Text(
            text = stringResource(Res.string.completion_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.completion_body),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = state.quest?.title ?: stringResource(Res.string.completion_neutral_quest_title),
            style = MaterialTheme.togetherlyTypography.titleL,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            textAlign = TextAlign.Center,
        )
        if (state.quest != null) {
            Text(
                text = state.quest.category.label().asString(),
                style = MaterialTheme.togetherlyTypography.labelM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
        Text(
            text = state.completedAtDisplay,
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )

        if (state.showAddMemoryPrompt) {
            TogetherlyPrimaryButton(
                label = stringResource(Res.string.completion_add_memory_action),
                onClick = { onAction(CompletionCelebrationAction.AddMemoryClicked) },
            )
        } else {
            TogetherlyPrimaryButton(
                label = stringResource(Res.string.completion_continue_action),
                onClick = { onAction(CompletionCelebrationAction.ContinueClicked) },
            )
        }
    }
}

/**
 * A one-shot fade/scale-in, budgeted at [com.togetherly.designsystem.motion.TogetherlyMotion.celebration]
 * via [resolveMotionDuration] (never the raw token — reduced motion collapses this to an instant
 * appearance, never a suppressed error or a missing visual). Purely decorative:
 * [clearAndSetSemantics] with no description removes it from the accessibility tree entirely — the
 * actual meaningful content ("You made a memory", the quest title, the date) are separate, real
 * text nodes right next to it, never merged with this shape.
 */
@Composable
private fun CelebrationStar(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val duration = resolveMotionDuration(MaterialTheme.togetherlyMotion.celebration, MaterialTheme.togetherlyReduceMotion)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = duration))
    }

    Box(
        modifier = modifier
            .padding(MaterialTheme.togetherlySpacing.m)
            .alpha(progress.value)
            .scale(0.6f + 0.4f * progress.value)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "★", style = MaterialTheme.togetherlyTypography.displayL, color = color)
    }
}
