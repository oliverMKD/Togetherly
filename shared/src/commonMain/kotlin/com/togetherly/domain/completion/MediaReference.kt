package com.togetherly.domain.completion

import com.togetherly.domain.validation.requireValidDomainText
import kotlin.jvm.JvmInline

/**
 * An app-controlled media identifier or relative reference — never a platform URI, an absolute
 * filesystem path, or a public URL. The data layer owns interpreting this value into an actual
 * storage location. Treat this value as private family memory data: it must never be logged or
 * sent to analytics as if it were harmless metadata.
 */
@JvmInline
value class MediaReference(val value: String) {
    init {
        requireValidDomainText(value, MAX_LENGTH)
    }

    companion object {
        const val MAX_LENGTH = 256
    }
}
