package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.data.local.saved.SavedQuestEntity
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import kotlin.time.Instant

internal class SavedQuestMapper : EntityMapper<SavedQuestEntity, SavedQuest> {

    override fun toEntity(domain: SavedQuest): DataResult<SavedQuestEntity> = DataResult.Success(
        SavedQuestEntity(
            questId = domain.questId.value,
            savedAtEpochMillis = domain.savedAt.toEpochMilliseconds(),
        ),
    )

    override fun toDomain(entity: SavedQuestEntity): DataResult<SavedQuest> = mapStorageCatching {
        SavedQuest(
            questId = QuestId(entity.questId),
            savedAt = Instant.fromEpochMilliseconds(entity.savedAtEpochMillis),
        )
    }
}
