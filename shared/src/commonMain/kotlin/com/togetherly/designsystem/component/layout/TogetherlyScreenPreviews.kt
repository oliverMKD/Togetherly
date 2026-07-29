package com.togetherly.designsystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography

@Composable
private fun ScreenShowcase() {
    TogetherlyScreen(
        topBar = { TogetherlyTopBar(title = "Today") },
        bottomBar = {
            TogetherlyPrimaryButton(
                label = "Continue",
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            repeat(10) { index ->
                Text(
                    text = "Content line ${index + 1}",
                    style = MaterialTheme.togetherlyTypography.bodyL,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScreenShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { ScreenShowcase() }
}

@Preview
@Composable
private fun ScreenShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { ScreenShowcase() }
}
