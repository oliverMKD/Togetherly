package com.togetherly.domain.explore

/**
 * The one genuinely new access-related model this feature needs — [com.togetherly.domain.quest.QuestAccess]
 * itself (a quest/pack's own Free/Premium requirement) has no "either" state, since a piece of
 * content is never ambiguously both. [ALL] is exactly that missing "don't filter by access at
 * all" state for a filter control, which is a presentation/query concern, not a content property.
 */
enum class QuestAccessFilter {
    ALL,
    FREE,
    PREMIUM,
}
