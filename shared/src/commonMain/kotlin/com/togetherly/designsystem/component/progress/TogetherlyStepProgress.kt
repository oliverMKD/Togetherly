package com.togetherly.designsystem.component.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_step_progress_description

/**
 * A horizontal step indicator for a multi-step flow (e.g. onboarding). [currentStep] is 1-indexed
 * against [totalSteps] (the first step is `1`, matching how "Step 1 of 3" reads aloud) — both are
 * [require]d into range since they're always caller-supplied constants describing a fixed flow,
 * never raw user input, so an out-of-range value is a caller bug worth failing fast on rather than
 * silently clamping.
 *
 * A completed segment is distinguished from a remaining one by *fill* (solid vs. outlined), not
 * only by color, so the distinction survives grayscale/color-blind viewing; each segment
 * [Modifier.weight]s equally so the whole bar adapts to whatever width it's given.
 */
@Composable
fun TogetherlyStepProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    require(totalSteps >= 1) { "totalSteps must be at least 1, was $totalSteps" }
    require(currentStep in 1..totalSteps) {
        "currentStep must be within 1..$totalSteps, was $currentStep"
    }

    val colors = MaterialTheme.togetherlyColors
    val description = stringResource(
        Res.string.ds_component_step_progress_description,
        currentStep,
        totalSteps,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        repeat(totalSteps) { index ->
            val completed = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .then(
                        if (completed) {
                            Modifier.background(colors.actionPrimary, MaterialTheme.togetherlyShapes.small)
                        } else {
                            Modifier.border(1.dp, colors.borderSubtle, MaterialTheme.togetherlyShapes.small)
                        },
                    ),
            )
        }
    }
}
