package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.LegalAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.legal_open_source_licenses_action
import togetherly.shared.generated.resources.legal_privacy_policy_action
import togetherly.shared.generated.resources.legal_subscription_terms_action
import togetherly.shared.generated.resources.legal_terms_of_use_action
import togetherly.shared.generated.resources.legal_title

private data class LegalRow(val label: StringResource, val action: LegalAction)

private val LEGAL_ROWS = listOf(
    LegalRow(Res.string.legal_privacy_policy_action, LegalAction.PrivacyPolicyClicked),
    LegalRow(Res.string.legal_terms_of_use_action, LegalAction.TermsOfUseClicked),
    LegalRow(Res.string.legal_subscription_terms_action, LegalAction.SubscriptionTermsClicked),
    LegalRow(Res.string.legal_open_source_licenses_action, LegalAction.OpenSourceLicensesClicked),
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
                        contentDescription = "Back",
                        onClick = { onAction(LegalAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            LEGAL_ROWS.forEach { row ->
                TogetherlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(row.action) },
                ) {
                    Text(
                        text = stringResource(row.label),
                        style = MaterialTheme.togetherlyTypography.titleM,
                        color = MaterialTheme.togetherlyColors.foregroundPrimary,
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
                    )
                }
            }
        }
    }
}
