package com.togetherly.domain.explore.usecase

import com.togetherly.domain.quest.FamilyQuest

/**
 * Pure and synchronous — no I/O, no coroutine, entirely local — so a ViewModel can call this from
 * a debounced search-text flow without it ever being "network search" in disguise.
 *
 * Matches [FamilyQuest.title]/[FamilyQuest.summary] and, standing in for "tags or category labels"
 * (this catalogue has no separate tag list — see [FamilyQuest]'s own field list), the quest's
 * [FamilyQuest.category] **enum name** (e.g. typing "discover" matches every [com.togetherly.domain.quest.QuestCategory.DISCOVER]
 * quest). This is a deliberate, documented substitution: the actual user-facing category label is
 * a localized [com.togetherly.core.ui.UiText.Resource], resolvable only inside a `@Composable`
 * (see e.g. [com.togetherly.feature.today.mapper.label]) — a pure domain-layer search function has
 * no `@Composable` context to resolve one in, and reaching for one here would put presentation
 * concerns inside the domain layer. If Explore later wants real localized-label search, that
 * mapping has to happen above this use case, not inside it.
 *
 * [String.lowercase] (no [java.util.Locale] argument) is Kotlin's locale-*independent* case
 * folding — the locale-safe choice for a search match that must behave identically regardless of
 * the device's configured locale.
 */
class SearchQuestsUseCase {
    operator fun invoke(quests: List<FamilyQuest>, query: String): List<FamilyQuest> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return quests

        val needle = trimmed.lowercase()
        return quests.filter { quest ->
            quest.title.value.lowercase().contains(needle) ||
                quest.summary.value.lowercase().contains(needle) ||
                quest.category.name.lowercase().contains(needle)
        }
    }
}
