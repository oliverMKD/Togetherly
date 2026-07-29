package com.togetherly.data.local.keys

import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuestSource

internal fun DailyQuestSource.toStorageKey(): String = when (this) {
    DailyQuestSource.AUTOMATIC -> "automatic"
    DailyQuestSource.CONTEXTUAL -> "contextual"
    DailyQuestSource.REROLL -> "reroll"
    DailyQuestSource.EXPLORE -> "explore"
}

internal fun dailyQuestSourceFromStorageKey(key: String): DailyQuestSource? = when (key) {
    "automatic" -> DailyQuestSource.AUTOMATIC
    "contextual" -> DailyQuestSource.CONTEXTUAL
    "reroll" -> DailyQuestSource.REROLL
    "explore" -> DailyQuestSource.EXPLORE
    else -> null
}

internal fun String.toDailyQuestSource(): DataResult<DailyQuestSource> =
    dailyQuestSourceFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()
