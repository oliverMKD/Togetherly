package com.togetherly.domain.daily

import com.togetherly.domain.quest.QuestId
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class DismissedQuest(
    val questId: QuestId,
    val dismissedAt: Instant,
    val localDate: LocalDate,
)
