package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme

@Preview
@Composable
private fun PrivacySummaryPreview() {
    TogetherlyTheme {
        PrivacyScreen(onAction = {})
    }
}

@Preview(fontScale = 2f)
@Composable
private fun PrivacySummaryLargeTextPreview() {
    TogetherlyTheme {
        PrivacyScreen(onAction = {})
    }
}
