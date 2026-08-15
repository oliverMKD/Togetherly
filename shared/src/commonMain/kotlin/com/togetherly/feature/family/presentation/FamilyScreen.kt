package com.togetherly.feature.family.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.FamilyAction
import com.togetherly.feature.family.model.FamilyUiState
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.about_title
import togetherly.shared.generated.resources.data_management_title
import togetherly.shared.generated.resources.family_entry_about_subtitle
import togetherly.shared.generated.resources.family_entry_data_management_subtitle
import togetherly.shared.generated.resources.family_entry_legal_subtitle
import togetherly.shared.generated.resources.family_entry_memory_settings_subtitle
import togetherly.shared.generated.resources.family_entry_privacy_subtitle
import togetherly.shared.generated.resources.family_entry_quest_preferences_subtitle
import togetherly.shared.generated.resources.family_entry_reminder_subtitle
import togetherly.shared.generated.resources.family_profile_edit_action
import togetherly.shared.generated.resources.family_profile_name_fallback
import togetherly.shared.generated.resources.family_profile_summary_context_label
import togetherly.shared.generated.resources.family_quest_preferences_action
import togetherly.shared.generated.resources.family_section_data
import togetherly.shared.generated.resources.family_section_memories_privacy
import togetherly.shared.generated.resources.family_section_support
import togetherly.shared.generated.resources.family_section_your_family
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
 * directly instead, the same stateless content [FamilyPlusStatusSection] itself renders). It sits
 * directly under the profile summary — subscription status is the one entry here with financial
 * stakes, so it gets top billing rather than being buried below every settings section.
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
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
            ) {
                if (state.message != null) {
                    TogetherlyInlineError(message = state.message.asString())
                }
                FamilyProfileSummarySection(state = state, onAction = onAction)
                familyPlusSection()

                SettingsSection(title = stringResource(Res.string.family_section_your_family)) {
                    SettingsEntryRow(
                        title = stringResource(Res.string.family_quest_preferences_action),
                        subtitle = stringResource(Res.string.family_entry_quest_preferences_subtitle),
                        glyph = "⚙",
                        tint = MaterialTheme.togetherlyColors.actionPrimary,
                        onClick = { onAction(FamilyAction.QuestPreferencesClicked) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        title = stringResource(Res.string.reminder_title),
                        subtitle = stringResource(Res.string.family_entry_reminder_subtitle),
                        glyph = "🔔",
                        tint = MaterialTheme.togetherlyColors.actionPrimary,
                        onClick = { onAction(FamilyAction.ReminderClicked) },
                    )
                }

                SettingsSection(title = stringResource(Res.string.family_section_memories_privacy)) {
                    SettingsEntryRow(
                        title = stringResource(Res.string.memory_settings_title),
                        subtitle = stringResource(Res.string.family_entry_memory_settings_subtitle),
                        glyph = "✧",
                        tint = MaterialTheme.togetherlyColors.positive,
                        onClick = { onAction(FamilyAction.MemorySettingsClicked) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        title = stringResource(Res.string.privacy_title),
                        subtitle = stringResource(Res.string.family_entry_privacy_subtitle),
                        glyph = "🔒",
                        tint = MaterialTheme.togetherlyColors.positive,
                        onClick = { onAction(FamilyAction.PrivacyClicked) },
                    )
                }

                SettingsSection(title = stringResource(Res.string.family_section_data)) {
                    SettingsEntryRow(
                        title = stringResource(Res.string.data_management_title),
                        subtitle = stringResource(Res.string.family_entry_data_management_subtitle),
                        glyph = "🗑",
                        tint = MaterialTheme.togetherlyColors.error,
                        onClick = { onAction(FamilyAction.DataManagementClicked) },
                    )
                }

                SettingsSection(title = stringResource(Res.string.family_section_support)) {
                    SettingsEntryRow(
                        title = stringResource(Res.string.legal_title),
                        subtitle = stringResource(Res.string.family_entry_legal_subtitle),
                        glyph = "§",
                        tint = MaterialTheme.togetherlyColors.foregroundSecondary,
                        onClick = { onAction(FamilyAction.LegalClicked) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        title = stringResource(Res.string.about_title),
                        subtitle = stringResource(Res.string.family_entry_about_subtitle),
                        glyph = "ⓘ",
                        tint = MaterialTheme.togetherlyColors.foregroundSecondary,
                        onClick = { onAction(FamilyAction.AboutClicked) },
                    )
                }
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

/** One labeled group of [SettingsEntryRow]s sharing a single card, matching a native grouped-settings-list look. */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.labelM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = Modifier.padding(start = MaterialTheme.togetherlySpacing.xs),
        )
        TogetherlyCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

/**
 * A single destination row: a tinted glyph badge, a title + one-line subtitle, and a trailing
 * chevron. The badge and chevron are decorative only — [title]/[subtitle] already carry the row's
 * full accessible label via [clickable]'s default merge, so both clear their own semantics rather
 * than having a screen reader read a raw glyph character or "greater-than" aloud.
 */
@Composable
private fun SettingsEntryRow(
    title: String,
    subtitle: String,
    glyph: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.togetherlySize.minimumTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(MaterialTheme.togetherlySpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
    ) {
        Box(
            modifier = Modifier
                .size(EntryGlyphBadgeSize)
                .background(color = tint.copy(alpha = 0.14f), shape = CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = glyph, style = MaterialTheme.togetherlyTypography.titleM, color = tint)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.togetherlyTypography.titleM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
            Text(text = subtitle, style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.foregroundSecondary)
        }
        Text(
            text = "›",
            style = MaterialTheme.togetherlyTypography.titleL,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Inset to align under each row's title/subtitle rather than spanning the full card width. */
@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(
            start = MaterialTheme.togetherlySpacing.m + EntryGlyphBadgeSize + MaterialTheme.togetherlySpacing.s,
        ),
        color = MaterialTheme.togetherlyColors.borderSubtle,
    )
}

private val EntryGlyphBadgeSize = 40.dp
