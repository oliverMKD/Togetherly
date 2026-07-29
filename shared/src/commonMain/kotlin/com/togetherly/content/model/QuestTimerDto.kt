package com.togetherly.content.model

import kotlinx.serialization.Serializable

/**
 * Seconds, not a serialized [kotlin.time.Duration] — the future mapper converts this into a
 * domain duration; the content boundary never carries that domain type directly.
 */
@Serializable
internal data class QuestTimerDto(
    val durationSeconds: Int,
    val keepScreenOn: Boolean = false,
)
