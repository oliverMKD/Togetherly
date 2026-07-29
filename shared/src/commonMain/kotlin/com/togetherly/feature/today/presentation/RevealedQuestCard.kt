package com.togetherly.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.model.QuestCardUi
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_completed_action
import togetherly.shared.generated.resources.today_completed_indicator
import togetherly.shared.generated.resources.today_saved_content_description
import togetherly.shared.generated.resources.today_see_quest_action
import togetherly.shared.generated.resources.today_unsaved_content_description

/**
 * Shows summary metadata only — duration/location/preparation/energy/material count, never full
 * ordered instructions, hints or the safety note (Quest Detail owns those, Step 8.6). Category
 * color ([color]) is always paired with [today_category_talk]-style text elsewhere on the card,
 * never the only category signal (see [color]'s own KDoc).
 */
@Composable
internal fun RevealedQuestCard(
    quest: QuestCardUi,
    onAction: (TodayAction) -> Unit,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
) {
    TogetherlyCard(modifier = modifier.fillMaxWidth()) {
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
                SaveToggle(isSaved = quest.isSaved, onToggle = { onAction(TodayAction.SaveClicked) })
            }

            Text(
                text = quest.title,
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = quest.summary,
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )

            QuestMetadataRow(quest = quest)

            if (isCompleted) {
                Text(
                    text = stringResource(Res.string.today_completed_indicator),
                    style = MaterialTheme.togetherlyTypography.bodyS,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }

            TogetherlyPrimaryButton(
                label = stringResource(
                    if (isCompleted) Res.string.today_completed_action else Res.string.today_see_quest_action,
                ),
                onClick = { onAction(TodayAction.StartClicked) },
                enabled = !isCompleted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryBadge(quest: QuestCardUi, modifier: Modifier = Modifier) {
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
private fun SaveToggle(isSaved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (isSaved) Res.string.today_saved_content_description else Res.string.today_unsaved_content_description,
    )
    TogetherlyIconButton(
        icon = {
            Text(
                text = if (isSaved) "★" else "☆",
                style = MaterialTheme.togetherlyTypography.titleL,
            )
        },
        contentDescription = description,
        onClick = onToggle,
        modifier = modifier,
    )
}

@Composable
private fun QuestMetadataRow(quest: QuestCardUi, modifier: Modifier = Modifier) {
    val labels = listOfNotNull(
        quest.durationLabel.asString(),
        quest.locationLabel.asString(),
        quest.preparationLabel.asString(),
        quest.energyLabel.asString(),
        quest.materialSummary?.asString(),
    )
    val joined = labels.joinToString(separator = "  •  ")

    Text(
        text = joined,
        modifier = modifier.semantics { contentDescription = labels.joinToString(separator = ". ") },
        style = MaterialTheme.togetherlyTypography.bodyS,
        color = MaterialTheme.togetherlyColors.foregroundSecondary,
    )
}
