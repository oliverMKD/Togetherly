package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_welcome_body
import togetherly.shared.generated.resources.onboarding_welcome_privacy
import togetherly.shared.generated.resources.onboarding_welcome_title

/**
 * No form data — see [com.togetherly.feature.onboarding.model.OnboardingStep]'s own KDoc. The
 * visual below is a deliberately abstract placeholder built from plain Compose shapes (circles at
 * varying alpha), not a branded illustration — see this step's own spec note in the project's
 * step history for why a final asset is explicitly out of scope here.
 */
@Composable
internal fun WelcomeStepContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        WelcomePlaceholderVisual()
        Text(
            text = stringResource(Res.string.onboarding_welcome_title),
            style = MaterialTheme.togetherlyTypography.displayM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.onboarding_welcome_body),
            style = MaterialTheme.togetherlyTypography.bodyL,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        Text(
            text = stringResource(Res.string.onboarding_welcome_privacy),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}

/** Purely decorative — cleared from the semantics tree so a screen reader never announces it. */
@Composable
private fun WelcomePlaceholderVisual() {
    val colors = MaterialTheme.togetherlyColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .alpha(0.35f)
                .background(colors.categoryDiscover, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .alpha(0.55f)
                .background(colors.categoryCreate, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.actionPrimary, MaterialTheme.togetherlyShapes.circular),
        )
    }
}
