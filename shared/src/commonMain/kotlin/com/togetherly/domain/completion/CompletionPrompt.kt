package com.togetherly.domain.completion

import com.togetherly.domain.validation.requireValidDomainText
import kotlin.jvm.JvmInline

@JvmInline
value class CompletionPrompt(val value: String) {
    init {
        requireValidDomainText(value, MAX_LENGTH)
    }

    companion object {
        const val MAX_LENGTH = 240
    }
}
