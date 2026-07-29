package com.togetherly.feature.family.presentation

import com.togetherly.core.ui.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries a one-shot success message from [FamilyProfileEditorViewModel] back to [FamilyViewModel]
 * across a navigation pop — a Koin singleton (`single { FamilySaveMessageStore() }`), same pattern
 * as [com.togetherly.feature.explore.presentation.ExploreFilterStore] for bridging two screens that
 * only ever talk through a root-level nav push/pop rather than nav args or a `SavedStateHandle`
 * result (see that store's own KDoc for why: [com.togetherly.navigation.destination.RootDestination]
 * isn't polymorphically serializable).
 *
 * [FamilyViewModel] starts collecting [message] in its own `onScreenStarted()` before the editor is
 * ever reachable (the editor only opens from the Family tab), so the publish→collect race that would
 * matter for a cold flow doesn't apply here; [consume] still exists so the message is shown exactly
 * once rather than replaying on every later collection (e.g. process death + state restoration).
 */
class FamilySaveMessageStore {
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    fun publish(message: UiText) {
        _message.value = message
    }

    fun consume() {
        _message.value = null
    }
}
