package com.togetherly.feature.today.model

/**
 * Mirrors [com.togetherly.domain.quest.QuestCategory] rather than reusing it directly — Today's UI
 * layer (labels, colors, decorative tokens, added in Step 8.4) must never need to import a domain
 * type to render a quest card. See [com.togetherly.feature.today.mapper.toUi] for the mapping.
 */
enum class QuestCategoryUi {
    TALK,
    CREATE,
    MOVE,
    KINDNESS,
    DISCOVER,
    SILLY,
    MEMORIES,
}
