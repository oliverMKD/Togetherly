package com.togetherly.feature.questdetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.questdetail.model.QuestDetailUi
import com.togetherly.feature.questdetail.model.QuestDetailUiState
import com.togetherly.feature.today.presentation.color
import com.togetherly.feature.today.mapper.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.questdetail_back_content_description
import togetherly.shared.generated.resources.questdetail_conflict_body
import togetherly.shared.generated.resources.questdetail_conflict_cancel_action
import togetherly.shared.generated.resources.questdetail_conflict_replace_action
import togetherly.shared.generated.resources.questdetail_conflict_title
import togetherly.shared.generated.resources.questdetail_age_suitability_title
import togetherly.shared.generated.resources.questdetail_hints_title
import togetherly.shared.generated.resources.questdetail_instructions_title
import togetherly.shared.generated.resources.questdetail_locked_body
import togetherly.shared.generated.resources.questdetail_locked_title
import togetherly.shared.generated.resources.questdetail_materials_title
import togetherly.shared.generated.resources.questdetail_pack_label
import togetherly.shared.generated.resources.questdetail_premium_badge
import togetherly.shared.generated.resources.questdetail_safety_title
import togetherly.shared.generated.resources.questdetail_saved_content_description
import togetherly.shared.generated.resources.questdetail_start_action
import togetherly.shared.generated.resources.questdetail_unlock_action
import togetherly.shared.generated.resources.questdetail_unsaved_content_description
import togetherly.shared.generated.resources.ds_component_back_content_description

/**
 * Stateless: every value comes from [state], every intent goes out through [onAction] —
 * [QuestDetailRoute] is the only place that talks to [QuestDetailViewModel] directly. Shows summary
 * metadata plus full ordered instructions/materials/hints/safety note — everything Today's own
 * card deliberately leaves out (see [com.togetherly.feature.today.presentation.RevealedQuestCard]'s
 * own KDoc).
 */
