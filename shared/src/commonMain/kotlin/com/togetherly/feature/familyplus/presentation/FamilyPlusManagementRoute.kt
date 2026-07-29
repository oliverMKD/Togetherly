package com.togetherly.feature.familyplus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * [onOpenPaywall]/[onOpenCustomerCenter] are the only ways this reaches outside itself — never a
 * direct `NavHostController` reference here or in [FamilyPlusManagementScreen].
 */
@Composable
fun FamilyPlusManagementRoute(
    onOpenPaywall: () -> Unit,
    onOpenCustomerCenter: () -> Unit,
    viewModel: FamilyPlusManagementViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FamilyPlusManagementEvent.OpenPaywall -> onOpenPaywall()
                FamilyPlusManagementEvent.OpenCustomerCenter -> onOpenCustomerCenter()
            }
        }
    }

    FamilyPlusManagementScreen(state = state, onAction = viewModel::onAction)
}

/**
 * Same wiring as [FamilyPlusManagementRoute] but renders [FamilyPlusManagementContent] with no
 * [com.togetherly.designsystem.component.layout.TogetherlyScreen] of its own, so the Family tab
 * root (Step 13.2) can embed it as one section inside its own single scrollable screen.
 */
@Composable
fun FamilyPlusStatusSection(
    onOpenPaywall: () -> Unit,
    onOpenCustomerCenter: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyPlusManagementViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FamilyPlusManagementEvent.OpenPaywall -> onOpenPaywall()
                FamilyPlusManagementEvent.OpenCustomerCenter -> onOpenCustomerCenter()
            }
        }
    }

    FamilyPlusManagementContent(state = state, onAction = viewModel::onAction, modifier = modifier)
}
