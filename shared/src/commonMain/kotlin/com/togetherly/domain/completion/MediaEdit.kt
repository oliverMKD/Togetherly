package com.togetherly.domain.completion

/**
 * Describes what should happen to a single media slot (photo or voice) on save, distinguishing
 * "leave whatever is already there alone" from "delete it" — a plain nullable pending reference
 * can express "replace with this" or "nothing new was staged", but not "remove what's already
 * committed", so collapsing all three into one nullable field would make removal inexpressible
 * without also discarding an untouched existing memory.
 */
sealed interface MediaEdit<out T> {
    data object Unchanged : MediaEdit<Nothing>
    data object Remove : MediaEdit<Nothing>
    data class Replace<T>(val value: T) : MediaEdit<T>
}
