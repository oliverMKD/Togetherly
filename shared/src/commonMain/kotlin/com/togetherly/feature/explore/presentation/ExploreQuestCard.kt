package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.presentation.color
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_quest_locked_content_description
import togetherly.shared.generated.resources.explore_quest_premium_badge
import togetherly.shared.generated.resources.explore_quest_saved_content_description
import togetherly.shared.generated.resources.explore_quest_unsaved_content_description

/**
 * Explore's own quest card — not [com.togetherly.feature.today.presentation.RevealedQuestCard],
 * which is `internal` to Today's package, hard-wired to [com.togetherly.feature.today.presentation.TodayAction],
 * and has no locked/premium fields to render (see [ExploreQuestUiModel]'s own KDoc for why). Reuses
 * Today's shared label/color mapper functions, not its card.
 *
 * [quest]'s locked state never dims the card — a small "Family Plus" badge (paired with the 🔒
 * glyph, never color alone) is the only visual difference from an unlocked card, matching this
 * feature's own task spec ("without making the content look disabled or unimportant").
 */
@Composable
internal fun ExploreQuestCard(
    quest: ExploreQuestUiModel,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.togetherlyColors

    TogetherlyCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.l),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(quest = quest)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
                    if (quest.isPremium) {
                        PremiumBadge(locked = quest.locked)
                    }
                    SaveToggle(isSaved = quest.isSaved, onToggle = onSaveClick)
                }
            }

            Text(
                text = quest.title,
                style = MaterialTheme.togetherlyTypography.titleM,
                color = colors.foregroundPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = quest.summary,
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = colors.foregroundSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            QuestMetadataRow(quest = quest)
        }
    }
}

@Composable
private fun CategoryBadge(quest: ExploreQuestUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.togetherlySpacing.s)
                .background(color = quest.category.color(), shape = CircleShape),
        )
        Text(
            text = quest.category.label().asString(),
            style = MaterialTheme.togetherlyTypography.labelM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}

@Composable
private fun PremiumBadge(locked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.togetherlyColors
    val label = stringResource(Res.string.explore_quest_premium_badge)
    val lockedDescription = stringResource(Res.string.explore_quest_locked_content_description)

    Row(
        modifier = modifier
            .background(colors.actionPrimary.copy(alpha = 0.12f), shape = MaterialTheme.togetherlyShapes.pill)
            .padding(horizontal = MaterialTheme.togetherlySpacing.xs, vertical = MaterialTheme.togetherlySpacing.xxs)
            .semantics(mergeDescendants = true) { contentDescription = if (locked) lockedDescription else label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        if (locked) {
            Text(text = "🔒", style = MaterialTheme.togetherlyTypography.labelM)
        }
        Text(text = label, style = MaterialTheme.togetherlyTypography.labelM, color = colors.actionPrimary)
    }
}

@Composable
private fun SaveToggle(isSaved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (isSaved) Res.string.explore_quest_saved_content_description else Res.string.explore_quest_unsaved_content_description,
    )
    TogetherlyIconButton(
        icon = { Text(text = if (isSaved) "★" else "☆", style = MaterialTheme.togetherlyTypography.titleL) },
        contentDescription = description,
        onClick = onToggle,
        modifier = modifier,
    )
}

@Composable
private fun QuestMetadataRow(quest: ExploreQuestUiModel, modifier: Modifier = Modifier) {
    val labels = listOf(quest.durationLabel.asString(), quest.locationLabel.asString(), quest.energyLabel.asString())
    val joined = labels.joinToString(separator = "  •  ")

    Text(
        text = joined,
        modifier = modifier.semantics { contentDescription = labels.joinToString(separator = ". ") },
        style = MaterialTheme.togetherlyTypography.bodyS,
        color = MaterialTheme.togetherlyColors.foregroundSecondary,
    )
}
