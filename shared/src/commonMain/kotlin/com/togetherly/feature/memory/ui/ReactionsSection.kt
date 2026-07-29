package com.togetherly.feature.memory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.feature.memory.mapper.emoji
import com.togetherly.feature.memory.mapper.label
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_reactions_heading

@Composable
internal fun ReactionsSection(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.memory_reactions_heading),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        TogetherlyChipFlowRow {
            FamilyReaction.entries.forEach { reaction ->
                TogetherlyChoiceChip(
                    label = reaction.label().asString(),
                    selected = reaction in state.reactions,
                    onClick = { onAction(CompletionMemoryAction.ReactionToggled(reaction)) },
                    enabled = !state.isSaving,
                    icon = { Text(reaction.emoji()) },
                )
            }
        }
    }
}
