package com.togetherly.designsystem.component.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_character_limit

/**
 * A single text input. Performs no domain validation of its own — [errorText] is entirely
 * feature-supplied; this component only ever renders whatever error state it's told to, never
 * decides what counts as invalid (a phone-number pattern, a required-field check, etc. all belong
 * to the caller). [characterLimit], likewise, only ever renders a counter — it does not clamp
 * [value] or judge whether being over the limit is an error; a caller wanting that pairs it with
 * its own [errorText].
 */
@Composable
fun TogetherlyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    characterLimit: Int? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.togetherlyColors
    val isError = errorText != null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = MaterialTheme.togetherlySize.inputHeight)
            .semantics { if (isError) error(errorText.orEmpty()) },
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.togetherlyShapes.medium,
        textStyle = MaterialTheme.togetherlyTypography.bodyL,
        supportingText = if (supportingText != null || errorText != null || characterLimit != null) {
            {
                TextFieldSupportingRow(
                    message = errorText ?: supportingText,
                    isError = isError,
                    value = value,
                    characterLimit = characterLimit,
                )
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.foregroundPrimary,
            unfocusedTextColor = colors.foregroundPrimary,
            disabledTextColor = colors.foregroundSecondary,
            focusedBorderColor = colors.actionPrimary,
            unfocusedBorderColor = colors.borderSubtle,
            disabledBorderColor = colors.borderSubtle,
            errorBorderColor = colors.error,
            focusedLabelColor = colors.actionPrimary,
            unfocusedLabelColor = colors.foregroundSecondary,
            errorLabelColor = colors.error,
            cursorColor = colors.actionPrimary,
            errorCursorColor = colors.error,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            errorSupportingTextColor = colors.error,
            focusedSupportingTextColor = colors.foregroundSecondary,
            unfocusedSupportingTextColor = colors.foregroundSecondary,
        ),
    )
}

@Composable
private fun TextFieldSupportingRow(
    message: String?,
    isError: Boolean,
    value: String,
    characterLimit: Int?,
) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier) {
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = if (isError) MaterialTheme.togetherlyColors.error else MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = Modifier.weight(1f),
        )
        if (characterLimit != null) {
            Text(
                text = stringResource(Res.string.ds_component_character_limit, value.length, characterLimit),
                style = MaterialTheme.togetherlyTypography.bodyS,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
    }
}
