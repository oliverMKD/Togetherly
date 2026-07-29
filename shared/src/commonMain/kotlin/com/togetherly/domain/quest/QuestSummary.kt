package com.togetherly.domain.quest

import com.togetherly.domain.validation.requireValidDomainText
import kotlin.jvm.JvmInline

@JvmInline
value class QuestSummary(val value: String) {
    init {
        requireValidDomainText(value, MAX_LENGTH)
    }

    companion object {
        const val MAX_LENGTH = 240
    }
}
