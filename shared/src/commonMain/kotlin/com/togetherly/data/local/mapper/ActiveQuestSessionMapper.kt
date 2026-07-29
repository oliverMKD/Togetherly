package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.ActiveQuestSessionEntity
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

internal class ActiveQuestSessionMapper : EntityMapper<ActiveQuestSessionEntity, ActiveQuestSession> {

    override fun toEntity(domain: ActiveQuestSession): DataResult<ActiveQuestSessionEntity> = DataResult.Success(
        ActiveQuestSessionEntity(
            slotId = ACTIVE_QUEST_SESSION_SLOT_ID,
            completionId = domain.completionId.value,
            familyId = domain.familyId.value,
            questId = domain.questId.value,
            questVersion = domain.questVersion,
            startedAtEpochMillis = domain.startedAt.toEpochMilliseconds(),
        ),
    )

    /** An entity read from any slot other than [ACTIVE_QUEST_SESSION_SLOT_ID] is corrupted storage. */
    override fun toDomain(entity: ActiveQuestSessionEntity): DataResult<ActiveQuestSession> {
        if (entity.slotId != ACTIVE_QUEST_SESSION_SLOT_ID) return corruptedStorage()

        return mapStorageCatching {
            ActiveQuestSession(
                completionId = CompletionId(entity.completionId),
                familyId = FamilyId(entity.familyId),
                questId = QuestId(entity.questId),
                questVersion = entity.questVersion,
                startedAt = Instant.fromEpochMilliseconds(entity.startedAtEpochMillis),
            )
        }
    }
}
