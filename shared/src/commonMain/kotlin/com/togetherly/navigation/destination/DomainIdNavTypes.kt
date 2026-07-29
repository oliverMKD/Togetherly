package com.togetherly.navigation.destination

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId

/**
 * [RootDestination]'s `@Serializable` route arguments use [QuestId]/[CompletionId] — inline value
 * classes wrapping [String] — rather than raw strings, so callers can never mix up which ID a nav
 * argument holds (see each route's own KDoc). Navigation's type-safe routing only infers a
 * [NavType] automatically for the primitives/enums it knows about; value classes aren't one of
 * them, so without these, resolving either route throws `could not find any NavType for argument`.
 * These are passed as the `typeMap` for every `composable<...>` block in
 * [com.togetherly.navigation.host.TogetherlyNavHost] whose route carries the corresponding ID.
 */
internal val questIdNavType: NavType<QuestId> = object : NavType<QuestId>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: QuestId) {
        bundle.write { putString(key, value.value) }
    }

    override fun get(bundle: SavedState, key: String): QuestId =
        QuestId(bundle.read { getString(key) })

    override fun parseValue(value: String): QuestId = QuestId(value)

    override fun serializeAsValue(value: QuestId): String = value.value
}

internal val completionIdNavType: NavType<CompletionId> = object : NavType<CompletionId>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: CompletionId) {
        bundle.write { putString(key, value.value) }
    }

    override fun get(bundle: SavedState, key: String): CompletionId =
        CompletionId(bundle.read { getString(key) })

    override fun parseValue(value: String): CompletionId = CompletionId(value)

    override fun serializeAsValue(value: CompletionId): String = value.value
}

internal val questPackIdNavType: NavType<QuestPackId> = object : NavType<QuestPackId>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: QuestPackId) {
        bundle.write { putString(key, value.value) }
    }

    override fun get(bundle: SavedState, key: String): QuestPackId =
        QuestPackId(bundle.read { getString(key) })

    override fun parseValue(value: String): QuestPackId = QuestPackId(value)

    override fun serializeAsValue(value: QuestPackId): String = value.value
}
