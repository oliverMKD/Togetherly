package com.togetherly.data.telemetry

import com.togetherly.core.telemetry.DiagnosticBreadcrumb
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.DiagnosticSanitizer
import com.togetherly.core.telemetry.DuplicateSignalDetector
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.ProviderConfigurationStatus
import com.togetherly.core.telemetry.TelemetryDebugRecorder

/**
 * The diagnostics-side counterpart to [DebugRecordingProductAnalytics] — see that class's own KDoc
 * for the "forward first, record second, never alter behavior" shape this mirrors exactly, and for
 * why [collectionEnabled] gates [recorder] the same "no capture before consent, even locally" way
 * [DebugOperationalDiagnostics]'s own logging already does, while [duplicateDetector] stays
 * ungated. [recorder] only ever holds an exception's class name (never its message — see
 * [OperationalDiagnostics.captureHandledException]'s own KDoc on why a message is never guaranteed
 * safe) and a breadcrumb message that already survived [sanitizer]; an unsafe breadcrumb is dropped
 * from [recorder] the same way it would be dropped before ever reaching a real provider, never
 * recorded in redacted form.
 */
internal class DebugRecordingOperationalDiagnostics(
    private val delegate: OperationalDiagnostics,
    private val recorder: TelemetryDebugRecorder,
    private val duplicateDetector: DuplicateSignalDetector,
    private val sanitizer: DiagnosticSanitizer = DiagnosticSanitizer(),
) : OperationalDiagnostics {

    private var collectionEnabled = false

    override fun captureHandledException(throwable: Throwable, context: DiagnosticContext) {
        val category = throwable::class.simpleName ?: "UnknownThrowable"
        val operationId = "$category:${context.tags["operation"] ?: context.tags["feature"] ?: ""}"
        duplicateDetector.check(operationId)
        if (collectionEnabled) {
            recorder.recordProviderError(category)
        }
        delegate.captureHandledException(throwable, context)
    }

    override fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb) {
        if (collectionEnabled) {
            sanitizer.sanitizeBreadcrumbMessage(breadcrumb.message)?.let { safeMessage ->
                recorder.recordBreadcrumb(safeMessage, breadcrumb.category)
            }
        }
        delegate.addBreadcrumb(breadcrumb)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        delegate.setCollectionEnabled(enabled)
    }

    override fun clearContext() = delegate.clearContext()
    override fun configurationStatus(): ProviderConfigurationStatus = delegate.configurationStatus()
}
