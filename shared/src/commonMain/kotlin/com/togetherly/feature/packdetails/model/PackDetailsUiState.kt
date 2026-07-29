package com.togetherly.feature.packdetails.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Reuses [ExplorePackUiModel]/[ExploreQuestUiModel] rather than a Pack-Details-specific duplicate —
 * both already carry everything a header/quest-list needs (title, description, quest count,
 * duration range, premium/locked state). [pack] is `null` only while [isLoading] or after [error].
 */
@Immutable
data class PackDetailsUiState(
    val isLoading: Boolean,
    val pack: ExplorePackUiModel?,
    val quests: PersistentList<ExploreQuestUiModel>,
    val accessState: ContentAccessState,
    val error: UiText?,
) {
    companion object {
        fun initial(): PackDetailsUiState = PackDetailsUiState(
            isLoading = true,
            pack = null,
            quests = persistentListOf(),
            accessState = ContentAccessState.FREE,
            error = null,
        )
    }
}
