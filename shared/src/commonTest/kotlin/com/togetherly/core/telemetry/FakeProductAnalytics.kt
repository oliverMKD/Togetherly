package com.togetherly.core.telemetry

class FakeProductAnalytics : ProductAnalytics {

    val capturedEvents: MutableList<AnalyticsEvent> = mutableListOf()
    val screensViewed: MutableList<AnalyticsScreen> = mutableListOf()
    var flushCallCount: Int = 0
        private set
    var resetCallCount: Int = 0
        private set
    var collectionEnabled: Boolean = false
        private set
    val collectionEnabledCalls: MutableList<Boolean> = mutableListOf()

    private var throwOnSetCollectionEnabled: Throwable? = null

    fun throwOnNextSetCollectionEnabled(throwable: Throwable) {
        throwOnSetCollectionEnabled = throwable
    }

    override fun capture(event: AnalyticsEvent) {
        if (!collectionEnabled) return
        capturedEvents += event
    }

    override fun screen(screen: AnalyticsScreen) {
        if (!collectionEnabled) return
        screensViewed += screen
    }

    override fun flush() {
        flushCallCount++
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        throwOnSetCollectionEnabled?.let {
            throwOnSetCollectionEnabled = null
            throw it
        }
        collectionEnabled = enabled
        collectionEnabledCalls += enabled
    }

    override fun reset() {
        resetCallCount++
    }

    var configurationStatusValue: ProviderConfigurationStatus = ProviderConfigurationStatus.CONFIGURED
    override fun configurationStatus(): ProviderConfigurationStatus = configurationStatusValue
}
