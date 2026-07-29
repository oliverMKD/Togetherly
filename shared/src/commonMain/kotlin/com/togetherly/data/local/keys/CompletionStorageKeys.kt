package com.togetherly.data.local.keys

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.FamilyReaction

internal fun FamilyReaction.toStorageKey(): String = when (this) {
    FamilyReaction.HAPPY -> "happy"
    FamilyReaction.SILLY -> "silly"
    FamilyReaction.LOVED_IT -> "loved_it"
    FamilyReaction.CALM -> "calm"
    FamilyReaction.SURPRISED -> "surprised"
}

internal fun familyReactionFromStorageKey(key: String): FamilyReaction? = when (key) {
    "happy" -> FamilyReaction.HAPPY
    "silly" -> FamilyReaction.SILLY
    "loved_it" -> FamilyReaction.LOVED_IT
    "calm" -> FamilyReaction.CALM
    "surprised" -> FamilyReaction.SURPRISED
    else -> null
}

internal fun String.toFamilyReaction(): DataResult<FamilyReaction> =
    familyReactionFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

/**
 * [com.togetherly.domain.completion.MemoryMedia] is a sealed interface, not an enum — there is no
 * single type to run a `when` over, so its two storage keys are plain constants instead.
 */
internal const val MEMORY_MEDIA_TYPE_PHOTO = "photo"
internal const val MEMORY_MEDIA_TYPE_VOICE = "voice"
