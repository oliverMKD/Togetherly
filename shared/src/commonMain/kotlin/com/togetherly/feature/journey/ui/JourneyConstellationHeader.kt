package com.togetherly.feature.journey.ui

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.sqrt
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.journey_constellation_content_description
import togetherly.shared.generated.resources.journey_summary_active_days
import togetherly.shared.generated.resources.journey_summary_total_completions

private val CONSTELLATION_HEIGHT = 140.dp

/**
 * Only the most recent [CONSTELLATION_TRAIL_SIZE] stars are eligible to be connected by a line —
 * a real constellation is a handful of nearby points traced into a small figure, never every star
 * in the sky chained into one zigzag. Capping by recency (rather than, say, drawing the whole
 * history) also doubles as a second "this is fresh" cue alongside [rememberGlowAlpha]'s halo on
 * the single newest star.
 */
private const val CONSTELLATION_TRAIL_SIZE = 6

/**
 * Even within the recent trail, a pair of consecutive-in-time stars can still land on opposite
 * corners of the panel — position is derived from a hash of the completion ID ([JourneyStarUi]
 * has no spatial relationship to time). Skipping a line past this length means the trail reads as
 * a loose, believable little figure with natural gaps, not a line stretched corner-to-corner. This
 * is a real screen length (compared against actual rendered pixel distance in [ConstellationLines]),
 * not a fraction of the panel's diagonal — the panel is much wider than it is tall, so a
 * fraction-space threshold would silently allow long horizontal lines while forbidding short-looking
 * vertical ones.
 */
private val MAX_CONSTELLATION_LINE_LENGTH = 100.dp

/**
 * The decorative starfield gets exactly one combined [contentDescription] (from
 * [Modifier.semantics] with `mergeDescendants = true`), never one per star — a screen reader user
 * doesn't need 40 individually-announced dots, only "here's how big your family's sky is". No star
 * inside is independently focusable/interactive, matching the accessibility audit item this
 * feature's spec calls out. The chronological connecting lines, the newest-star glow, and the
 * rich-memory rings are all drawn purely decoratively for the same reason — [contentDescription]
 * is updated in prose instead of adding more semantics nodes.
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
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.togetherlyColors.backgroundElevated, MaterialTheme.togetherlyColors.backgroundSurface),
                    ),
                )
                .semantics(mergeDescendants = true) { contentDescription = constellationDescription },
        ) {
            ConstellationLines(stars = stars, modifier = Modifier.fillMaxSize())

            stars.forEachIndexed { index, star ->
                StarDot(star = star, areaWidth = maxWidth, areaHeight = maxHeight, isNewest = index == 0)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
            Text(
                text = stringResource(Res.string.journey_summary_total_completions, summary.totalCompletions),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            if (summary.activeDayCount > 0) {
                Text(
                    text = pluralStringResource(Res.plurals.journey_summary_active_days, summary.activeDayCount, summary.activeDayCount),
                    style = MaterialTheme.togetherlyTypography.labelM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
        }

        summary.achievedMilestones.latestOrNull()?.let { milestone ->
            MilestoneBadge(text = milestone.copy().asString())
        }
    }
}

/**
 * A faint solid trail through only the most recent [CONSTELLATION_TRAIL_SIZE] stars — see that
 * constant's KDoc for why the whole history is never connected, and [MAX_CONSTELLATION_LINE_LENGTH]
 * for why even this short trail can have gaps in it. Solid rather than dashed: a dash pattern reads
 * as a route on a map, where a plain thin line reads as a star chart. Purely decorative: drawn once
 * per pair, never per-star, so it can't be mistaken for an interactive element by a screen reader.
 */
