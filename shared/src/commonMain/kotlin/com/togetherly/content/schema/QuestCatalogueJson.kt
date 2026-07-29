package com.togetherly.content.schema

import kotlinx.serialization.json.Json

/**
 * Scoped to catalogue parsing only — not a global application JSON configuration. Strict by
 * design: unknown keys and invalid enum values must fail loudly so authoring mistakes are
 * caught, not silently coerced or dropped.
 */
internal val questCatalogueJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    explicitNulls = false
    coerceInputValues = false
}
