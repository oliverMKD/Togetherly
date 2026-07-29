package com.togetherly.core.telemetry

class FakeOperationalDiagnostics : OperationalDiagnostics {

    data class CapturedException(val throwable: Throwable, val context: DiagnosticContext)

    val capturedExceptions: MutableList<CapturedException> = mutableListOf()
    val breadcrumbs: MutableList<DiagnosticBreadcrumb> = mutableListOf()
    var clearContextCallCount: Int = 0
        private set
    var collectionEnabled: Boolean = false
        private set
    val collectionEnabledCalls: MutableList<Boolean> = mutableListOf()

    override fun captureHandledException(throwable: Throwable, context: DiagnosticContext) {
        if (!collectionEnabled) return
        capturedExceptions += CapturedException(throwable, context)
    }

    override fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb) {
        if (!collectionEnabled) return
        breadcrumbs += breadcrumb
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        collectionEnabledCalls += enabled
    }

    override fun clearContext() {
        clearContextCallCount++
    }

    var configurationStatusValue: ProviderConfigurationStatus = ProviderConfigurationStatus.CONFIGURED
    override fun configurationStatus(): ProviderConfigurationStatus = configurationStatusValue
}
