package com.togetherly.designsystem.component.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing

@Composable
private fun TextFieldShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            var normalValue by remember { mutableStateOf("") }
            TogetherlyTextField(
                value = normalValue,
                onValueChange = { normalValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Family name",
                placeholder = "The Rivera family",
            )

            var focusedValue by remember { mutableStateOf("Rivera") }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            TogetherlyTextField(
                value = focusedValue,
                onValueChange = { focusedValue = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = "Family name (focused)",
            )

            var errorValue by remember { mutableStateOf("") }
            TogetherlyTextField(
                value = errorValue,
                onValueChange = { errorValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Family name",
                errorText = "A family name is required",
                characterLimit = 30,
            )

            TogetherlyTextField(
                value = "Rivera",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Family name",
                enabled = false,
            )
        }
    }
}

@Preview
@Composable
private fun TextFieldShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { TextFieldShowcase() }
}

@Preview
@Composable
private fun TextFieldShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { TextFieldShowcase() }
}

@Preview(fontScale = 2f)
@Composable
private fun TextFieldShowcaseLargeFontScalePreview() {
    TogetherlyTheme(darkTheme = false) { TextFieldShowcase() }
}
