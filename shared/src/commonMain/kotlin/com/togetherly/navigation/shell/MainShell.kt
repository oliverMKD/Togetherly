package com.togetherly.navigation.shell

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyBottomNavigationBar
import com.togetherly.designsystem.component.navigation.TogetherlyDestination
import com.togetherly.designsystem.component.navigation.TogetherlyNavigationItem
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.presentation.ExploreRoute
import com.togetherly.feature.family.presentation.FamilyRoute
import com.togetherly.feature.journey.ui.JourneyRoute
import com.togetherly.feature.today.presentation.TodayRoute
import com.togetherly.navigation.destination.MainDestination
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.nav_destination_explore
import togetherly.shared.generated.resources.nav_destination_family
import togetherly.shared.generated.resources.nav_destination_journey
import togetherly.shared.generated.resources.nav_destination_today

/**
 * A [MainTab] pairs a type-safe [MainDestination] route with the design system's own,
 * navigation-agnostic [TogetherlyDestination] identity — this list is the one place that mapping
 * happens, exactly as [TogetherlyDestination]'s own KDoc anticipates ("the navigation shell will
 * map destinations to component inputs later").
 */
private data class MainTab(
    val destination: MainDestination,
    val dsDestination: TogetherlyDestination,
    val label: StringResource,
    val monogram: String,
)

private val MAIN_TABS = listOf(
    MainTab(MainDestination.Today, TogetherlyDestination.Today, Res.string.nav_destination_today, "T"),
    MainTab(MainDestination.Explore, TogetherlyDestination.Explore, Res.string.nav_destination_explore, "E"),
    MainTab(MainDestination.Journey, TogetherlyDestination.Journey, Res.string.nav_destination_journey, "J"),
    MainTab(MainDestination.Family, TogetherlyDestination.Family, Res.string.nav_destination_family, "F"),
)

/**
 * The four-tab shell behind [com.togetherly.navigation.destination.RootDestination.Main]. Owns its
 * own nested `NavController`/`NavHost` for the four tabs — a screen inside one tab never reaches
 * outside this shell to navigate to another top-level root destination; that always goes through
 * whatever this shell was given at its own boundary (nothing yet, since a cross-root navigation
 * need — e.g. a future full-screen Quest Mode — doesn't exist at this step).
 *
 * Each tab keeps its own back-stack state across switches (`saveState`/`restoreState` below, the
 * standard Navigation Compose bottom-nav recipe) rather than resetting to a blank slate every time
 * the user switches tabs. Re-selecting the already-active tab is a no-op — `launchSingleTop`
 * prevents a duplicate entry — rather than popping to some "root" within the tab, since every tab
 * is a single flat screen today with nothing to pop through; a future nested flow within a tab is
 * exactly where "return to root on reselect" would be added, at this same call site, not a reason
 * to build that machinery now.
 *
 * Bottom navigation exists *only* inside this shell — neither Bootstrap nor Onboarding wrap their
 * content in it.
 *
 * [onOpenQuestDetail] (Step 8.6) is exactly the boundary the class KDoc above anticipates: Today's
 * own [com.togetherly.feature.today.presentation.TodayEvent.OpenQuestDetail] climbs out through
 * this callback to [com.togetherly.navigation.host.TogetherlyNavHost], which pushes
 * [com.togetherly.navigation.destination.RootDestination.QuestDetail] onto the *root* back stack —
 * never a destination inside this shell's own nested tab [NavHost]. [onRerollLimitReached] (Step
 * 11.6) climbs out the same way for [com.togetherly.feature.today.presentation.TodayEvent.RerollLimitReached],
 * reaching [com.togetherly.navigation.destination.RootDestination.FamilyPlusPaywall].
 * [onOpenFamilyPlusPaywall]/[onOpenCustomerCenter] (Step 11.7) do the same for the Family tab's
 * embedded [com.togetherly.feature.familyplus.presentation.FamilyPlusStatusSection] — the tab itself
 * lives inside this shell's nested [NavHost] (it's one of the four tabs), but both its outward
 * actions still climb out to the root graph, same as every other cross-root navigation here.
 * [onOpenFamilyProfileEditor] (Step 13.2) does the same for the Family tab root's own
 * [com.togetherly.feature.family.presentation.FamilyRoute].
 * [onOpenQuestDetailFromExplore]/[onOpenPackDetails] (Step 12.1) are the same boundary for the
 * Explore tab's own [com.togetherly.feature.explore.presentation.ExploreRoute]. Explore gets its
 * own `onOpenQuestDetailFromExplore` rather than reusing [onOpenQuestDetail] directly — both reach
 * [com.togetherly.navigation.destination.RootDestination.QuestDetail], but only this one needs to
 * pass [com.togetherly.feature.questdetail.model.QuestOpenSource.EXPLORE], so [TogetherlyNavHost][com.togetherly.navigation.host.TogetherlyNavHost]
 * (the only place either is actually implemented) can tell which tab a click came from without
 * [MainShell] itself branching on it.
 */
