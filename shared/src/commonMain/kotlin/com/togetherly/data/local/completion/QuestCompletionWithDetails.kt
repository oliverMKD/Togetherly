package com.togetherly.data.local.completion

import androidx.room.Embedded
import androidx.room.Relation

internal data class QuestCompletionWithDetails(
    @Embedded
    val completion: QuestCompletionEntity,

    @Relation(parentColumn = "id", entityColumn = "completionId")
    val reactions: List<CompletionReactionEntity>,

    @Relation(parentColumn = "id", entityColumn = "completionId")
    val media: List<MemoryMediaEntity>,
)
