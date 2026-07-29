package com.togetherly.data.telemetry

/**
 * The one fake standing in for [SentrySdkAdapter] in tests — never the real `io.sentry.kotlin.multiplatform`
 * global `object`, per that interface's own KDoc on why it exists.
 */
internal class FakeSentrySdkAdapter : SentrySdkAdapter {

    data class SetupCall(val dsn: String, val release: String, val environment: String, val debug: Boolean)
    data class CaptureCall(val throwable: Throwable, val tags: Map<String, String>)
    data class BreadcrumbCall(val message: String, val category: String?)

    val setupCalls: MutableList<SetupCall> = mutableListOf()
    val captureCalls: MutableList<CaptureCall> = mutableListOf()
    val breadcrumbCalls: MutableList<BreadcrumbCall> = mutableListOf()
    var clearScopeCallCount: Int = 0
        private set
    var closeCallCount: Int = 0
        private set

    private var throwOnSetup: Throwable? = null
    private var throwOnCapture: Throwable? = null

    fun throwOnNextSetup(throwable: Throwable) {
        throwOnSetup = throwable
    }

    fun throwOnNextCapture(throwable: Throwable) {
        throwOnCapture = throwable
    }

    override fun setup(dsn: String, release: String, environment: String, debug: Boolean) {
        throwOnSetup?.let {
            throwOnSetup = null
            throw it
        }
        setupCalls += SetupCall(dsn, release, environment, debug)
    }

    override fun captureException(throwable: Throwable, tags: Map<String, String>) {
        throwOnCapture?.let {
            throwOnCapture = null
            throw it
        }
        captureCalls += CaptureCall(throwable, tags)
    }

    override fun addBreadcrumb(message: String, category: String?) {
        breadcrumbCalls += BreadcrumbCall(message, category)
    }

    override fun clearScope() {
        clearScopeCallCount++
    }

    override fun close() {
        closeCallCount++
    }
}
