package com.togetherly.feature.journey.ui

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyReduceMotion
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.journey.StarVisualVariant
import com.togetherly.feature.journey.mapper.copy
import com.togetherly.feature.journey.mapper.latestOrNull
import com.togetherly.feature.journey.model.JourneyStarUi
import com.togetherly.feature.journey.model.JourneySummaryUi
import com.togetherly.feature.today.presentation.color
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.journey_constellation_content_description
import togetherly.shared.generated.resources.journey_summary_total_completions

private val CONSTELLATION_HEIGHT = 140.dp

/**
 * The decorative starfield gets exactly one combined [contentDescription] (from
 * [Modifier.semantics] with `mergeDescendants = true`), never one per star — a screen reader user
 * doesn't need 40 individually-announced dots, only "here's how big your family's sky is". No star
 * inside is independently focusable/interactive, matching the accessibility audit item this
 * feature's spec calls out.
 */
@Composable
internal fun JourneyConstellationHeader(
    summary: JourneySummaryUi,
    stars: PersistentList<JourneyStarUi>,
    modifier: Modifier = Modifier,
) {
    val constellationDescription = stringResource(Res.string.journey_constellation_content_description, summary.totalCompletions)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(CONSTELLATION_HEIGHT)
                .clip(MaterialTheme.togetherlyShapes.card)
                .background(MaterialTheme.togetherlyColors.backgroundSurface)
                .semantics(mergeDescendants = true) { contentDescription = constellationDescription },
        ) {
            stars.forEach { star ->
                StarDot(star = star, areaWidth = maxWidth, areaHeight = maxHeight)
            }
        }

        Text(
            text = stringResource(Res.string.journey_summary_total_completions, summary.totalCompletions),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        summary.achievedMilestones.latestOrNull()?.let { milestone ->
            Text(
                text = milestone.copy().asString(),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
    }
}

@Composable
private fun StarDot(star: JourneyStarUi, areaWidth: Dp, areaHeight: Dp) {
    val diameter = when (star.visualVariant) {
        StarVisualVariant.SMALL -> 6.dp
        StarVisualVariant.MEDIUM -> 9.dp
        StarVisualVariant.LARGE -> 12.dp
    }
    val color = star.category?.color() ?: MaterialTheme.togetherlyColors.foregroundSecondary
    val alpha = rememberTwinkleAlpha(seed = star.completionId.value.hashCode())

    Box(
        modifier = Modifier
            .offset(x = areaWidth * star.position.x, y = areaHeight * star.position.y)
            .size(diameter)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun rememberTwinkleAlpha(seed: Int): Float {
    if (MaterialTheme.togetherlyReduceMotion) return 1f

    val infiniteTransition = rememberInfiniteTransition(label = "journey-star-twinkle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400 + (seed.mod(5)) * 220, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(seed.mod(7) * 90),
        ),
        label = "journey-star-alpha",
    )
    return alpha
}
