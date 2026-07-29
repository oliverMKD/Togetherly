package com.togetherly.domain.quest

import com.togetherly.domain.validation.requireValidDomainId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** [Serializable] so it can be carried directly as a type-safe navigation argument (e.g. [com.togetherly.navigation.destination.RootDestination.PackDetails]). */
@Serializable
@JvmInline
value class QuestPackId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
