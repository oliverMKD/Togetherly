package com.togetherly.navigation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.revenuecat.purchases.kmp.ui.revenuecatui.CustomerCenter
import com.togetherly.designsystem.component.gate.TogetherlyParentalGateDialog
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.completion.ui.CompletionCelebrationRoute
import com.togetherly.feature.debug.presentation.DebugTelemetryRoute
import com.togetherly.feature.explore.presentation.ExploreFiltersRoute
import com.togetherly.feature.family.presentation.AboutRoute
import com.togetherly.feature.family.presentation.DataManagementRoute
import com.togetherly.feature.family.presentation.FamilyProfileEditorRoute
import com.togetherly.feature.family.presentation.LegalRoute
import com.togetherly.feature.family.presentation.MemorySettingsRoute
import com.togetherly.feature.family.presentation.OpenSourceLicensesRoute
import com.togetherly.feature.family.presentation.PrivacyRoute
import com.togetherly.feature.family.presentation.QuestPreferencesRoute
import com.togetherly.feature.memory.ui.CompletionMemoryRoute
import com.togetherly.feature.reminder.presentation.ReminderRoute
import com.togetherly.feature.onboarding.presentation.OnboardingRoute
import com.togetherly.feature.packdetails.presentation.PackDetailsRoute
import com.togetherly.feature.paywall.model.PaywallContext
import com.togetherly.feature.paywall.presentation.FamilyPlusPaywallRoute
import com.togetherly.feature.questdetail.model.QuestOpenSource
import com.togetherly.feature.questdetail.presentation.QuestDetailRoute
import com.togetherly.feature.questmode.presentation.QuestModeRoute
import com.togetherly.feature.saved.presentation.SavedRoute
import com.togetherly.navigation.destination.MainDestination
import com.togetherly.navigation.destination.RootDestination
import com.togetherly.navigation.destination.completionIdNavType
import com.togetherly.navigation.destination.questIdNavType
import com.togetherly.navigation.destination.questPackIdNavType
import com.togetherly.navigation.shell.MainShell
import com.togetherly.navigation.shell.RequestedTabStore
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.typeOf

/**
 * The app's single shared `NavHost`, and the only place a [NavHostController] for the root graph
 * is created — every navigation call site below reaches it through this composable's own local
 * `navController`, never by constructing or resolving one elsewhere (see this package's own
 * boundary rule: [NavHostController] stays at the navigation boundary).
 *
 * Back-stack shape, by design:
 * - [RootDestination.Bootstrap] is replaced (`popUpTo(Bootstrap) { inclusive = true }`), never
 *   pushed onto — there is no route back to it once a decision is made.
 * - [RootDestination.Onboarding] is a single destination hosting the whole onboarding state
 *   machine ([OnboardingRoute]/[com.togetherly.feature.onboarding.presentation.OnboardingScreen])
 *   — there is deliberately no per-step nested graph here (see
 *   [com.togetherly.feature.onboarding.model.OnboardingStep]'s own KDoc for why). Completing it
 *   replaces the destination (`popUpTo(Onboarding) { inclusive = true }`), so
 *   [OnboardingRoute.onFamilyCreated] firing — which only ever happens after
 *   [com.togetherly.domain.family.usecase.CreateFamilyProfile] has actually committed the write
 *   (see [OnboardingViewModel][com.togetherly.feature.onboarding.presentation.OnboardingViewModel]'s
 *   own KDoc) — leaves no trace of onboarding in the back stack; Back from
 *   [RootDestination.Main] cannot return to it.
 * - [OnboardingRoute.onNavigateBack] (fired when the user backs out of onboarding's first
 *   internal step, [com.togetherly.feature.onboarding.model.OnboardingStep.WELCOME]) simply pops
 *   the back stack here. Since [RootDestination.Bootstrap] was already removed, that leaves
 *   nothing — the platform's own exit/dismiss behavior takes over, no extra handling needed.
 *
 * [pendingGatedNavigation] (Step 11.8) is the one parental-purchase-gate seam: Today's reroll
 * limit and a locked premium quest are both child-facing triggers, so their `FamilyPlusPaywall`
 * navigation is deferred behind [TogetherlyParentalGateDialog] rather than firing immediately —
 * confirming it runs the deferred navigation exactly once and clears this state; dismissing just
 * clears it, navigating nowhere. [MainShell.onOpenFamilyPlusPaywall] (reached only from the
 * parent-facing Family Plus management screen) navigates directly with no gate, per this
 * feature's own task spec ("a Family Plus screen opened directly from parent-facing settings may
 * follow existing parent-area rules").
 */
