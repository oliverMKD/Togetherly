package com.togetherly.content.mapper

import com.togetherly.content.model.QuestPackDto
import com.togetherly.domain.quest.ArtworkKey
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle

internal interface QuestPackMapper {
    fun map(
        dto: QuestPackDto,
        path: String,
    ): ContentMappingResult<QuestPack>
}

/**
 * A null [QuestPackDto.category] maps to a mixed-category pack (`category = null`) — not an
 * error. Performs no pack-to-quest membership validation; that belongs to full catalogue
 * validation later.
 */
internal class DefaultQuestPackMapper : QuestPackMapper {

    override fun map(dto: QuestPackDto, path: String): ContentMappingResult<QuestPack> {
        val category = dto.category?.let { raw ->
            mapQuestCategory(raw, "$path.category").getOrElse { return ContentMappingResult.Failure(it) }
        }
        val access = mapQuestAccess(dto.access, "$path.access")
            .getOrElse { return ContentMappingResult.Failure(it) }

        val questIds = mutableListOf<QuestId>()
        for ((index, raw) in dto.questIds.withIndex()) {
            questIds += mapCatching("$path.questIds[$index]", raw) { QuestId(raw) }
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        return mapCatching(path, dto.id) {
            QuestPack(
                id = QuestPackId(dto.id),
                title = QuestTitle(dto.title),
                description = QuestSummary(dto.description),
                category = category,
                access = access,
                questIds = questIds,
                artworkKey = ArtworkKey(dto.artworkKey),
                sortOrder = dto.sortOrder,
            )
        }
    }
}
