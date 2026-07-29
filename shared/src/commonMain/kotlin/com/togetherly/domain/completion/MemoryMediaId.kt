package com.togetherly.domain.completion

import com.togetherly.domain.validation.requireValidDomainId
import kotlin.jvm.JvmInline

@JvmInline
value class MemoryMediaId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
