package com.togetherly.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class SampleException(message: String? = null) : Exception(message)

class DiagnosticSanitizerTest {

    private val sanitizer = DiagnosticSanitizer()

    @Test
    fun sanitizeTagsAlwaysIncludesTheExceptionTypeTag() {
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext())

        assertEquals("SampleException", tags["exception_type"])
    }

    @Test
    fun sanitizeTagsOverridesACallerSuppliedExceptionTypeTag() {
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext(mapOf("exception_type" to "spoofed")))

        assertEquals("SampleException", tags["exception_type"])
    }

    @Test
    fun safeTagValuesPassThroughUnchanged() {
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext(mapOf("feature" to "purchase", "operation" to "revenuecat_call")))

        assertEquals("purchase", tags["feature"])
        assertEquals("revenuecat_call", tags["operation"])
    }

    @Test
    fun sensitiveContextIsRejectedNotRedacted() {
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext(mapOf("user_email" to "parent@example.com")))

        assertTrue("user_email" !in tags)
    }

    @Test
    fun aSlashFreeUrlLikeValueIsRejected() {
        // A slash-bearing URL ("https://...") is caught by the file-path stripping below instead
        // (see filePathsAreSanitizedRatherThanRejectedOutright) and survives as "<path>" — this
        // covers the one shape stripping can't neutralize: a bare "www." host with no path at all.
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext(mapOf("hint" to "www.example.com")))

        assertTrue("hint" !in tags)
    }

    @Test
    fun filePathsAreSanitizedRatherThanRejectedOutright() {
        val tags = sanitizer.sanitizeTags(
            SampleException(),
            DiagnosticContext(mapOf("detail" to "Error reading /data/user/0/com.togetherly/files/photo.jpg")),
        )

        assertEquals("Error reading <path>", tags["detail"])
    }

    @Test
    fun urlQueryParametersAreStrippedBeforeThePathCheck() {
        val tags = sanitizer.sanitizeTags(
            SampleException(),
            DiagnosticContext(mapOf("detail" to "loaded config?token=abc123&user=me")),
        )

        assertEquals("loaded config", tags["detail"])
    }

    @Test
    fun blankTagKeysAreDropped() {
        val tags = sanitizer.sanitizeTags(SampleException(), DiagnosticContext(mapOf("" to "value", "feature" to "purchase")))

        assertEquals(setOf("feature", "exception_type"), tags.keys)
    }

    @Test
    fun sanitizeBreadcrumbMessageReturnsSafeMessageUnchanged() {
        assertEquals("catalogue load started", sanitizer.sanitizeBreadcrumbMessage("catalogue load started"))
    }

    @Test
    fun sanitizeBreadcrumbMessageDropsAnUnsafeMessage() {
        assertNull(sanitizer.sanitizeBreadcrumbMessage("contact parent@example.com for details"))
    }

    @Test
    fun sanitizeBreadcrumbMessageDropsAnOverlongMessage() {
        assertNull(sanitizer.sanitizeBreadcrumbMessage("x".repeat(201)))
    }
}
