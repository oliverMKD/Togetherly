package com.togetherly.designsystem.component.gate

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme

@Preview
@Composable
private fun TogetherlyParentalGateDialogPreview() {
    TogetherlyTheme {
        TogetherlyParentalGateDialog(onConfirmed = {}, onDismiss = {})
    }
}
