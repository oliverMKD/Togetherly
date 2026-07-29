package com.togetherly.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing

/**
 * A plain colored dot — never a real product icon — standing in for a leading icon in previews so
 * this catalogue doesn't depend on any icon library (see this step's own scope note: production
 * icon assets aren't part of this pass).
 */
@Composable
private fun PreviewLeadingIcon() {
    Box(
        modifier = Modifier
            .size(MaterialTheme.togetherlySize.iconM)
            .background(MaterialTheme.togetherlyColors.foregroundOnAccent, CircleShape),
    )
}

@Composable
private fun ButtonStateShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            TogetherlyPrimaryButton(label = "Continue", onClick = {})
            TogetherlyPrimaryButton(label = "Continue", onClick = {}, enabled = false)
            TogetherlyPrimaryButton(label = "Continue", onClick = {}, loading = true)
            TogetherlyPrimaryButton(label = "Continue", onClick = {}, leadingIcon = { PreviewLeadingIcon() })

            TogetherlySecondaryButton(label = "Not today", onClick = {})
            TogetherlySecondaryButton(label = "Not today", onClick = {}, enabled = false)
            TogetherlySecondaryButton(label = "Not today", onClick = {}, loading = true)

            TogetherlyTextButton(label = "Skip", onClick = {})
            TogetherlyTextButton(label = "Skip", onClick = {}, enabled = false)
            TogetherlyTextButton(label = "Skip", onClick = {}, loading = true)

            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                TogetherlyIconButton(icon = { PreviewLeadingIcon() }, contentDescription = "Preview action", onClick = {})
                TogetherlyIconButton(icon = { PreviewLeadingIcon() }, contentDescription = "Preview action", onClick = {}, enabled = false)
            }
        }
    }
}

@Preview
@Composable
private fun ButtonStatesLightPreview() {
    TogetherlyTheme(darkTheme = false) { ButtonStateShowcase() }
}

@Preview
@Composable
private fun ButtonStatesDarkPreview() {
    TogetherlyTheme(darkTheme = true) { ButtonStateShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun ButtonStatesLargeFontScalePreview() {
    TogetherlyTheme(darkTheme = false) { ButtonStateShowcase() }
}
