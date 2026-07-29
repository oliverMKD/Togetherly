package com.togetherly.domain.saved

import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

data class SavedQuest(
    val questId: QuestId,
    val savedAt: Instant,
)
