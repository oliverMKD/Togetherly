package com.togetherly.domain.completion

import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

internal val STARTED_AT: Instant = Instant.parse("2026-01-01T10:00:00Z")
internal val COMPLETED_AT: Instant = Instant.parse("2026-01-01T10:15:00Z")

internal fun validActiveQuestSession(
    completionId: CompletionId = CompletionId("completion-1"),
    familyId: FamilyId = FamilyId("family-1"),
    questId: QuestId = QuestId("quest-1"),
    questVersion: Int = 1,
    startedAt: Instant = STARTED_AT,
) = ActiveQuestSession(
    completionId = completionId,
    familyId = familyId,
    questId = questId,
    questVersion = questVersion,
    startedAt = startedAt,
)

internal fun validQuestCompletion(
    id: CompletionId = CompletionId("completion-1"),
    familyId: FamilyId = FamilyId("family-1"),
    questId: QuestId = QuestId("quest-1"),
    questVersion: Int = 1,
    startedAt: Instant? = STARTED_AT,
    completedAt: Instant = COMPLETED_AT,
    note: MemoryNote? = null,
    reactions: Set<FamilyReaction> = emptySet(),
    media: List<MemoryMedia> = emptyList(),
) = QuestCompletion(
    id = id,
    familyId = familyId,
    questId = questId,
    questVersion = questVersion,
    startedAt = startedAt,
    completedAt = completedAt,
    note = note,
    reactions = reactions,
    media = media,
)
