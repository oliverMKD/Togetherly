package com.togetherly.feature.family.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.LegalAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_back_content_description
import togetherly.shared.generated.resources.legal_open_source_licenses_action
import togetherly.shared.generated.resources.legal_privacy_policy_action
import togetherly.shared.generated.resources.legal_subscription_terms_action
import togetherly.shared.generated.resources.legal_terms_of_use_action
import togetherly.shared.generated.resources.legal_title

/** [external] picks the trailing glyph only (↗ vs ›) — [LegalViewModel] is the source of truth for which rows actually open a browser vs. navigate in-app. */
private data class LegalRow(val label: StringResource, val action: LegalAction, val external: Boolean)

private val LEGAL_ROWS = listOf(
    LegalRow(Res.string.legal_privacy_policy_action, LegalAction.PrivacyPolicyClicked, external = true),
    LegalRow(Res.string.legal_terms_of_use_action, LegalAction.TermsOfUseClicked, external = true),
    LegalRow(Res.string.legal_subscription_terms_action, LegalAction.SubscriptionTermsClicked, external = true),
    LegalRow(Res.string.legal_open_source_licenses_action, LegalAction.OpenSourceLicensesClicked, external = false),
)

@Composable
internal fun LegalScreen(
    onAction: (LegalAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.legal_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(LegalAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TogetherlyCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LEGAL_ROWS.forEachIndexed { index, row ->
                        LegalEntryRow(
                            title = stringResource(row.label),
                            external = row.external,
                            onClick = { onAction(row.action) },
                        )
                        if (index != LEGAL_ROWS.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = MaterialTheme.togetherlySpacing.m + EntryGlyphBadgeSize + MaterialTheme.togetherlySpacing.s,
                                ),
                                color = MaterialTheme.togetherlyColors.borderSubtle,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * External links get "↗" instead of "›" — this row genuinely leaves the app (opens a browser),
 * which is worth signaling differently from "Open source licenses" navigating to an in-app screen.
 * Both the badge and trailing glyph are decorative; the title text alone is this row's accessible
 * label (same convention as [FamilyScreen]'s own `SettingsEntryRow`).
 */
@Composable
private fun LegalEntryRow(title: String, external: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                .background(color = MaterialTheme.togetherlyColors.foregroundSecondary.copy(alpha = 0.12f), shape = CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "§", style = MaterialTheme.togetherlyTypography.titleM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
        }
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (external) "↗" else "›",
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

private val EntryGlyphBadgeSize = 40.dp
