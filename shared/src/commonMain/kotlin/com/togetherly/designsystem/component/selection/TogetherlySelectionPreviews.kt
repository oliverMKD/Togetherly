package com.togetherly.designsystem.component.selection

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
private fun ChoiceChipShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m)) {
            TogetherlyChoiceChip(label = "Talk", selected = true, onClick = {})
            TogetherlyChoiceChip(label = "Create", selected = false, onClick = {})
            TogetherlyChoiceChip(label = "Move", selected = false, onClick = {})
            TogetherlyChoiceChip(label = "Kindness", selected = false, onClick = {})
            TogetherlyChoiceChip(label = "Discover", selected = false, onClick = {}, enabled = false)
        }
    }
}

@Preview
@Composable
private fun ChoiceChipShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { ChoiceChipShowcase() }
}

@Preview
@Composable
private fun ChoiceChipShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { ChoiceChipShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun ChoiceChipShowcaseLargeFontScalePreview() {
    TogetherlyTheme(darkTheme = false) { ChoiceChipShowcase() }
}
