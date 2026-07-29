package com.togetherly.data.media

import com.togetherly.domain.completion.MediaReference
import kotlin.time.Duration

data class CommittedVoice(
    val reference: MediaReference,
    val duration: Duration,
    val sizeBytes: Long,
)
