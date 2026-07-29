package com.togetherly.feature.familyplus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.familyplus.model.FamilyPlusManagementUiState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.familyplus_active_title
import togetherly.shared.generated.resources.familyplus_free_body
import togetherly.shared.generated.resources.familyplus_free_title
import togetherly.shared.generated.resources.familyplus_manage_subscription_action
import togetherly.shared.generated.resources.familyplus_provider_unavailable_notice
import togetherly.shared.generated.resources.familyplus_restore_action
import togetherly.shared.generated.resources.familyplus_view_plans_action

/**
 * Stateless — [FamilyPlusManagementRoute] is the only place that talks to
 * [FamilyPlusManagementViewModel] directly. Kept as a standalone full-screen wrapper around
 * [FamilyPlusManagementContent] for previews/tests; the Family tab root (Step 13.2) embeds
 * [FamilyPlusManagementContent] directly via [FamilyPlusStatusSection] instead, since it supplies
 * its own [TogetherlyScreen].
 * Never builds a custom cancellation/refund flow — those live only in RevenueCat's own Customer
 * Center (see [FamilyPlusManagementAction.ManageSubscriptionClicked]).
 */
@Composable
internal fun FamilyPlusManagementScreen(
    state: FamilyPlusManagementUiState,
    onAction: (FamilyPlusManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(modifier = modifier) {
        FamilyPlusManagementContent(state = state, onAction = onAction)
    }
}

/**
 * The FamilyPlus status content only — no [TogetherlyScreen]/scaffolding of its own, so it can be
 * embedded inside another screen's layout (see [FamilyPlusStatusSection]) without nesting scroll
 * containers.
 */
@Composable
internal fun FamilyPlusManagementContent(
    state: FamilyPlusManagementUiState,
    onAction: (FamilyPlusManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TogetherlyLoadingIndicator()
        }
        state.access.isPlus -> PremiumContent(state = state, onAction = onAction, modifier = modifier)
        else -> FreeContent(state = state, onAction = onAction, modifier = modifier)
    }
}

@Composable
private fun FreeContent(
    state: FamilyPlusManagementUiState,
    onAction: (FamilyPlusManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m)) {
        Text(
            text = stringResource(Res.string.familyplus_free_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.familyplus_free_body),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        if (!state.customerCenterAvailable) {
            ProviderUnavailableNotice()
        }
        TogetherlyPrimaryButton(
            label = stringResource(Res.string.familyplus_view_plans_action),
            onClick = { onAction(FamilyPlusManagementAction.ViewPlansClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
        RestoreAndMessage(state = state, onAction = onAction)
    }
}

@Composable
private fun PremiumContent(
    state: FamilyPlusManagementUiState,
    onAction: (FamilyPlusManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m)) {
        Text(
            text = stringResource(Res.string.familyplus_active_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        if (state.renewalInfo != null) {
            Text(
                text = state.renewalInfo.asString(),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }
        if (!state.customerCenterAvailable) {
            ProviderUnavailableNotice()
        }
        TogetherlyPrimaryButton(
            label = stringResource(Res.string.familyplus_manage_subscription_action),
            onClick = { onAction(FamilyPlusManagementAction.ManageSubscriptionClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
        RestoreAndMessage(state = state, onAction = onAction)
    }
}

/**
 * Small, non-alarming hint shown when [FamilyPlusManagementUiState.customerCenterAvailable] is
 * `false` — mirrors [com.togetherly.domain.purchase.PurchaseStartupState]'s own design intent of
 * never presenting a purchase-provider hiccup as a scary error; this only nudges that some
 * purchase-related actions may not work yet, without blocking the rest of the screen.
 */
@Composable
private fun ProviderUnavailableNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.familyplus_provider_unavailable_notice),
        style = MaterialTheme.togetherlyTypography.bodyS,
        color = MaterialTheme.togetherlyColors.foregroundSecondary,
        modifier = modifier,
    )
}

@Composable
private fun RestoreAndMessage(
    state: FamilyPlusManagementUiState,
    onAction: (FamilyPlusManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
        TogetherlyTextButton(
            label = stringResource(Res.string.familyplus_restore_action),
            loading = state.isRestoring,
            onClick = { onAction(FamilyPlusManagementAction.RestoreClicked) },
        )
        if (state.message != null) {
            TogetherlyInlineError(message = state.message.asString())
        }
    }
}
