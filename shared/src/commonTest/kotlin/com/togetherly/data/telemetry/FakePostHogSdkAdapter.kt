package com.togetherly.data.telemetry

/**
 * The one fake standing in for [PostHogSdkAdapter] in tests — never the real `com.posthog.kmp`
 * global `object`, per that interface's own KDoc on why it exists.
 */
internal class FakePostHogSdkAdapter : PostHogSdkAdapter {

    data class SetupCall(val projectKey: String, val host: String?, val debug: Boolean)
    data class CaptureCall(val event: String, val properties: Map<String, Any>)
    data class ScreenCall(val screenName: String, val properties: Map<String, Any>)

    val setupCalls: MutableList<SetupCall> = mutableListOf()
    val captureCalls: MutableList<CaptureCall> = mutableListOf()
    val screenCalls: MutableList<ScreenCall> = mutableListOf()
    var optInCallCount: Int = 0
        private set
    var optOutCallCount: Int = 0
        private set
    var resetCallCount: Int = 0
        private set
    var flushCallCount: Int = 0
        private set

    var anonymousIdValue: String? = "test-anonymous-id"

    private var throwOnSetup: Throwable? = null
    private var throwOnCapture: Throwable? = null

    fun throwOnNextSetup(throwable: Throwable) {
        throwOnSetup = throwable
    }

    fun throwOnNextCapture(throwable: Throwable) {
        throwOnCapture = throwable
    }

    override fun setup(projectKey: String, host: String?, debug: Boolean) {
        throwOnSetup?.let {
            throwOnSetup = null
            throw it
        }
        setupCalls += SetupCall(projectKey, host, debug)
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        throwOnCapture?.let {
            throwOnCapture = null
            throw it
        }
        captureCalls += CaptureCall(event, properties)
    }

    override fun screen(screenName: String, properties: Map<String, Any>) {
        screenCalls += ScreenCall(screenName, properties)
    }

    override fun optIn() {
        optInCallCount++
    }

    override fun optOut() {
        optOutCallCount++
    }

    override fun reset() {
        resetCallCount++
    }

    override fun flush() {
        flushCallCount++
    }

    override fun anonymousId(): String? = anonymousIdValue
}
