package com.togetherly.domain.completion

import com.togetherly.domain.validation.requirePositive
import kotlin.time.Duration

data class PendingVoiceReference(
    val reference: PendingMediaReference,
    val duration: Duration,
) {
    init {
        requirePositive(duration)
    }
}
