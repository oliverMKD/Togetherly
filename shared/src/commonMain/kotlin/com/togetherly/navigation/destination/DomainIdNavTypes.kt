package com.togetherly.navigation.destination

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.paywall.model.PaywallContext
import com.togetherly.feature.questdetail.model.QuestOpenSource

/**
 * [RootDestination]'s `@Serializable` route arguments use [QuestId]/[CompletionId] — inline value
 * classes wrapping [String] — rather than raw strings, so callers can never mix up which ID a nav
 * argument holds (see each route's own KDoc). Navigation's type-safe routing only infers a
 * [NavType] automatically for its built-in argument types; inline value classes and feature enums
 * are not inferred consistently across Kotlin Multiplatform targets. Without these, constructing
 * the iOS graph throws `could not find any NavType for argument`. These are passed as the
 * `typeMap` for every `composable<...>` block in [com.togetherly.navigation.host.TogetherlyNavHost]
 * whose route carries the corresponding type.
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

internal val paywallContextNavType: NavType<PaywallContext> = enumNavType(PaywallContext::valueOf)

internal val questOpenSourceNavType: NavType<QuestOpenSource> = enumNavType(QuestOpenSource::valueOf)

private fun <T : Enum<T>> enumNavType(parse: (String) -> T): NavType<T> = object : NavType<T>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: T) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): T =
        parse(bundle.read { getString(key) })

    override fun parseValue(value: String): T = parse(value)

    override fun serializeAsValue(value: T): String = value.name
}
