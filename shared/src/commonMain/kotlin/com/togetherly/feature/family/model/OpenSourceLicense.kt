package com.togetherly.feature.family.model

data class OpenSourceLicense(
    val name: String,
    val licenseType: String,
    val url: String,
)

/**
 * Hand-curated against this project's own `gradle/libs.versions.toml` plus the transitive runtime
 * dependencies `./gradlew :shared:printResolvedRuntimeDependencies` actually resolves (notably
 * RevenueCat's own Android SDK dependencies — OkHttp/Okio/Coil/commonmark — which do ship in the
 * built app even though this module never declares them directly). Test-only dependencies
 * (Turbine, `kotlin-test`, `androidx.test.*`) are deliberately excluded, since they never ship in
 * the built app. See `docs/open-source-licenses.md` for how to regenerate/verify this list, and
 * update it here by hand whenever a runtime dependency changes.
 */
val OPEN_SOURCE_LICENSES: List<OpenSourceLicense> = listOf(
    OpenSourceLicense("Kotlin Standard Library", "Apache License 2.0", "https://github.com/JetBrains/kotlin"),
    OpenSourceLicense("kotlinx.coroutines", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    OpenSourceLicense("kotlinx.datetime", "Apache License 2.0", "https://github.com/Kotlin/kotlinx-datetime"),
    OpenSourceLicense("kotlinx.serialization", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    OpenSourceLicense("kotlinx.collections.immutable", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.collections.immutable"),
    OpenSourceLicense("Compose Multiplatform", "Apache License 2.0", "https://github.com/JetBrains/compose-multiplatform"),
    OpenSourceLicense("AndroidX Room", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    OpenSourceLicense("AndroidX Lifecycle", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    OpenSourceLicense("AndroidX Navigation", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    OpenSourceLicense("AndroidX SQLite", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/sqlite"),
    OpenSourceLicense("AndroidX Activity", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/activity"),
    OpenSourceLicense("Koin", "Apache License 2.0", "https://github.com/InsertKoinIO/koin"),
    OpenSourceLicense("RevenueCat Purchases KMP", "MIT License", "https://github.com/RevenueCat/purchases-kmp"),
    OpenSourceLicense("RevenueCat Purchases Android SDK", "MIT License", "https://github.com/RevenueCat/purchases-android"),
    OpenSourceLicense("OkHttp", "Apache License 2.0", "https://github.com/square/okhttp"),
    OpenSourceLicense("Okio", "Apache License 2.0", "https://github.com/square/okio"),
    OpenSourceLicense("Coil", "Apache License 2.0", "https://github.com/coil-kt/coil"),
    OpenSourceLicense("commonmark-java", "BSD 2-Clause License", "https://github.com/commonmark/commonmark-java"),
)
