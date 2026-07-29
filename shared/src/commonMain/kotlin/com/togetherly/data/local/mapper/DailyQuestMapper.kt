package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.data.local.daily.DailyQuestEntity
import com.togetherly.data.local.keys.toDailyQuestSource
import com.togetherly.data.local.keys.toDurationBand
import com.togetherly.data.local.keys.toEnergyLevel
import com.togetherly.data.local.keys.toPreparationLevel
import com.togetherly.data.local.keys.toQuestCategory
import com.togetherly.data.local.keys.toQuestLocation
import com.togetherly.data.local.keys.toStorageKey
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.quest.QuestId
import kotlin.time.Instant

internal class DailyQuestMapper : EntityMapper<DailyQuestEntity, DailyQuest> {

    override fun toEntity(domain: DailyQuest): DataResult<DailyQuestEntity> = DataResult.Success(
        DailyQuestEntity(
            localDate = domain.localDate.toString(),
            questId = domain.questId.value,
            selectionIndex = domain.selectionIndex,
            selectedAtEpochMillis = domain.selectedAt.toEpochMilliseconds(),
            source = domain.source.toStorageKey(),
            contextDuration = domain.context.duration?.toStorageKey(),
            contextLocation = domain.context.location?.toStorageKey(),
            contextEnergy = domain.context.energy?.toStorageKey(),
            contextPreparation = domain.context.preparation?.toStorageKey(),
            contextCategory = domain.context.preferredCategory?.toStorageKey(),
        ),
    )

    override fun toDomain(entity: DailyQuestEntity): DataResult<DailyQuest> {
        val localDate = parseStoredLocalDate(entity.localDate).getOrElse { return DataResult.Error(it) }
        val source = entity.source.toDailyQuestSource().getOrElse { return DataResult.Error(it) }
        val duration = entity.contextDuration?.toDurationBand()?.getOrElse { return DataResult.Error(it) }
        val location = entity.contextLocation?.toQuestLocation()?.getOrElse { return DataResult.Error(it) }
        val energy = entity.contextEnergy?.toEnergyLevel()?.getOrElse { return DataResult.Error(it) }
        val preparation = entity.contextPreparation?.toPreparationLevel()?.getOrElse { return DataResult.Error(it) }
        val category = entity.contextCategory?.toQuestCategory()?.getOrElse { return DataResult.Error(it) }

        return mapStorageCatching {
            DailyQuest(
                questId = QuestId(entity.questId),
                localDate = localDate,
                selectionIndex = entity.selectionIndex,
                selectedAt = Instant.fromEpochMilliseconds(entity.selectedAtEpochMillis),
                source = source,
                context = QuestContext(
                    duration = duration,
                    location = location,
                    energy = energy,
                    preparation = preparation,
                    preferredCategory = category,
                ),
            )
        }
    }
}
