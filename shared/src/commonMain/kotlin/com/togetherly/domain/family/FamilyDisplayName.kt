package com.togetherly.domain.family

import com.togetherly.domain.validation.requireValidDomainText
import kotlin.jvm.JvmInline

@JvmInline
value class FamilyDisplayName(val value: String) {
    init {
        requireValidDomainText(value, MAX_LENGTH)
    }

    companion object {
        const val MAX_LENGTH = 60
    }
}
