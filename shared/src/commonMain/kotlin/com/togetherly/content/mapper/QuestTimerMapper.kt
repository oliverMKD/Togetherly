package com.togetherly.content.mapper

import com.togetherly.content.model.QuestTimerDto
import com.togetherly.domain.quest.QuestTimer
import kotlin.time.Duration.Companion.seconds

/**
 * `null` maps to `null` — an absent timer is not an error. [QuestTimer]'s own invariants
 * (positive duration, reasonable maximum) are the source of truth; this mapper only converts
 * units and reports a failure at [path] if they're violated.
 */
internal fun mapQuestTimer(dto: QuestTimerDto?, path: String): ContentMappingResult<QuestTimer?> {
    if (dto == null) return ContentMappingResult.Success(null)

    return mapCatching(path, dto.durationSeconds.toString()) {
        QuestTimer(duration = dto.durationSeconds.seconds, keepScreenOn = dto.keepScreenOn)
    }
}
