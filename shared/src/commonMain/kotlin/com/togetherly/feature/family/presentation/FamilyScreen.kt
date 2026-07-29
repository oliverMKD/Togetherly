package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.FamilyAction
import com.togetherly.feature.family.model.FamilyUiState
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.about_title
import togetherly.shared.generated.resources.data_management_title
import togetherly.shared.generated.resources.family_profile_edit_action
import togetherly.shared.generated.resources.family_profile_name_fallback
import togetherly.shared.generated.resources.family_profile_summary_context_label
import togetherly.shared.generated.resources.family_quest_preferences_action
import togetherly.shared.generated.resources.family_title
import togetherly.shared.generated.resources.legal_title
import togetherly.shared.generated.resources.memory_settings_title
import togetherly.shared.generated.resources.privacy_title
import togetherly.shared.generated.resources.reminder_title

/**
 * [familyPlusSection] is a slot rather than [FamilyPlusStatusSection][com.togetherly.feature.familyplus.presentation.FamilyPlusStatusSection]
 * called directly, so this screen stays previewable/testable without a live Koin graph — that
 * section resolves its own `FamilyPlusManagementViewModel` via `koinViewModel()`, which previews
 * never provide (see this file's own Previews file: it passes `FamilyPlusManagementContent`
 * directly instead, the same stateless content [FamilyPlusStatusSection] itself renders).
 */
@Composable
internal fun FamilyScreen(
    state: FamilyUiState,
    onAction: (FamilyAction) -> Unit,
    familyPlusSection: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = { TogetherlyTopBar(title = stringResource(Res.string.family_title)) },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            state.error != null -> TogetherlyInlineError(message = state.error.asString())
            else -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
            ) {
                if (state.message != null) {
                    TogetherlyInlineError(message = state.message.asString())
                }
                FamilyProfileSummarySection(state = state, onAction = onAction)
                QuestPreferencesEntrySection(onAction = onAction)
                ReminderEntrySection(onAction = onAction)
                MemorySettingsEntrySection(onAction = onAction)
                PrivacyEntrySection(onAction = onAction)
                LegalEntrySection(onAction = onAction)
                AboutEntrySection(onAction = onAction)
                DataManagementEntrySection(onAction = onAction)
                familyPlusSection()
            }
        }
    }
}

/**
 * Shows only fields that actually exist on [com.togetherly.domain.family.FamilyProfile]: display
 * name, broad age groups, and preferred activity contexts (location + duration). Deliberately never
 * shows a child profile photo, exact birth date, full child name, public username, location, or
 * online status — none of those exist on the domain model in the first place (see
 * [com.togetherly.domain.family.AgeBand]'s own KDoc: "never a child identity"), so there is nothing
 * to accidentally leak here. There is also no participant count — see
 * [com.togetherly.feature.family.model.FamilyProfileUiState]'s own KDoc for why one was not added.
 */
@Composable
private fun FamilyProfileSummarySection(
    state: FamilyUiState,
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
        ) {
            Text(
                text = state.familyName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.family_profile_name_fallback),
                style = MaterialTheme.togetherlyTypography.headlineM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            if (state.ageBands.isNotEmpty()) {
                Text(
                    text = state.ageBands.sortedBy { it.ordinal }.map { it.label() }.joinToString(),
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
            val contexts = buildList {
                add(state.locationPreference.label())
                addAll(state.preferredDurations.sortedBy { it.ordinal }.map { it.label() })
            }
            if (contexts.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.family_profile_summary_context_label, contexts.joinToString()),
                    style = MaterialTheme.togetherlyTypography.bodyS,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
            }
            TogetherlySecondaryButton(
                label = stringResource(Res.string.family_profile_edit_action),
                onClick = { onAction(FamilyAction.EditProfileClicked) },
            )
        }
    }
}

/**
 * Fills the "Preferences section" this screen's own Step 13.2 KDoc deliberately left as a gap —
 * quest preferences (duration/energy/location/preparation ranking signals, Step 13.3) get their
 * own dedicated screen rather than being crammed into this summary card.
 */
@Composable
private fun QuestPreferencesEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.QuestPreferencesClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.family_quest_preferences_action),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

@Composable
private fun ReminderEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.ReminderClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.reminder_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

@Composable
private fun MemorySettingsEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.MemorySettingsClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.memory_settings_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

@Composable
private fun PrivacyEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.PrivacyClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.privacy_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

@Composable
private fun LegalEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.LegalClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.legal_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

/** Reuses `data_management_title` as the label, same "reuse the destination's own title" convention every other entry section here follows. */
@Composable
private fun DataManagementEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.DataManagementClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.data_management_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}

@Composable
private fun AboutEntrySection(
    onAction: (FamilyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onAction(FamilyAction.AboutClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.about_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
        }
    }
}
