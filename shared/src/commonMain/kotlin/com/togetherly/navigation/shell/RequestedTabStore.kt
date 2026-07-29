package com.togetherly.navigation.shell

import com.togetherly.navigation.destination.MainDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Koin singleton (`single { RequestedTabStore() }`), same bridge-two-screens-without-nav-args
 * pattern as [com.togetherly.feature.explore.presentation.ExploreFilterStore]/
 * [com.togetherly.feature.family.presentation.FamilySaveMessageStore] — [MainShell] owns its own
 * nested `NavController` privately, so a destination pushed *above* [RootDestination.Main][com.togetherly.navigation.destination.RootDestination.Main]
 * (e.g. the Memory Settings screen's own "Manage memories" action) has no direct way to select a
 * specific tab once it pops back down to Main. [request] followed by popping back to Main, and
 * [MainShell] switching tabs + calling [consume] the next time it's composed, is that bridge.
 */
class RequestedTabStore {
    private val _requestedTab = MutableStateFlow<MainDestination?>(null)
    val requestedTab: StateFlow<MainDestination?> = _requestedTab.asStateFlow()

    fun request(tab: MainDestination) {
        _requestedTab.value = tab
    }

    fun consume() {
        _requestedTab.value = null
    }
}
