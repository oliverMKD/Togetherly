package com.togetherly.content.mapper

import com.togetherly.content.model.QuestCatalogueDto
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestPack

internal interface QuestCatalogueMapper {
    fun map(
        dto: QuestCatalogueDto,
    ): ContentMappingResult<QuestCatalogue>
}

/**
 * Returns the first mapping failure encountered — collecting every issue in the catalogue
 * before mapping is the full validator's job in a later step, not this one.
 */
internal class DefaultQuestCatalogueMapper(
    private val questMapper: FamilyQuestMapper,
    private val packMapper: QuestPackMapper,
) : QuestCatalogueMapper {

    override fun map(dto: QuestCatalogueDto): ContentMappingResult<QuestCatalogue> {
        val packs = mutableListOf<QuestPack>()
        for ((index, packDto) in dto.packs.withIndex()) {
            packs += packMapper.map(packDto, "packs[$index]")
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        val quests = mutableListOf<FamilyQuest>()
        for ((index, questDto) in dto.quests.withIndex()) {
            quests += questMapper.map(questDto, "quests[$index]")
                .getOrElse { return ContentMappingResult.Failure(it) }
        }

        return ContentMappingResult.Success(
            QuestCatalogue(
                schemaVersion = dto.schemaVersion,
                catalogueVersion = dto.catalogueVersion,
                locale = dto.locale,
                packs = packs,
                quests = quests,
            ),
        )
    }
}
