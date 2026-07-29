package com.togetherly.designsystem.component.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography

@Composable
private fun CardShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            TogetherlyCard {
                Text(
                    text = "A plain container card",
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
                )
            }
            TogetherlySelectableCard(
                selected = true,
                onClick = {},
                title = "Selected option",
                supportingText = "This one is currently picked",
                leadingContent = { PreviewLeadingDot() },
            )
            TogetherlySelectableCard(
                selected = false,
                onClick = {},
                title = "Unselected option",
                supportingText = "This one is not picked",
                leadingContent = { PreviewLeadingDot() },
            )
            TogetherlySelectableCard(
                selected = false,
                onClick = {},
                enabled = false,
                title = "Disabled option",
            )
        }
    }
}

@Composable
private fun PreviewLeadingDot() {
    Box(
        modifier = Modifier
            .size(MaterialTheme.togetherlySize.iconL)
            .background(MaterialTheme.togetherlyColors.categoryDiscover, CircleShape),
    )
}

@Preview
@Composable
private fun CardShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { CardShowcase() }
}

@Preview
@Composable
private fun CardShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { CardShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun CardShowcaseLargeFontScalePreview() {
    TogetherlyTheme(darkTheme = false) { CardShowcase() }
}
