package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.data.local.daily.DismissedQuestEntity
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

internal class DismissedQuestMapper : EntityMapper<DismissedQuestEntity, DismissedQuest> {

    override fun toEntity(domain: DismissedQuest): DataResult<DismissedQuestEntity> = DataResult.Success(
        DismissedQuestEntity(
            questId = domain.questId.value,
            dismissedAtEpochMillis = domain.dismissedAt.toEpochMilliseconds(),
            localDate = domain.localDate.toString(),
        ),
    )

    override fun toDomain(entity: DismissedQuestEntity): DataResult<DismissedQuest> {
        val localDate = parseStoredLocalDate(entity.localDate).getOrElse { return DataResult.Error(it) }

        return mapStorageCatching {
            DismissedQuest(
                questId = QuestId(entity.questId),
                dismissedAt = Instant.fromEpochMilliseconds(entity.dismissedAtEpochMillis),
                localDate = localDate,
            )
        }
    }
}
