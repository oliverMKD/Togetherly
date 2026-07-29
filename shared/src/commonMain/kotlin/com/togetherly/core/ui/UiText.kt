package com.togetherly.core.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A string a presentation layer can hold and pass around (in a `UiState`, across a
 * `ViewModel`/Composable boundary) without resolving it to text yet — resolution needs a
 * [Composable] context (for [Resource], to read the current locale's Compose resource), which a
 * `ViewModel` never has.
 *
 * [Dynamic] exists for the rare case where the text is already fully formed at the point of
 * construction (e.g. echoing back a user-entered name) — it is never a way to sneak an
 * un-translated literal past this type for copy that should be a [Resource] instead.
 */
sealed interface UiText {

    data class Resource(
        val resource: StringResource,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText

    data class Dynamic(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> if (formatArgs.isEmpty()) {
        stringResource(resource)
    } else {
        stringResource(resource, *formatArgs.toTypedArray())
    }
    is UiText.Dynamic -> value
}
