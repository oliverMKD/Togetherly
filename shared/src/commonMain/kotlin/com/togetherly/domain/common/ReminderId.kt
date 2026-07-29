package com.togetherly.domain.common

import com.togetherly.domain.validation.requireValidDomainId
import kotlin.jvm.JvmInline

@JvmInline
value class ReminderId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
