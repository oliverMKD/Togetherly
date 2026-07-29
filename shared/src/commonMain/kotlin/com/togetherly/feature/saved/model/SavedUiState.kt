package com.togetherly.feature.saved.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Reuses [ExploreQuestUiModel]/the quest card it renders with rather than a Saved-specific
 * duplicate — a saved quest shows exactly the same title/summary/duration/energy/location/
 * saved-toggle/premium-badge/locked-state Explore's own cards do (this feature's own task spec:
 * "Completed and premium indicators remain visible"). [ExploreQuestUiModel.isSaved] is always
 * `true` for every item here by construction — this screen only ever lists what's saved.
 */
@Immutable
data class SavedUiState(
    val isLoading: Boolean,
    val quests: PersistentList<ExploreQuestUiModel>,
    val error: UiText?,
) {
    companion object {
        fun initial(): SavedUiState = SavedUiState(isLoading = true, quests = persistentListOf(), error = null)
    }
}
