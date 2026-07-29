package com.togetherly.feature.memory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.input.TogetherlyTextField
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_note_heading
import togetherly.shared.generated.resources.memory_note_placeholder

@Composable
internal fun NoteSection(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.memory_note_heading),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        TogetherlyTextField(
            value = state.note,
            onValueChange = { onAction(CompletionMemoryAction.NoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.memory_note_placeholder),
            errorText = state.noteError?.asString(),
            enabled = !state.isSaving,
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            characterLimit = MemoryNote.MAX_LENGTH,
        )
    }
}
