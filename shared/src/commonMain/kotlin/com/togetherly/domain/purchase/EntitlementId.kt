package com.togetherly.domain.purchase

import com.togetherly.domain.validation.requireValidDomainId
import kotlin.jvm.JvmInline

@JvmInline
value class EntitlementId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
