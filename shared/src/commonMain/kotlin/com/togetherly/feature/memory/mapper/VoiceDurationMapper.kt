package com.togetherly.feature.memory.mapper

import kotlin.time.Duration

/** Plain manual `m:ss` formatting — matches this codebase's existing no-platform-date-API convention. */
internal fun Duration.toVoiceDurationLabel(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
