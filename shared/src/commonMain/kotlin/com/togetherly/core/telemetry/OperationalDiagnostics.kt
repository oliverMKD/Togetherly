package com.togetherly.core.telemetry

/**
 * Provider-neutral — nothing here mentions Sentry or any other vendor (see `docs/telemetry.md`).
 * "Handled" is the operative word in [captureHandledException]: this is for exceptions the app
 * already caught and recovered from (matching [com.togetherly.core.error.AppError.Unexpected]'s
 * own niche), never a crash-reporting hook that runs before the app can decide what's safe to
 * attach — [context] and every [DiagnosticBreadcrumb.message] are still validated the same way
 * [ProductAnalytics] properties are before a real provider implementation may transmit them.
 *
 * [setCollectionEnabled] follows the same disabled-by-default, no-collection-before-consent
 * contract as [ProductAnalytics.setCollectionEnabled] — see that method's own KDoc. [clearContext]
 * drops accumulated breadcrumbs/tags without touching the enabled/disabled flag itself; called
 * together with [setCollectionEnabled] `false` by [TelemetryCoordinator] when diagnostics consent
 * is revoked.
 */
interface OperationalDiagnostics {
    fun captureHandledException(
        throwable: Throwable,
        context: DiagnosticContext,
    )

    fun addBreadcrumb(breadcrumb: DiagnosticBreadcrumb)

    fun setCollectionEnabled(enabled: Boolean)

    fun clearContext()

    /** See [ProviderConfigurationStatus]'s own KDoc for what each value means and who reports it. */
    fun configurationStatus(): ProviderConfigurationStatus
}
