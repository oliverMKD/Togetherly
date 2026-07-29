package com.togetherly.feature.paywall.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.card.TogetherlySelectableCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.feature.paywall.model.FamilyPlusPaywallUiState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.paywall_already_premium_continue_action
import togetherly.shared.generated.resources.paywall_already_premium_title
import togetherly.shared.generated.resources.paywall_benefit_moods
import togetherly.shared.generated.resources.paywall_benefit_quest_packs
import togetherly.shared.generated.resources.paywall_benefit_rerolls
import togetherly.shared.generated.resources.paywall_benefit_support
import togetherly.shared.generated.resources.paywall_close_content_description
import togetherly.shared.generated.resources.paywall_privacy_policy_action
import togetherly.shared.generated.resources.paywall_purchase_action
import togetherly.shared.generated.resources.paywall_recommended_label
import togetherly.shared.generated.resources.paywall_restore_action
import togetherly.shared.generated.resources.paywall_terms_of_use_action
import togetherly.shared.generated.resources.paywall_title
import togetherly.shared.generated.resources.purchase_error_unknown
import togetherly.shared.generated.resources.purchase_success_message

/**
 * Stateless — [FamilyPlusPaywallRoute] is the only place that talks to [FamilyPlusPaywallViewModel]
 * directly. Never claims a feature this app doesn't actually have: no cloud backup, no
 * cross-device sync, no unlimited family members, no shared online accounts — only the four
 * benefits this feature's own task spec lists. Never shows a fabricated "best value" percentage;
 * [state.access.isPlus] switches to [AlreadyPremiumContent] rather than the purchase flow whenever
 * Family Plus is already active, whether the family arrived here already premium or just purchased
 * moments ago.
 */
@Composable
internal fun FamilyPlusPaywallScreen(
    state: FamilyPlusPaywallUiState,
    onAction: (FamilyPlusPaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("×", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.paywall_close_content_description),
                        onClick = { onAction(FamilyPlusPaywallAction.CloseClicked) },
                    )
                },
            )
        },
        bottomBar = {
            if (!state.access.isPlus && state.packages.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m)) {
                    TogetherlyPrimaryButton(
                        label = stringResource(Res.string.paywall_purchase_action),
                        loading = state.isPurchasing,
                        enabled = state.selectedPackageId != null && !state.isRestoring,
                        onClick = { onAction(FamilyPlusPaywallAction.PurchaseClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TogetherlyTextButton(
                        label = stringResource(Res.string.paywall_restore_action),
                        loading = state.isRestoring,
                        enabled = !state.isPurchasing,
                        onClick = { onAction(FamilyPlusPaywallAction.RestoreClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        when {
            state.access.isPlus -> AlreadyPremiumContent(onContinue = { onAction(FamilyPlusPaywallAction.CloseClicked) })
            state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            state.packages.isEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TogetherlyInlineError(
                    message = (state.error ?: UiText.Resource(Res.string.purchase_error_unknown)).asString(),
                    onRetry = { onAction(FamilyPlusPaywallAction.RetryClicked) },
                )
            }
            else -> PaywallOfferContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun PaywallOfferContent(
    state: FamilyPlusPaywallUiState,
    onAction: (FamilyPlusPaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l)) {
        Text(
            text = stringResource(Res.string.paywall_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        if (state.introMessage != null) {
            Text(
                text = state.introMessage.asString(),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            listOf(
                stringResource(Res.string.paywall_benefit_rerolls),
                stringResource(Res.string.paywall_benefit_quest_packs),
                stringResource(Res.string.paywall_benefit_moods),
                stringResource(Res.string.paywall_benefit_support),
            ).forEach { benefit ->
                Text(
                    text = "•  $benefit",
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    color = MaterialTheme.togetherlyColors.foregroundPrimary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            state.packages.forEach { purchasePackage ->
                PackageOption(
                    purchasePackage = purchasePackage,
                    selected = purchasePackage.productId == state.selectedPackageId,
                    onClick = { onAction(FamilyPlusPaywallAction.PackageSelected(purchasePackage.productId)) },
                )
            }
        }

        if (state.error != null) {
            TogetherlyInlineError(message = state.error.asString())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m)) {
            TogetherlyTextButton(
                label = stringResource(Res.string.paywall_privacy_policy_action),
                onClick = { onAction(FamilyPlusPaywallAction.PrivacyPolicyClicked) },
            )
            TogetherlyTextButton(
                label = stringResource(Res.string.paywall_terms_of_use_action),
                onClick = { onAction(FamilyPlusPaywallAction.TermsClicked) },
            )
        }
    }
}

@Composable
private fun PackageOption(
    purchasePackage: PurchasePackage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (purchasePackage.type == PurchasePackageType.ANNUAL) {
            Text(
                text = stringResource(Res.string.paywall_recommended_label),
                style = MaterialTheme.togetherlyTypography.labelM,
                color = MaterialTheme.togetherlyColors.actionPrimary,
                modifier = Modifier.padding(bottom = MaterialTheme.togetherlySpacing.xxs),
            )
        }
        TogetherlySelectableCard(
            selected = selected,
            onClick = onClick,
            title = purchasePackage.title,
            supportingText = purchasePackage.formattedPrice,
        )
    }
}

@Composable
private fun AlreadyPremiumContent(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Text(
            text = stringResource(Res.string.paywall_already_premium_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.purchase_success_message),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        TogetherlyPrimaryButton(
            label = stringResource(Res.string.paywall_already_premium_continue_action),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
