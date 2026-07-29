package com.togetherly.buildvalidation

import com.togetherly.core.telemetry.AnalyticsEvent
import com.togetherly.core.telemetry.TelemetryEventRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Step 14.6's remaining build-validation checks — the ones that need a real filesystem (source
 * files, `docs/`) rather than just in-memory Kotlin objects, so they live here (`androidHostTest`,
 * a plain JVM test source set) instead of `commonTest`. Mirrors the working-directory assumption
 * `ci.yml`'s `:shared:testAndroidHostTest` step already relies on implicitly: Gradle runs this
 * task with the `shared/` module directory as the working directory, so `src/commonMain/...` and
 * `../docs/...` both resolve correctly both locally and in CI — see [projectRoot]/[sharedModuleRoot].
 *
 * See [com.togetherly.core.telemetry.TelemetryEventRegistryValidationTest] (`commonTest`) for the
 * cross-platform half of Step 14.6's build validation (event-name uniqueness/snake_case), which
 * doesn't need file access at all.
 */
class BuildValidationChecksTest {

    private val sharedModuleRoot = File("src")
    private val projectRoot = File("..")

    /** [com.togetherly.core.telemetry.AnalyticsEvent.DebugTestEvent] is deliberately debug-only — never a product signal — so the taxonomy doc's product-event catalogue correctly omits it. */
    private val nonProductEventNames = setOf("debug_test_event")

    /** The only files allowed to mention `com.togetherly.feature.debug` outside that package's own sources — DI wiring and navigation plumbing, never another feature reaching in directly. */
    private val allowedDebugUiReferrers = setOf(
        "commonMain/kotlin/com/togetherly/app/di/PresentationModule.kt",
        "commonMain/kotlin/com/togetherly/navigation/destination/RootDestination.kt",
        "commonMain/kotlin/com/togetherly/navigation/host/TogetherlyNavHost.kt",
    )

    private fun ktFiles(root: File): List<File> = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun everyNonProductEventNameAppearsInTheAnalyticsTaxonomyDoc() {
        val taxonomyDoc = File(projectRoot, "docs/analytics-event-taxonomy.md")
        assertTrue(taxonomyDoc.exists(), "docs/analytics-event-taxonomy.md not found at ${taxonomyDoc.absolutePath}")
        val taxonomyText = taxonomyDoc.readText()

        val undocumented = TelemetryEventRegistry.schemas.keys
            .filterNot { it in nonProductEventNames }
            .filterNot { taxonomyText.contains("`$it`") }

        assertTrue(undocumented.isEmpty(), "Event(s) missing from docs/analytics-event-taxonomy.md: $undocumented")
    }

    @Test
    fun featureCodeNeverImportsPostHogOrSentrySdkTypesDirectly() {
        val featureRoot = File(sharedModuleRoot, "commonMain/kotlin/com/togetherly/feature")
        assertTrue(featureRoot.exists(), "feature package not found at ${featureRoot.absolutePath}")

        val offenders = ktFiles(featureRoot)
            .filter { file -> file.readLines().any { it.startsWith("import com.posthog.kmp.") || it.startsWith("import io.sentry.") } }
            .map { it.relativeTo(sharedModuleRoot).path }

        assertTrue(
            offenders.isEmpty(),
            "Feature code must depend only on ProductAnalytics/OperationalDiagnostics, never a concrete SDK type directly: $offenders",
        )
    }

    @Test
    fun productionCodeReferencesDebugTelemetryUiOnlyThroughTheAllowedIntegrationPoints() {
        val commonMainRoot = File(sharedModuleRoot, "commonMain/kotlin")
        val debugFeatureRoot = File(commonMainRoot, "com/togetherly/feature/debug")
        assertTrue(debugFeatureRoot.exists(), "feature/debug package not found at ${debugFeatureRoot.absolutePath}")

        val referrers = ktFiles(commonMainRoot)
            .filterNot { it.startsWith(debugFeatureRoot) }
            // Real `import` lines only — a KDoc cross-reference like [Foo][com.togetherly.feature.debug.Foo]
            // creates no actual compile-time dependency and isn't what this check is for.
            .filter { file -> file.readLines().any { it.startsWith("import com.togetherly.feature.debug") } }
            .map { it.relativeTo(sharedModuleRoot).path }
            .toSet()

        val unexpected = referrers - allowedDebugUiReferrers
        assertTrue(
            unexpected.isEmpty(),
            "Only DI wiring and navigation plumbing may reference feature.debug from outside that package: $unexpected",
        )
    }

    /**
     * The allowlist above is deliberately explicit rather than "anything under app/di or
     * navigation/" — this test fails loudly if the allowlist itself has gone stale (a file was
     * removed/renamed) rather than the allowlist silently becoming over-broad.
     */
    @Test
    fun theAllowedDebugUiReferrerListItselfPointsAtRealFiles() {
        val missing = allowedDebugUiReferrers.filterNot { File(sharedModuleRoot, it).exists() }
        assertTrue(missing.isEmpty(), "Allowlisted file(s) no longer exist: $missing")
    }

    /** Sanity check on the test itself: [AnalyticsEvent] still exists at the import above and the registry is non-empty, so the two checks above aren't silently scanning nothing. */
    @Test
    fun sanityRegistryIsReachableFromThisTestSourceSet() {
        assertTrue(TelemetryEventRegistry.schemas.isNotEmpty())
    }
}
