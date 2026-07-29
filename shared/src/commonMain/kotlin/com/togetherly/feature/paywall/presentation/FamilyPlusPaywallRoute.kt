package com.togetherly.feature.paywall.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.feature.paywall.model.PaywallContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [FamilyPlusPaywallScreen] never receives a [androidx.navigation.NavHostController] or opens a
 * URL itself — this Route is the only place [FamilyPlusPaywallEvent.OpenExternalLink] resolves,
 * via Compose Multiplatform's own [LocalUriHandler] (works on both Android and iOS with no
 * additional platform code), matching [FamilyPlusPaywallEvent.PurchaseSucceeded]/[FamilyPlusPaywallEvent.RestoreSucceeded]
 * intentionally *not* closing the screen here — the screen itself switches to its "already premium"
 * content (driven by [com.togetherly.feature.paywall.model.FamilyPlusPaywallUiState.access]) so the
 * family sees the warm confirmation before choosing Continue, rather than being snapped back to
 * whatever screen opened the paywall.
 */
@Composable
fun FamilyPlusPaywallRoute(
    context: PaywallContext,
    onClose: () -> Unit,
    viewModel: FamilyPlusPaywallViewModel = koinViewModel { parametersOf(context) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FamilyPlusPaywallEvent.Close -> onClose()
                FamilyPlusPaywallEvent.PurchaseSucceeded, FamilyPlusPaywallEvent.RestoreSucceeded -> Unit
                is FamilyPlusPaywallEvent.OpenExternalLink -> uriHandler.openUri(event.url)
            }
        }
    }

    FamilyPlusPaywallScreen(state = state, onAction = viewModel::onAction)
}
