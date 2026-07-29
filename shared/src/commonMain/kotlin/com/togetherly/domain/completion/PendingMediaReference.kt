package com.togetherly.domain.completion

import com.togetherly.domain.validation.requireValidDomainText
import kotlin.jvm.JvmInline

/**
 * Points to an app-private staging file that has not yet been promoted into permanent storage —
 * never an Android `Uri`, an iOS `NSURL`, a public URL, a photo-library identifier, or raw bytes.
 * Like [MediaReference], the data layer owns interpreting this value into an actual storage
 * location, and it must never be logged or sent to analytics as if it were harmless metadata.
 */
@JvmInline
value class PendingMediaReference(val value: String) {
    init {
        requireValidDomainText(value, MAX_LENGTH)
    }

    companion object {
        const val MAX_LENGTH = 256
    }
}
