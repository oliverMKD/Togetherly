package com.togetherly.domain.purchase

import com.togetherly.domain.validation.requireValidDomainId
import kotlin.jvm.JvmInline

@JvmInline
value class ProductId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
