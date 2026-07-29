package com.togetherly.designsystem.component.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing

@Composable
private fun StepProgressShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            TogetherlyStepProgress(currentStep = 1, totalSteps = 4)
            TogetherlyStepProgress(currentStep = 2, totalSteps = 4)
            TogetherlyStepProgress(currentStep = 4, totalSteps = 4)
            TogetherlyStepProgress(currentStep = 1, totalSteps = 1)
        }
    }
}

@Preview
@Composable
private fun StepProgressShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { StepProgressShowcase() }
}

@Preview
@Composable
private fun StepProgressShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { StepProgressShowcase() }
}
