package com.togetherly.designsystem.component.feedback

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
private fun FeedbackShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            TogetherlyLoadingIndicator()
            TogetherlyLoadingIndicator(label = "Loading today's quest")
            TogetherlyInlineError(message = "We couldn't load today's quest.")
            TogetherlyInlineError(message = "We couldn't load today's quest.", onRetry = {})
        }
    }
}

@Preview
@Composable
private fun FeedbackShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { FeedbackShowcase() }
}

@Preview
@Composable
private fun FeedbackShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { FeedbackShowcase() }
}
