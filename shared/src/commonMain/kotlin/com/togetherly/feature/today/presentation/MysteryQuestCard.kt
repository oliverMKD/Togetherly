package com.togetherly.feature.today.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_mystery_body
import togetherly.shared.generated.resources.today_mystery_content_description
import togetherly.shared.generated.resources.today_mystery_title
import togetherly.shared.generated.resources.today_reveal_action

/**
 * Never receives a [com.togetherly.feature.today.model.QuestCardUi] — there is nothing here for a
 * screen reader to merge into hidden-content text by accident. [clearAndSetSemantics] on the
 * decorative visual collapses its own subtree into one merged [today_mystery_content_description],
 * so it never fragments into multiple unlabeled nodes.
 */
@Composable
internal fun MysteryQuestCard(onReveal: () -> Unit, modifier: Modifier = Modifier) {
    var revealRequested by remember { mutableStateOf(false) }
    val mysteryDescription = stringResource(Res.string.today_mystery_content_description)

    TogetherlyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.togetherlySize.iconL * 2)
                    .background(color = MaterialTheme.togetherlyColors.actionPrimary, shape = CircleShape)
                    .clearAndSetSemantics { contentDescription = mysteryDescription },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.togetherlyTypography.displayM,
                    color = MaterialTheme.togetherlyColors.actionPrimaryContent,
                )
            }

            Text(
                text = stringResource(Res.string.today_mystery_title),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(Res.string.today_mystery_body),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )

            TogetherlyPrimaryButton(
                label = stringResource(Res.string.today_reveal_action),
                enabled = !revealRequested,
                onClick = {
                    revealRequested = true
                    onReveal()
                },
            )
        }
    }
}
