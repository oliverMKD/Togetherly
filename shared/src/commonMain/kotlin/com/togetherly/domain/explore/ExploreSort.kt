package com.togetherly.domain.explore

/**
 * Deliberately three values, not four: the task's own illustrative example also lists
 * `RecentlyAdded`, but neither [com.togetherly.domain.quest.FamilyQuest] nor
 * [com.togetherly.domain.quest.QuestPack] carries any creation/added timestamp today — only
 * [com.togetherly.domain.quest.FamilyQuest.version], a content-schema version, not a chronology.
 * Sorting by "recently added" would have to fake recency from catalogue/JSON order, which would
 * misrepresent content that was simply *listed* later as though it were *added* later. [RECOMMENDED]
 * and [ALPHABETICAL]/[SHORTEST_FIRST] all have real, honest backing data; a real "recently added"
 * sort is deferred until content actually carries a timestamp worth sorting by.
 *
 * [RECOMMENDED] is the catalogue's own stable, deterministic order (pack [com.togetherly.domain.quest.QuestPack.sortOrder]
 * for packs, [com.togetherly.domain.quest.repository.QuestRepository]'s own already-deterministic
 * order for quests) — the curated order content already ships in, not a computed personalization
 * score; [com.togetherly.domain.recommendation.DeterministicQuestRecommendationPolicy] picks one
 * quest for *today*, it doesn't rank an entire browsable catalogue, so reusing it here would be
 * solving a different problem than what Explore's list actually needs.
 */
enum class ExploreSort {
    RECOMMENDED,
    SHORTEST_FIRST,
    ALPHABETICAL,
}
