package com.togetherly.feature.family.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.family.model.FamilyProfileEditorEvent
import org.koin.compose.viewmodel.koinViewModel

/**
 * [onNavigateBack]/[onSaved] are the only ways this reaches outside itself. Both currently just
 * pop the back stack (see [com.togetherly.navigation.host.TogetherlyNavHost]'s `composable<RootDestination.FamilyProfileEditor>`
 * block) — kept as two separate callbacks rather than one, since a future change to what "saved"
 * does (e.g. a different return destination) shouldn't have to also change what plain Back does.
 */
@Composable
fun FamilyProfileEditorRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: FamilyProfileEditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FamilyProfileEditorEvent.SaveCompleted -> onSaved()
                FamilyProfileEditorEvent.NavigatedBackWithoutSaving -> onNavigateBack()
            }
        }
    }

    FamilyProfileEditorScreen(state = state, onAction = viewModel::onAction)
}
