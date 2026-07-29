package com.togetherly.content.mapper

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * Explicit, case-sensitive lookups — never `enumValueOf()`. The catalogue schema's raw strings
 * are an independent contract from Kotlin enum names, and unexpected casing is a schema
 * violation to report, not something to silently normalize.
 */
internal fun mapQuestCategory(raw: String, path: String): ContentMappingResult<QuestCategory> =
    when (raw) {
        "talk" -> ContentMappingResult.Success(QuestCategory.TALK)
        "create" -> ContentMappingResult.Success(QuestCategory.CREATE)
        "move" -> ContentMappingResult.Success(QuestCategory.MOVE)
        "kindness" -> ContentMappingResult.Success(QuestCategory.KINDNESS)
        "discover" -> ContentMappingResult.Success(QuestCategory.DISCOVER)
        "silly" -> ContentMappingResult.Success(QuestCategory.SILLY)
        "memories" -> ContentMappingResult.Success(QuestCategory.MEMORIES)
        else -> ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, raw))
    }

internal fun mapAgeBand(raw: String, path: String): ContentMappingResult<AgeBand> =
    when (raw) {
        "6-8" -> ContentMappingResult.Success(AgeBand.AGE_6_TO_8)
        "9-11" -> ContentMappingResult.Success(AgeBand.AGE_9_TO_11)
        "12-13" -> ContentMappingResult.Success(AgeBand.AGE_12_TO_13)
        else -> ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, raw))
    }

internal fun mapQuestLocation(raw: String, path: String): ContentMappingResult<QuestLocation> =
    when (raw) {
        "indoor" -> ContentMappingResult.Success(QuestLocation.INDOOR)
        "outdoor" -> ContentMappingResult.Success(QuestLocation.OUTDOOR)
        "either" -> ContentMappingResult.Success(QuestLocation.EITHER)
        else -> ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, raw))
    }

internal fun mapPreparationLevel(raw: String, path: String): ContentMappingResult<PreparationLevel> =
    when (raw) {
        "none" -> ContentMappingResult.Success(PreparationLevel.NONE)
        "simple-materials" -> ContentMappingResult.Success(PreparationLevel.SIMPLE_MATERIALS)
        "advanced" -> ContentMappingResult.Success(PreparationLevel.ADVANCED)
        else -> ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, raw))
    }

internal fun mapEnergyLevel(raw: String, path: String): ContentMappingResult<EnergyLevel> =
    when (raw) {
        "calm" -> ContentMappingResult.Success(EnergyLevel.CALM)
        "moderate" -> ContentMappingResult.Success(EnergyLevel.MODERATE)
        "active" -> ContentMappingResult.Success(EnergyLevel.ACTIVE)
        else -> ContentMappingResult.Failure(ContentMappingIssue(path, ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, raw))
    }
