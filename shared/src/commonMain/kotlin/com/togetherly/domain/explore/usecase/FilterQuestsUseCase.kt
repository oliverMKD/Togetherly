package com.togetherly.domain.explore.usecase

import com.togetherly.domain.explore.ExploreFilters
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.recommendation.durationBandFor

/**
 * Pure and synchronous, same reasoning as [SearchQuestsUseCase]. Every configured filter group
 * (category/duration/energy/location/age/access) combines with every other group using AND — a
 * quest must satisfy all of them to appear. Each group in [ExploreFilters] is single-valued by
 * design (one category, one duration band, and so on — see [ExploreFilters]'s own KDoc for why no
 * multi-select "OR within a group" model exists), so there is no separate within-group combination
 * rule to choose: a group is either unset (matches everything) or set to exactly one value a quest
 * must equal. Filtering never removes premium content from the underlying list on its own —
 * [QuestAccessFilter.FREE]/[QuestAccessFilter.PREMIUM] filter by the quest's own declared
 * [FamilyQuest.access] *type*, never by whether the current family can actually unlock it; whether
 * locked premium content is presented as locked is [EvaluateQuestAccessUseCase]'s job, applied
 * separately, after filtering.
 *
 * Order-preserving and free of any randomness or non-deterministic collection iteration — the same
 * `quests`/`filters` input always produces the same output list in the same order.
 */
class FilterQuestsUseCase {
    operator fun invoke(quests: List<FamilyQuest>, filters: ExploreFilters): List<FamilyQuest> = quests.filter { quest ->
        matchesCategory(quest, filters) &&
            matchesDuration(quest, filters) &&
            matchesEnergy(quest, filters) &&
            matchesLocation(quest, filters) &&
            matchesAgeBand(quest, filters) &&
            matchesAccess(quest, filters)
    }

    private fun matchesCategory(quest: FamilyQuest, filters: ExploreFilters): Boolean =
        filters.category == null || quest.category == filters.category

    private fun matchesDuration(quest: FamilyQuest, filters: ExploreFilters): Boolean =
        filters.duration == null || durationBandFor(quest.durationMinutes) == filters.duration

    private fun matchesEnergy(quest: FamilyQuest, filters: ExploreFilters): Boolean =
        filters.energy == null || quest.energy == filters.energy

    private fun matchesLocation(quest: FamilyQuest, filters: ExploreFilters): Boolean =
        filters.location == null || quest.location == filters.location

    private fun matchesAgeBand(quest: FamilyQuest, filters: ExploreFilters): Boolean =
        filters.ageBand == null || filters.ageBand in quest.ageBands

    private fun matchesAccess(quest: FamilyQuest, filters: ExploreFilters): Boolean = when (filters.access) {
        QuestAccessFilter.ALL -> true
        QuestAccessFilter.FREE -> quest.access is QuestAccess.Free
        QuestAccessFilter.PREMIUM -> quest.access is QuestAccess.Premium
    }
}
