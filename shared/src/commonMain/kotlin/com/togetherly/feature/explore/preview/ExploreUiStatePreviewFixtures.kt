package com.togetherly.feature.explore.preview

import com.togetherly.core.ui.UiText
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.model.ExploreEmptyState
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.explore.model.ExploreUiState
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_duration_ten_minutes
import togetherly.shared.generated.resources.today_duration_twenty_minutes
import togetherly.shared.generated.resources.today_energy_active
import togetherly.shared.generated.resources.today_location_outdoor

/**
 * A living catalogue of Explore's UI states, for [com.togetherly.feature.explore.presentation.ExploreScreenPreviews]
 * — never a substitute for [com.togetherly.feature.explore.presentation.ExploreViewModelTest], which
 * proves behavior; these only prove appearance. Every fixture is fictional sample data, matching
 * this project's privacy audit requirement that previews/screenshots never carry real data.
 */

fun sampleExploreQuest(
    id: String = "preview-quest",
    title: String = "Backyard Scavenger Hunt",
    isSaved: Boolean = false,
    isPremium: Boolean = false,
    locked: Boolean = false,
) = ExploreQuestUiModel(
    id = QuestId(id),
    title = title,
    summary = "Find five hidden treasures together before the timer runs out.",
    category = QuestCategoryUi.DISCOVER,
    durationLabel = UiText.Resource(Res.string.today_duration_twenty_minutes),
    energyLabel = UiText.Resource(Res.string.today_energy_active),
    locationLabel = UiText.Resource(Res.string.today_location_outdoor),
    isSaved = isSaved,
    isPremium = isPremium,
    locked = locked,
)

fun sampleExplorePack(
    id: String = "preview-pack",
    title: String = "Creative Sparks",
    isPremium: Boolean = false,
    locked: Boolean = false,
) = ExplorePackUiModel(
    id = QuestPackId(id),
    title = title,
    description = "Drawing, making and imagination — six quests for hands-on family creativity.",
    questCount = 6,
    durationLabel = UiText.Resource(Res.string.today_duration_ten_minutes),
    isPremium = isPremium,
    locked = locked,
    theme = QuestCategoryUi.CREATE,
)

fun loadingExploreUiState() = ExploreUiState.initial()

fun freeFamilyExploreUiState() = ExploreUiState.initial().copy(
    isLoading = false,
    featuredPacks = persistentListOf(
        sampleExplorePack(id = "quick-wins", title = "Quick Wins"),
        sampleExplorePack(id = "creative-sparks", title = "Creative Sparks", isPremium = true, locked = true),
    ),
    packs = persistentListOf(
        sampleExplorePack(id = "quick-wins", title = "Quick Wins"),
        sampleExplorePack(id = "everyday-together", title = "Everyday Together"),
        sampleExplorePack(id = "creative-sparks", title = "Creative Sparks", isPremium = true, locked = true),
        sampleExplorePack(id = "calm-and-connected", title = "Calm & Connected", isPremium = true, locked = true),
    ),
    quests = persistentListOf(
        sampleExploreQuest(id = "quest-1", title = "Backyard Scavenger Hunt"),
        sampleExploreQuest(id = "quest-2", title = "Draw a Shared Imaginary Creature", isPremium = true, locked = true),
        sampleExploreQuest(id = "quest-3", title = "Three Good Things", isSaved = true),
    ),
    access = FamilyAccess.free(),
)

fun familyPlusExploreUiState() = freeFamilyExploreUiState().let { state ->
    state.copy(
        featuredPacks = state.featuredPacks.map { it.copy(locked = false) }.toPersistentList(),
        packs = state.packs.map { it.copy(locked = false) }.toPersistentList(),
        quests = state.quests.map { it.copy(locked = false) }.toPersistentList(),
        access = FamilyAccess.lifetime(),
    )
}

fun searchResultsExploreUiState() = freeFamilyExploreUiState().copy(
    searchQuery = "draw",
    isSearchActive = true,
    featuredPacks = persistentListOf(),
    packs = persistentListOf(sampleExplorePack(id = "creative-sparks", title = "Creative Sparks", isPremium = true, locked = true)),
    quests = persistentListOf(sampleExploreQuest(id = "quest-2", title = "Draw a Shared Imaginary Creature", isPremium = true, locked = true)),
)

fun emptySearchResultsExploreUiState() = freeFamilyExploreUiState().copy(
    searchQuery = "xyzzy",
    isSearchActive = true,
    featuredPacks = persistentListOf(),
    packs = persistentListOf(),
    quests = persistentListOf(),
    emptyState = ExploreEmptyState.SEARCH,
)

fun emptyCategoryResultsExploreUiState() = freeFamilyExploreUiState().copy(
    selectedCategory = QuestCategoryUi.SILLY,
    packs = persistentListOf(),
    quests = persistentListOf(),
    emptyState = ExploreEmptyState.FILTER,
)

fun errorExploreUiState() = ExploreUiState.initial().copy(
    isLoading = false,
    error = UiText.Dynamic("Something went wrong. Please try again."),
)

fun longTextExploreUiState() = freeFamilyExploreUiState().copy(
    packs = persistentListOf(
        sampleExplorePack(
            id = "long-pack",
            title = "A Very Long Pack Title That Should Truncate Gracefully Instead Of Breaking The Layout",
        ),
    ),
    quests = persistentListOf(
        sampleExploreQuest(
            id = "long-quest",
            title = "A Very Long Quest Title That Should Truncate Gracefully Instead Of Breaking The Card Layout",
        ),
    ),
)

fun lockedPremiumCardsExploreUiState() = freeFamilyExploreUiState().copy(
    featuredPacks = persistentListOf(
        sampleExplorePack(id = "creative-sparks", title = "Creative Sparks", isPremium = true, locked = true),
        sampleExplorePack(id = "calm-and-connected", title = "Calm & Connected", isPremium = true, locked = true),
    ),
    packs = persistentListOf(
        sampleExplorePack(id = "creative-sparks", title = "Creative Sparks", isPremium = true, locked = true),
        sampleExplorePack(id = "calm-and-connected", title = "Calm & Connected", isPremium = true, locked = true),
    ),
    quests = persistentListOf(
        sampleExploreQuest(id = "quest-2", title = "Draw a Shared Imaginary Creature", isPremium = true, locked = true),
    ),
)