@Composable
private fun ConstellationLines(stars: PersistentList<JourneyStarUi>, modifier: Modifier = Modifier) {
    val trail = stars.take(CONSTELLATION_TRAIL_SIZE)
    if (trail.size < 2) return

    val lineColor = MaterialTheme.togetherlyColors.foregroundSecondary

    Canvas(modifier = modifier) {
        val maxLineLengthPx = MAX_CONSTELLATION_LINE_LENGTH.toPx()
        for (index in 0 until trail.size - 1) {
            val start = Offset(size.width * trail[index].position.x, size.height * trail[index].position.y)
            val end = Offset(size.width * trail[index + 1].position.x, size.height * trail[index + 1].position.y)
            val dx = end.x - start.x
            val dy = end.y - start.y
            if (sqrt(dx * dx + dy * dy) > maxLineLengthPx) continue

            drawLine(
                color = lineColor.copy(alpha = 0.22f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The latest achieved milestone gets a tinted pill rather than plain secondary text — a small
 * "you got here together" flourish for what is otherwise the app's one piece of pure positive
 * reinforcement copy. Same alpha-tint-background + solid-foreground pattern as
 * [com.togetherly.feature.explore.presentation.ExploreQuestCard]'s `PremiumBadge`.
 */
@Composable
private fun MilestoneBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color = MaterialTheme.togetherlyColors.positive.copy(alpha = 0.14f), shape = MaterialTheme.togetherlyShapes.pill)
            .padding(horizontal = MaterialTheme.togetherlySpacing.s, vertical = MaterialTheme.togetherlySpacing.xxs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.togetherlyTypography.labelM,
            color = MaterialTheme.togetherlyColors.positive,
        )
    }
}

/**
 * [isNewest] stars (index 0 — [stars] is newest-first) get a soft radial glow behind them, a "you
 * are here" marker for the most recent shared memory. Stars whose completion carries a photo or
 * voice memory ([JourneyStarUi.hasPhoto]/[JourneyStarUi.hasVoice]) get a thin ring instead of
 * staying a plain dot — both signals were already computed by [com.togetherly.domain.journey.JourneyStarPolicy]
 * but previously unused here. [LARGE][StarVisualVariant.LARGE] stars get their own smaller, static
 * halo and a brighter twinkle ceiling ([rememberTwinkleAlpha]'s `maxAlpha`) purely for the
 * depth cue a real sky has — some stars read as closer/brighter than others — never as a
 * significance signal the way [isNewest] and [hasRichMemory] are; [StarVisualVariant] itself
 * stays a stable hash with no meaning attached.
 */
@Composable
private fun StarDot(star: JourneyStarUi, areaWidth: Dp, areaHeight: Dp, isNewest: Boolean) {
    val diameter = when (star.visualVariant) {
        StarVisualVariant.SMALL -> 6.dp
        StarVisualVariant.MEDIUM -> 9.dp
        StarVisualVariant.LARGE -> 12.dp
    }
    val maxAlpha = when (star.visualVariant) {
        StarVisualVariant.SMALL -> 0.75f
        StarVisualVariant.MEDIUM -> 0.9f
        StarVisualVariant.LARGE -> 1f
    }
    val color = star.category?.color() ?: MaterialTheme.togetherlyColors.foregroundSecondary
    val twinkleAlpha = rememberTwinkleAlpha(seed = star.completionId.value.hashCode(), maxAlpha = maxAlpha)
    val hasRichMemory = star.hasPhoto || star.hasVoice

    Box(
        modifier = Modifier
            .offset(x = areaWidth * star.position.x, y = areaHeight * star.position.y)
            .size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        if (isNewest) {
            val glowAlpha = rememberGlowAlpha()
            Box(
                modifier = Modifier
                    .size(diameter * 3f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.5f * glowAlpha), color.copy(alpha = 0f)),
                        ),
                        shape = CircleShape,
                    ),
            )
        } else if (star.visualVariant == StarVisualVariant.LARGE) {
            Box(
                modifier = Modifier
                    .size(diameter * 2f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        if (hasRichMemory) {
            Box(
                modifier = Modifier
                    .size(diameter + 6.dp)
                    .alpha(twinkleAlpha)
                    .border(width = 1.dp, color = color.copy(alpha = 0.6f), shape = CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(diameter)
                .alpha(twinkleAlpha)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun rememberTwinkleAlpha(seed: Int, maxAlpha: Float): Float {
    if (MaterialTheme.togetherlyReduceMotion) return maxAlpha

    val infiniteTransition = rememberInfiniteTransition(label = "journey-star-twinkle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = maxAlpha * 0.55f,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400 + (seed.mod(5)) * 220, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(seed.mod(7) * 90),
        ),
        label = "journey-star-alpha",
    )
    return alpha
}

/** A slower, gentler pulse than [rememberTwinkleAlpha] — this marks "newest", not "twinkling", so it reads as a single steady beacon rather than shimmer. */
@Composable
private fun rememberGlowAlpha(): Float {
    if (MaterialTheme.togetherlyReduceMotion) return 0.85f

    val infiniteTransition = rememberInfiniteTransition(label = "journey-star-glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "journey-star-glow-alpha",
    )
    return alpha
}
