package com.togetherly.data.local.database

import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class SampleMigrationFailure : Exception("simulated migration failure")

class DatabaseBuilderTest {

    @Test
    fun aSuccessfulBuildNeverCapturesAnything() {
        val diagnostics = FakeOperationalDiagnostics()
        diagnostics.setCollectionEnabled(true)
        val sentinel = object {}

        val result = buildTogetherlyDatabaseCapturingFailures(diagnostics) { sentinel }

        assertEquals(sentinel, result)
        assertTrue(diagnostics.capturedExceptions.isEmpty())
    }

    @Test
    fun aMigrationFailureIsCapturedAndRethrown() {
        val diagnostics = FakeOperationalDiagnostics()
        diagnostics.setCollectionEnabled(true)

        assertFailsWith<SampleMigrationFailure> {
            buildTogetherlyDatabaseCapturingFailures(diagnostics) { throw SampleMigrationFailure() }
        }

        val captured = diagnostics.capturedExceptions.single()
        assertTrue(captured.throwable is SampleMigrationFailure)
        assertEquals(DiagnosticContext(mapOf("feature" to "database", "operation" to "migration")), captured.context)
    }
}
