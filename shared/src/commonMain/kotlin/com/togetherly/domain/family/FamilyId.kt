package com.togetherly.domain.family

import com.togetherly.domain.validation.requireValidDomainId
import kotlin.jvm.JvmInline

@JvmInline
value class FamilyId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
