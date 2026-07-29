package com.togetherly.core.telemetry

/** Does nothing, unconditionally — see [NoOpProductAnalytics]'s own KDoc; the same reasoning applies here. */
class NoOpOperationalDiagnostics : OperationalDiagnostics {
    override fun captureHandledException(throwable: Throwable, context: DiagnosticContext) = Unit
    override fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb) = Unit
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun clearContext() = Unit
    override fun configurationStatus(): ProviderConfigurationStatus = ProviderConfigurationStatus.MISSING
}
