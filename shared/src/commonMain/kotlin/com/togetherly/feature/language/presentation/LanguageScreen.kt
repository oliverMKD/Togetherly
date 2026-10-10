package com.togetherly.feature.language.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.language.model.LanguageAction
import com.togetherly.feature.language.model.LanguageUiState
import com.togetherly.feature.language.model.SupportedLanguage
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_back_content_description
import togetherly.shared.generated.resources.language_restart_required
import togetherly.shared.generated.resources.language_system_default
import togetherly.shared.generated.resources.language_title

@Composable
internal fun LanguageScreen(
    state: LanguageUiState,
    onAction: (LanguageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.language_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(LanguageAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            if (state.showRestartPrompt) {
                RestartRequiredBanner(onDismiss = { onAction(LanguageAction.RestartPromptDismissed) })
            }

            TogetherlyCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LanguageRow(
                        flag = "🌐",
                        name = stringResource(Res.string.language_system_default),
                        selected = state.selectedCode == null,
                        onClick = { onAction(LanguageAction.LanguageSelected(null)) },
                    )
                    SupportedLanguage.entries.forEach { language ->
                        LanguageRowDivider()
                        LanguageRow(
                            flag = language.flag,
                            name = language.displayName,
                            selected = state.selectedCode == language.code,
                            onClick = { onAction(LanguageAction.LanguageSelected(language.code)) },
                        )
                    }
                }
            }
        }
    }
}

/** A one-time, dismissible note — iOS only ([LanguageUiState.showRestartPrompt] is never true on Android, see [LanguageViewModel]'s own KDoc). */
@Composable
private fun RestartRequiredBanner(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onDismiss, role = Role.Button)
            .background(
                color = MaterialTheme.togetherlyColors.positive.copy(alpha = 0.14f),
                shape = RoundedCornerShape(MaterialTheme.togetherlySpacing.s),
            )
            .padding(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.language_restart_required),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.positive,
        )
    }
}

/**
 * Same row shape as `SettingsEntryRow` in `FamilyScreen.kt` — a flag glyph in place of a tinted
 * badge, and a trailing checkmark in place of a chevron, since this list selects one of several
 * options rather than drilling into another screen. [Modifier.selectable] (not plain `clickable`)
 * so a screen reader announces the row's selected state, the correct semantics for a single-choice
 * list like this one.
 */
@Composable
private fun LanguageRow(flag: String, name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.togetherlySize.minimumTouchTarget)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(MaterialTheme.togetherlySpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
    ) {
        Text(text = flag, style = MaterialTheme.togetherlyTypography.titleL, modifier = Modifier.clearAndSetSemantics {})
        Text(
            text = name,
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.actionPrimary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun LanguageRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = MaterialTheme.togetherlySpacing.m),
        color = MaterialTheme.togetherlyColors.borderSubtle,
    )
}