@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    onOpenQuestDetail: (QuestId) -> Unit = {},
    onOpenQuestDetailFromExplore: (QuestId) -> Unit = {},
    onOpenPackDetails: (QuestPackId) -> Unit = {},
    onOpenExploreFilters: () -> Unit = {},
    onOpenSaved: () -> Unit = {},
    onRerollLimitReached: () -> Unit = {},
    onOpenFamilyPlusPaywall: () -> Unit = {},
    onOpenCustomerCenter: () -> Unit = {},
    onOpenFamilyProfileEditor: () -> Unit = {},
    onOpenQuestPreferences: () -> Unit = {},
    onOpenReminder: () -> Unit = {},
    onOpenMemorySettings: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLegal: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenDataManagement: () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    // Bridges "Manage memories" (reached from a root-level screen pushed above Main, with no
    // direct handle on this shell's own nested NavController) back to a specific tab — see
    // RequestedTabStore's own KDoc.
    val requestedTabStore = koinInject<RequestedTabStore>()
    val requestedTab by requestedTabStore.requestedTab.collectAsStateWithLifecycle()
    LaunchedEffect(requestedTab) {
        val tab = requestedTab ?: return@LaunchedEffect
        navController.navigate(tab) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        requestedTabStore.consume()
    }

    TogetherlyScreen(
        modifier = modifier,
        scrollable = false,
        bottomBar = {
            TogetherlyBottomNavigationBar(
                items = MAIN_TABS.map { tab ->
                    TogetherlyNavigationItem(
                        destination = tab.dsDestination,
                        label = stringResource(tab.label),
                        selected = currentDestination?.hasRoute(tab.destination::class) == true,
                        icon = { Text(text = tab.monogram, color = LocalContentColor.current) },
                    )
                },
                onItemSelected = { selected ->
                    val tab = MAIN_TABS.first { it.dsDestination == selected }
                    navController.navigate(tab.destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) {
        NavHost(navController = navController, startDestination = MainDestination.Today) {
            composable<MainDestination.Today> {
                TodayRoute(onOpenQuestDetail = onOpenQuestDetail, onRerollLimitReached = onRerollLimitReached)
            }
            composable<MainDestination.Explore> {
                ExploreRoute(
                    onOpenQuestDetail = onOpenQuestDetailFromExplore,
                    onOpenPackDetails = onOpenPackDetails,
                    onOpenFilters = onOpenExploreFilters,
                    onOpenSaved = onOpenSaved,
                )
            }
            composable<MainDestination.Journey> {
                JourneyRoute(
                    onGoToToday = {
                        navController.navigate(MainDestination.Today) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<MainDestination.Family> {
                FamilyRoute(
                    onOpenPaywall = onOpenFamilyPlusPaywall,
                    onOpenCustomerCenter = onOpenCustomerCenter,
                    onOpenProfileEditor = onOpenFamilyProfileEditor,
                    onOpenQuestPreferences = onOpenQuestPreferences,
                    onOpenReminder = onOpenReminder,
                    onOpenMemorySettings = onOpenMemorySettings,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenLegal = onOpenLegal,
                    onOpenAbout = onOpenAbout,
                    onOpenDataManagement = onOpenDataManagement,
                )
            }
        }
    }
}
