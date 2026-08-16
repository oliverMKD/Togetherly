package com.togetherly.feature.today.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyReduceMotion
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
            MysteryGlow(contentDescription = mysteryDescription)

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

/**
 * A slow, ever-so-slightly breathing halo behind the "?" mark — reads as "something is here,
 * waiting for you" rather than a static icon. The pulse is fully suppressed under reduced motion
 * by collapsing [pulseScale]'s target to its start value, not by skipping the animation call
 * itself, so this composable's structure never branches on [togetherlyReduceMotion] (see
 * [com.togetherly.designsystem.theme.togetherlyReduceMotion]'s own KDoc for why every consumer
 * should resolve motion through one seam).
 */
@Composable
private fun MysteryGlow(contentDescription: String, modifier: Modifier = Modifier) {
    val reduceMotion = MaterialTheme.togetherlyReduceMotion
    val pulseTransition = rememberInfiniteTransition(label = "mystery-pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mystery-pulse-scale",
    )
    val colors = MaterialTheme.togetherlyColors

    Box(modifier = modifier.size(OuterGlowSize), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(OuterGlowSize)
                .scale(pulseScale)
                .background(color = colors.actionPrimary.copy(alpha = 0.10f), shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .size(MiddleGlowSize)
                .background(color = colors.actionPrimary.copy(alpha = 0.18f), shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .size(CoreSize)
                .background(color = colors.actionPrimary, shape = CircleShape)
                .clearAndSetSemantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                style = MaterialTheme.togetherlyTypography.displayM,
                color = colors.actionPrimaryContent,
            )
        }
    }
}

private val CoreSize = 64.dp
private val MiddleGlowSize = 84.dp
private val OuterGlowSize = 104.dp