@Composable
internal fun QuestDetailScreen(
    state: QuestDetailUiState,
    onAction: (QuestDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(QuestDetailAction.BackClicked) },
                    )
                },
                actions = {
                    if (state is QuestDetailUiState.Content) {
                        SaveToggle(isSaved = state.isSaved, onToggle = { onAction(QuestDetailAction.SaveClicked) })
                    }
                },
            )
        },
        bottomBar = {
            if (state is QuestDetailUiState.Content) {
                if (state.locked) {
                    TogetherlyPrimaryButton(
                        label = stringResource(Res.string.questdetail_unlock_action),
                        onClick = { onAction(QuestDetailAction.UnlockClicked) },
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
                    )
                } else {
                    TogetherlyPrimaryButton(
                        label = stringResource(Res.string.questdetail_start_action),
                        loading = state.isStarting,
                        onClick = { onAction(QuestDetailAction.StartClicked) },
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
                    )
                }
            }
        },
    ) {
        when (state) {
            QuestDetailUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            is QuestDetailUiState.Error -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TogetherlyInlineError(
                    message = state.message.asString(),
                    onRetry = { onAction(QuestDetailAction.RetryClicked) },
                )
            }
            is QuestDetailUiState.Content -> {
                if (state.locked) {
                    LockedContentExplanation(modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.togetherlySpacing.m))
                }
                QuestDetailContent(quest = state.quest)
                if (state.startError != null) {
                    TogetherlyInlineError(
                        message = state.startError.asString(),
                        modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.togetherlySpacing.s),
                    )
                }
                if (state.activeSessionConflict) {
                    ActiveSessionConflictDialog(
                        onReplace = { onAction(QuestDetailAction.ReplaceActiveSessionConfirmed) },
                        onCancel = { onAction(QuestDetailAction.KeepActiveSessionClicked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveToggle(isSaved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (isSaved) Res.string.questdetail_saved_content_description else Res.string.questdetail_unsaved_content_description,
    )
    TogetherlyIconButton(
        icon = { Text(text = if (isSaved) "★" else "☆", style = MaterialTheme.togetherlyTypography.titleL) },
        contentDescription = description,
        onClick = onToggle,
        modifier = modifier,
    )
}

@Composable
private fun QuestDetailContent(quest: QuestDetailUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
                Box(
                    modifier = Modifier.size(MaterialTheme.togetherlySpacing.s).background(quest.category.color(), CircleShape),
                )
                Text(
                    text = quest.category.label().asString(),
                    style = MaterialTheme.togetherlyTypography.labelM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
            if (quest.isPremium) {
                PremiumBadge()
            }
        }

        Text(text = quest.title, style = MaterialTheme.togetherlyTypography.headlineM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
        Text(text = quest.summary, style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)

        if (quest.packTitle != null) {
            Text(
                text = stringResource(Res.string.questdetail_pack_label, quest.packTitle),
                style = MaterialTheme.togetherlyTypography.labelM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }

        val metadataLabels = listOf(quest.durationLabel.asString(), quest.locationLabel.asString(), quest.preparationLabel.asString(), quest.energyLabel.asString())
        Text(
            text = metadataLabels.joinToString(separator = "  •  "),
            modifier = Modifier.semantics { contentDescription = metadataLabels.joinToString(separator = ". ") },
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )

        if (quest.ageBandLabels.isNotEmpty()) {
            val ageBandText = quest.ageBandLabels.map { it.asString() }.joinToString(separator = ", ")
            QuestDetailSection(title = stringResource(Res.string.questdetail_age_suitability_title)) {
                Text(
                    text = ageBandText,
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
            }
        }

        if (quest.materials.isNotEmpty()) {
            QuestDetailSection(title = stringResource(Res.string.questdetail_materials_title)) {
                quest.materials.forEach { material ->
                    Text(text = "•  $material", style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
                }
            }
        }

        if (quest.instructions.isNotEmpty()) {
            QuestDetailSection(title = stringResource(Res.string.questdetail_instructions_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                    quest.instructions.forEachIndexed { index, instruction ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.togetherlyColors.backgroundSurface, shape = MaterialTheme.togetherlyShapes.medium)
                                .padding(MaterialTheme.togetherlySpacing.s),
                        ) {
                            Text(
                                text = "${index + 1}.  $instruction",
                                style = MaterialTheme.togetherlyTypography.bodyM,
                                color = MaterialTheme.togetherlyColors.foregroundPrimary,
                            )
                        }
                    }
                }
            }
        }

        if (quest.hints.isNotEmpty()) {
            QuestDetailSection(title = stringResource(Res.string.questdetail_hints_title)) {
                quest.hints.forEach { hint ->
                    Text(text = "•  $hint", style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
                }
            }
        }

        if (quest.safetyNote != null) {
            QuestDetailSafetyNote(safetyNote = quest.safetyNote)
        }
    }
}

/**
 * Its own composable, not another [QuestDetailSection] call, so its title can carry the same
 * `warning`-colored treatment [com.togetherly.feature.questmode.presentation.QuestModeScreen]'s
 * own safety note already uses — previously this screen showed the identical safety copy in the
 * same neutral style as Materials/Hints, which meant the one genuinely safety-critical section
 * looked no more important than a shopping list.
 */
@Composable
private fun QuestDetailSafetyNote(safetyNote: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.questdetail_safety_title),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.warning,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = safetyNote,
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.togetherlyColors
    Text(
        text = stringResource(Res.string.questdetail_premium_badge),
        style = MaterialTheme.togetherlyTypography.labelM,
        color = colors.actionPrimary,
        modifier = modifier,
    )
}

/**
 * Real content, not a fake blur — this feature's own task spec: "Do not obscure the screen with a
 * fake blur if that harms accessibility. Prefer an intentional locked-content section."
 */
@Composable
private fun LockedContentExplanation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.togetherlyColors.backgroundSurface, shape = MaterialTheme.togetherlyShapes.medium)
            .padding(MaterialTheme.togetherlySpacing.m)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        Text(
            text = stringResource(Res.string.questdetail_locked_title),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.questdetail_locked_body),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}

@Composable
private fun QuestDetailSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * "Continue previous quest" (the third option the task describes) is not offered here — this
 * viewmodel doesn't currently learn the previous session's own completion/quest identity, only
 * that a conflict exists (see [QuestDetailViewModel.onStartClicked]'s own KDoc) — so
 * [onCancel] plays the role of both "keep the existing session untouched" and "cancel"; a future
 * pass can split it once the conflict carries the previous session's own navigable identity.
 */
@Composable
private fun ActiveSessionConflictDialog(onReplace: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.questdetail_conflict_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.questdetail_conflict_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = onReplace) { Text(stringResource(Res.string.questdetail_conflict_replace_action)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.questdetail_conflict_cancel_action)) }
        },
    )
}