@Composable
fun TogetherlyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var pendingGatedNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestedTabStore = koinInject<RequestedTabStore>()

    NavHost(
        navController = navController,
        startDestination = RootDestination.Bootstrap,
        modifier = modifier,
    ) {
        composable<RootDestination.Bootstrap> {
            BootstrapScreen(
                onRequiresOnboarding = {
                    navController.navigate(RootDestination.Onboarding) {
                        popUpTo(RootDestination.Bootstrap) { inclusive = true }
                    }
                },
                onReady = {
                    navController.navigate(RootDestination.Main) {
                        popUpTo(RootDestination.Bootstrap) { inclusive = true }
                    }
                },
            )
        }

        composable<RootDestination.Onboarding> {
            OnboardingRoute(
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onFamilyCreated = {
                    navController.navigate(RootDestination.Main) {
                        popUpTo(RootDestination.Onboarding) { inclusive = true }
                    }
                },
            )
        }

        composable<RootDestination.Main> {
            MainShell(
                onOpenQuestDetail = { questId ->
                    navController.navigate(RootDestination.QuestDetail(questId))
                },
                onOpenQuestDetailFromExplore = { questId ->
                    navController.navigate(RootDestination.QuestDetail(questId, source = QuestOpenSource.EXPLORE))
                },
                onOpenPackDetails = { packId ->
                    navController.navigate(RootDestination.PackDetails(packId))
                },
                onOpenExploreFilters = {
                    navController.navigate(RootDestination.ExploreFilters)
                },
                onOpenSaved = {
                    navController.navigate(RootDestination.Saved)
                },
                onRerollLimitReached = {
                    pendingGatedNavigation = {
                        navController.navigate(RootDestination.FamilyPlusPaywall(context = PaywallContext.PREMIUM_REROLL))
                    }
                },
                onOpenFamilyPlusPaywall = {
                    navController.navigate(RootDestination.FamilyPlusPaywall(context = PaywallContext.FAMILY_PLUS_MANAGEMENT))
                },
                onOpenCustomerCenter = {
                    navController.navigate(RootDestination.CustomerCenter)
                },
                onOpenFamilyProfileEditor = {
                    navController.navigate(RootDestination.FamilyProfileEditor)
                },
                onOpenQuestPreferences = {
                    navController.navigate(RootDestination.QuestPreferences)
                },
                onOpenReminder = {
                    navController.navigate(RootDestination.Reminder)
                },
                onOpenMemorySettings = {
                    navController.navigate(RootDestination.MemorySettings)
                },
                onOpenPrivacy = {
                    navController.navigate(RootDestination.Privacy)
                },
                onOpenLegal = {
                    navController.navigate(RootDestination.Legal)
                },
                onOpenAbout = {
                    navController.navigate(RootDestination.About)
                },
                onOpenDataManagement = {
                    navController.navigate(RootDestination.DataManagement)
                },
            )
        }

        composable<RootDestination.FamilyProfileEditor> {
            FamilyProfileEditorRoute(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<RootDestination.QuestPreferences> {
            QuestPreferencesRoute(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<RootDestination.Reminder> {
            ReminderRoute(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<RootDestination.MemorySettings> {
            MemorySettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onOpenManageMemories = {
                    requestedTabStore.request(MainDestination.Journey)
                    navController.popBackStack(route = RootDestination.Main, inclusive = false)
                },
            )
        }

        composable<RootDestination.Privacy> {
            PrivacyRoute(onNavigateBack = { navController.popBackStack() })
        }

        composable<RootDestination.Legal> {
            LegalRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenOpenSourceLicenses = { navController.navigate(RootDestination.OpenSourceLicenses) },
            )
        }

        composable<RootDestination.OpenSourceLicenses> {
            OpenSourceLicensesRoute(onNavigateBack = { navController.popBackStack() })
        }

        composable<RootDestination.About> {
            AboutRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenDebugTelemetry = { navController.navigate(RootDestination.DebugTelemetry) },
            )
        }

        composable<RootDestination.DebugTelemetry> {
            DebugTelemetryRoute(onNavigateBack = { navController.popBackStack() })
        }

        composable<RootDestination.DataManagement> {
            DataManagementRoute(
                onNavigateBack = { navController.popBackStack() },
                onLocalDataDeleted = {
                    navController.navigate(RootDestination.Onboarding) {
                        popUpTo(RootDestination.Main) { inclusive = true }
                    }
                },
            )
        }

        composable<RootDestination.FamilyPlusPaywall> { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.FamilyPlusPaywall>()
            FamilyPlusPaywallRoute(
                context = destination.context,
                onClose = { navController.popBackStack() },
            )
        }

        composable<RootDestination.CustomerCenter> {
            CustomerCenter(onDismiss = { navController.popBackStack() })
        }

        composable<RootDestination.QuestDetail>(
            typeMap = mapOf(typeOf<QuestId>() to questIdNavType),
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.QuestDetail>()
            QuestDetailRoute(
                questId = destination.questId,
                source = destination.source,
                onNavigateBack = { navController.popBackStack() },
                onStartedQuestMode = { completionId ->
                    navController.navigate(RootDestination.QuestMode(completionId))
                },
                onOpenPaywall = { questId ->
                    pendingGatedNavigation = {
                        navController.navigate(
                            RootDestination.FamilyPlusPaywall(context = PaywallContext.PREMIUM_QUEST, questId = questId.value),
                        )
                    }
                },
            )
        }

        composable<RootDestination.PackDetails>(
            typeMap = mapOf(typeOf<QuestPackId>() to questPackIdNavType),
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.PackDetails>()
            PackDetailsRoute(
                packId = destination.packId,
                onNavigateBack = { navController.popBackStack() },
                onOpenQuestDetail = { questId ->
                    navController.navigate(RootDestination.QuestDetail(questId, source = QuestOpenSource.PACK))
                },
                onOpenPaywall = { packId ->
                    pendingGatedNavigation = {
                        navController.navigate(
                            RootDestination.FamilyPlusPaywall(context = PaywallContext.PREMIUM_PACK, packId = packId.value),
                        )
                    }
                },
            )
        }

        composable<RootDestination.ExploreFilters> {
            ExploreFiltersRoute(onNavigateBack = { navController.popBackStack() })
        }

        composable<RootDestination.Saved> {
            SavedRoute(
                onOpenQuestDetail = { questId ->
                    navController.navigate(RootDestination.QuestDetail(questId, source = QuestOpenSource.SAVED))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<RootDestination.QuestMode>(
            typeMap = mapOf(typeOf<CompletionId>() to completionIdNavType),
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.QuestMode>()
            QuestModeRoute(
                completionId = destination.completionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToToday = { navController.popBackStack(route = RootDestination.Main, inclusive = false) },
                onNavigateToCompletion = { completionId ->
                    // QuestDetail and QuestMode are both popped off here (down to Main) so Back
                    // from the celebration can never reopen the now-completed session — see
                    // RootDestination.CompletionCelebration's own KDoc.
                    navController.navigate(RootDestination.CompletionCelebration(completionId)) {
                        popUpTo(RootDestination.Main) { inclusive = false }
                    }
                },
                onTimerFinished = {},
            )
        }

        composable<RootDestination.CompletionCelebration>(
            typeMap = mapOf(typeOf<CompletionId>() to completionIdNavType),
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.CompletionCelebration>()
            CompletionCelebrationRoute(
                completionId = destination.completionId,
                onNavigateToToday = { navController.popBackStack(route = RootDestination.Main, inclusive = false) },
                onAddMemory = { completionId ->
                    navController.navigate(RootDestination.CompletionMemory(completionId)) {
                        popUpTo(RootDestination.Main) { inclusive = false }
                    }
                },
            )
        }

        composable<RootDestination.CompletionMemory>(
            typeMap = mapOf(typeOf<CompletionId>() to completionIdNavType),
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<RootDestination.CompletionMemory>()
            CompletionMemoryRoute(
                completionId = destination.completionId,
                onNavigateBack = { navController.popBackStack() },
                onMemorySaved = { navController.popBackStack(route = RootDestination.Main, inclusive = false) },
                onMemorySkipped = { navController.popBackStack(route = RootDestination.Main, inclusive = false) },
            )
        }
    }

    val gatedNavigation = pendingGatedNavigation
    if (gatedNavigation != null) {
        TogetherlyParentalGateDialog(
            onConfirmed = {
                pendingGatedNavigation = null
                gatedNavigation()
            },
            onDismiss = { pendingGatedNavigation = null },
        )
    }
}
