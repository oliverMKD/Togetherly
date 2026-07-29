package com.togetherly.domain.completion

import com.togetherly.domain.validation.requireAtMost
import com.togetherly.domain.validation.requirePositive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

sealed interface MemoryMedia {
    val id: MemoryMediaId

    data class Photo(
        override val id: MemoryMediaId,
        val localReference: MediaReference,
    ) : MemoryMedia

    data class Voice(
        override val id: MemoryMediaId,
        val localReference: MediaReference,
        val duration: Duration,
    ) : MemoryMedia {
        init {
            requirePositive(duration)
            requireAtMost(duration, MAX_DURATION)
        }

        companion object {
            val MAX_DURATION: Duration = 5.minutes
        }
    }
}
