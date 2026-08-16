package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.today.presentation.color
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_pack_free
import togetherly.shared.generated.resources.explore_pack_locked_content_description
import togetherly.shared.generated.resources.explore_pack_premium
import togetherly.shared.generated.resources.explore_pack_quest_count

private val FEATURED_CARD_WIDTH = 220.dp
private val HERO_HEIGHT = 64.dp

/**
 * Never a photo — a colored "hero" band plus a solid circle stand in for illustration (this
 * feature's own task spec: "Do not use remote stock photography... use shapes, colors, icons and
 * subtle illustration-like composition"). [pack]'s [ExplorePackUiModel.locked] never dims or
 * grays the card — only the small lock glyph in the corner and the access label communicate it,
 * both paired with visible text, never color alone.
 */
@Composable
internal fun ExplorePackCard(
    pack: ExplorePackUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
) {
    val colors = MaterialTheme.togetherlyColors
    val accessLabel = stringResource(if (pack.isPremium) Res.string.explore_pack_premium else Res.string.explore_pack_free)
    val lockedDescription = stringResource(Res.string.explore_pack_locked_content_description)
    val questCountLabel = pluralStringResource(Res.plurals.explore_pack_quest_count, pack.questCount, pack.questCount)
    val cardDescription = buildString {
        append(pack.title)
        append(". ")
        append(accessLabel)
        if (pack.locked) {
            append(". ")
            append(lockedDescription)
        }
    }

    TogetherlyCard(
        onClick = onClick,
        modifier = modifier
            .then(if (featured) Modifier.width(FEATURED_CARD_WIDTH) else Modifier.fillMaxWidth())
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HERO_HEIGHT)
                    .background(pack.theme.color().copy(alpha = 0.16f), shape = MaterialTheme.togetherlyShapes.medium),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(MaterialTheme.togetherlySpacing.xl)
                        .background(pack.theme.color(), shape = CircleShape),
                )
                if (pack.locked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MaterialTheme.togetherlySpacing.xxs)
                            .background(colors.backgroundSurface, shape = MaterialTheme.togetherlyShapes.pill)
                            .padding(horizontal = MaterialTheme.togetherlySpacing.xs, vertical = MaterialTheme.togetherlySpacing.xxs),
                    ) {
                        Text(text = "🔒", style = MaterialTheme.togetherlyTypography.labelM)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
            ) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.togetherlyTypography.titleM,
                    color = colors.foregroundPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pack.description,
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    color = colors.foregroundSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = accessLabel,
                        style = MaterialTheme.togetherlyTypography.labelM,
                        color = if (pack.isPremium) colors.actionPrimary else colors.foregroundSecondary,
                    )
                    Text(text = "•", style = MaterialTheme.togetherlyTypography.labelM, color = colors.foregroundSecondary)
                    Text(text = questCountLabel, style = MaterialTheme.togetherlyTypography.labelM, color = colors.foregroundSecondary)
                    pack.durationLabel?.let { label ->
                        Text(text = "•", style = MaterialTheme.togetherlyTypography.labelM, color = colors.foregroundSecondary)
                        Text(text = label.asString(), style = MaterialTheme.togetherlyTypography.labelM, color = colors.foregroundSecondary)
                    }
                }
            }
        }
    }
}
