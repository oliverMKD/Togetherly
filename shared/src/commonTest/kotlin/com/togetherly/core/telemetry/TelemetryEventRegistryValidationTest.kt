package com.togetherly.core.telemetry

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Step 14.6's build-validation checks for [TelemetryEventRegistry] itself — event-name uniqueness
 * is enforced at construction time (see [TelemetryEventRegistry.schemas]'s own `require`, which
 * this file's mere successful compilation/execution already exercises); this file covers the
 * naming-convention half: every registered event name is lowercase snake_case, matching the schema
 * every event in `docs/analytics-event-taxonomy.md` and [AnalyticsEvent]'s own `EVENT_NAME`
 * constants already follow by convention. Cross-platform (`commonTest`) since it only touches
 * [TelemetryEventRegistry], never the filesystem — see `androidHostTest`'s
 * `BuildValidationChecksTest` for the source/doc-scanning checks that do need a JVM filesystem.
 */
class TelemetryEventRegistryValidationTest {

    private val snakeCase = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")

    @Test
    fun everyRegisteredEventNameIsSnakeCase() {
        for (eventName in TelemetryEventRegistry.schemas.keys) {
            assertTrue(snakeCase.matches(eventName), "\"$eventName\" is not snake_case")
        }
    }

    @Test
    fun everyAllowedPropertyNameIsSnakeCase() {
        for (schema in TelemetryEventRegistry.schemas.values) {
            for (property in schema.allowedProperties) {
                assertTrue(snakeCase.matches(property), "\"$property\" (on \"${schema.eventName}\") is not snake_case")
            }
        }
    }

    @Test
    fun registryIsNonEmptyAndLoadsWithoutDuplicateNameCrash() {
        // TelemetryEventRegistry.schemas' own `require` would already have thrown at class-init
        // time if two schemas shared an eventName — reaching this assertion at all is half the
        // proof; the size check catches the registry being accidentally emptied instead.
        assertTrue(TelemetryEventRegistry.schemas.isNotEmpty())
    }
}
