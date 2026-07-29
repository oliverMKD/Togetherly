package com.togetherly.domain.quest

import com.togetherly.domain.validation.requireValidDomainId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** [Serializable] so it can be carried directly as a type-safe navigation argument (e.g. [com.togetherly.navigation.destination.RootDestination.QuestDetail]). */
@Serializable
@JvmInline
value class QuestId(val value: String) {
    init {
        requireValidDomainId(value)
    }
}
