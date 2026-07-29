package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.family.model.FamilyEvent
import com.togetherly.feature.familyplus.presentation.FamilyPlusStatusSection
import org.koin.compose.viewmodel.koinViewModel

/**
 * [onOpenPaywall]/[onOpenCustomerCenter] climb out for the embedded [FamilyPlusStatusSection];
 * [onOpenProfileEditor] climbs out for [com.togetherly.navigation.destination.RootDestination.FamilyProfileEditor].
 * Never a direct `NavHostController` reference here, same rule [FamilyPlusManagementRoute]'s own
 * KDoc states.
 */
@Composable
fun FamilyRoute(
    onOpenPaywall: () -> Unit,
    onOpenCustomerCenter: () -> Unit,
    onOpenProfileEditor: () -> Unit,
    onOpenQuestPreferences: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenMemorySettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLegal: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDataManagement: () -> Unit,
    viewModel: FamilyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FamilyEvent.OpenProfileEditor -> onOpenProfileEditor()
                FamilyEvent.OpenQuestPreferences -> onOpenQuestPreferences()
                FamilyEvent.OpenReminder -> onOpenReminder()
                FamilyEvent.OpenMemorySettings -> onOpenMemorySettings()
                FamilyEvent.OpenPrivacy -> onOpenPrivacy()
                FamilyEvent.OpenLegal -> onOpenLegal()
                FamilyEvent.OpenAbout -> onOpenAbout()
                FamilyEvent.OpenDataManagement -> onOpenDataManagement()
            }
        }
    }

    FamilyScreen(
        state = state,
        onAction = viewModel::onAction,
        familyPlusSection = {
            FamilyPlusStatusSection(onOpenPaywall = onOpenPaywall, onOpenCustomerCenter = onOpenCustomerCenter)
        },
    )
}
