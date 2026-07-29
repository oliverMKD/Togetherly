package com.togetherly.data.local.keys

import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation

/**
 * A separate, database-only key convention from [com.togetherly.content.mapper.CatalogueEnumMappers]'
 * JSON schema keys (e.g. "simple-materials" there vs. "simple_material" style here) — the two
 * schemas are independent contracts that should be free to evolve separately, even though they
 * happen to describe the same domain enums.
 */
internal fun QuestCategory.toStorageKey(): String = when (this) {
    QuestCategory.TALK -> "talk"
    QuestCategory.CREATE -> "create"
    QuestCategory.MOVE -> "move"
    QuestCategory.KINDNESS -> "kindness"
    QuestCategory.DISCOVER -> "discover"
    QuestCategory.SILLY -> "silly"
    QuestCategory.MEMORIES -> "memories"
}

internal fun questCategoryFromStorageKey(key: String): QuestCategory? = when (key) {
    "talk" -> QuestCategory.TALK
    "create" -> QuestCategory.CREATE
    "move" -> QuestCategory.MOVE
    "kindness" -> QuestCategory.KINDNESS
    "discover" -> QuestCategory.DISCOVER
    "silly" -> QuestCategory.SILLY
    "memories" -> QuestCategory.MEMORIES
    else -> null
}

internal fun String.toQuestCategory(): DataResult<QuestCategory> =
    questCategoryFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun QuestLocation.toStorageKey(): String = when (this) {
    QuestLocation.INDOOR -> "indoor"
    QuestLocation.OUTDOOR -> "outdoor"
    QuestLocation.EITHER -> "either"
}

internal fun questLocationFromStorageKey(key: String): QuestLocation? = when (key) {
    "indoor" -> QuestLocation.INDOOR
    "outdoor" -> QuestLocation.OUTDOOR
    "either" -> QuestLocation.EITHER
    else -> null
}

internal fun String.toQuestLocation(): DataResult<QuestLocation> =
    questLocationFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun EnergyLevel.toStorageKey(): String = when (this) {
    EnergyLevel.CALM -> "calm"
    EnergyLevel.MODERATE -> "moderate"
    EnergyLevel.ACTIVE -> "active"
}

internal fun energyLevelFromStorageKey(key: String): EnergyLevel? = when (key) {
    "calm" -> EnergyLevel.CALM
    "moderate" -> EnergyLevel.MODERATE
    "active" -> EnergyLevel.ACTIVE
    else -> null
}

internal fun String.toEnergyLevel(): DataResult<EnergyLevel> =
    energyLevelFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun PreparationLevel.toStorageKey(): String = when (this) {
    PreparationLevel.NONE -> "none"
    PreparationLevel.SIMPLE_MATERIALS -> "simple_materials"
    PreparationLevel.ADVANCED -> "advanced"
}

internal fun preparationLevelFromStorageKey(key: String): PreparationLevel? = when (key) {
    "none" -> PreparationLevel.NONE
    "simple_materials" -> PreparationLevel.SIMPLE_MATERIALS
    "advanced" -> PreparationLevel.ADVANCED
    else -> null
}

internal fun String.toPreparationLevel(): DataResult<PreparationLevel> =
    preparationLevelFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()
