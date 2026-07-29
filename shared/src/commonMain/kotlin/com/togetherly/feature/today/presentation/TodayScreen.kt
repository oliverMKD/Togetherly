package com.togetherly.feature.today.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.motion.resolveMotionDuration
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyMotion
import com.togetherly.designsystem.theme.togetherlyReduceMotion
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.today.model.QuestCardUi
import com.togetherly.feature.today.model.TodayContentState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_filters_action
import togetherly.shared.generated.resources.today_reroll_action
import togetherly.shared.generated.resources.today_reroll_used

/**
 * Stateless: every value it renders comes from [state], every intent goes out through [onAction] —
 * [TodayRoute] is the only place that talks to [TodayViewModel] directly. Today's own scroll
 * container (not the shell's — [com.togetherly.navigation.shell.MainShell] renders each tab with
 * `scrollable = false`) so a small screen with a fully revealed, materials-heavy card never clips.
 */
@Composable
internal fun TodayScreen(
    state: TodayUiState,
    onAction: (TodayAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = MaterialTheme.togetherlySpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        TodayHeader(greeting = state.greeting.asString(), familyName = state.familyName)

        FilterEntryAction(activeCount = state.filters.activeCount, onClick = { onAction(TodayAction.FiltersClicked) })

        when (val content = state.content) {
            TodayContentState.Loading -> TogetherlyLoadingIndicator()
            is TodayContentState.Error -> TogetherlyInlineError(
                message = content.message.asString(),
                onRetry = if (content.retryAvailable) {
                    { onAction(TodayAction.RetryClicked) }
                } else {
                    null
                },
            )
            is TodayContentState.Mystery, is TodayContentState.Revealed, is TodayContentState.Completed ->
                TodayQuestReveal(content = content, onAction = onAction)
        }

        if (state.content is TodayContentState.Revealed) {
            RerollEntryAction(
                rerollsRemaining = state.rerollsRemaining,
                isRerolling = state.isRerolling,
                onClick = { onAction(TodayAction.RerollClicked) },
            )
        }

        if (state.transientError != null) {
            TogetherlyInlineError(
                message = state.transientError.asString(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.togetherlySpacing.m),
            )
        }
    }

    if (state.filtersVisible) {
        TodayFilterSheet(
            filters = state.filters,
            isApplying = state.isApplyingFilters,
            onAction = onAction,
            onDismiss = { onAction(TodayAction.FiltersDismissed) },
        )
    }

    if (state.rerollConfirmationVisible) {
        RerollConfirmationDialog(
            onConfirm = { onAction(TodayAction.RerollConfirmed) },
            onDismiss = { onAction(TodayAction.RerollCancelled) },
        )
    }
}

@Composable
private fun TodayHeader(greeting: String, familyName: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        if (familyName != null) {
            Text(
                text = familyName,
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
    }
}

@Composable
private fun FilterEntryAction(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val baseLabel = stringResource(Res.string.today_filters_action)
    val label = if (activeCount > 0) "$baseLabel ($activeCount)" else baseLabel
    TogetherlySecondaryButton(label = label, onClick = onClick, modifier = modifier)
}

@Composable
private fun RerollEntryAction(
    rerollsRemaining: Int?,
    isRerolling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rerollsRemaining == 0) {
        Text(
            text = stringResource(Res.string.today_reroll_used),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = modifier,
        )
    } else {
        TogetherlyTextButton(
            label = stringResource(Res.string.today_reroll_action),
            onClick = onClick,
            loading = isRerolling,
            modifier = modifier,
        )
    }
}

/**
 * [content]'s underlying [QuestCardUi] is extracted once, outside the [Crossfade], so the quest
 * data driving the *outgoing* branch stays available for the whole transition — [Crossfade] keeps
 * rendering both branches' composables while animating, and deriving the quest fresh from
 * [content] inside each branch would go `null` mid-fade the instant [content] itself flips to
 * [TodayContentState.Revealed].
 */
@Composable
private fun TodayQuestReveal(
    content: TodayContentState,
    onAction: (TodayAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quest = when (content) {
        is TodayContentState.Mystery -> content.quest
        is TodayContentState.Revealed -> content.quest
        is TodayContentState.Completed -> content.quest
        else -> return
    }
    val revealed = content is TodayContentState.Revealed || content is TodayContentState.Completed
    val completed = content is TodayContentState.Completed
    val duration = resolveMotionDuration(MaterialTheme.togetherlyMotion.reveal, MaterialTheme.togetherlyReduceMotion)

    Crossfade(
        targetState = revealed,
        modifier = modifier,
        animationSpec = tween(durationMillis = duration),
        label = "today-quest-reveal",
    ) { isRevealed ->
        if (isRevealed) {
            RevealedQuestCard(quest = quest, onAction = onAction, isCompleted = completed)
        } else {
            MysteryQuestCard(onReveal = { onAction(TodayAction.RevealClicked) })
        }
    }
}
