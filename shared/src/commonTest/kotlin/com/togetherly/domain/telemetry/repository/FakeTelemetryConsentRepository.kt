package com.togetherly.domain.telemetry.repository

import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTelemetryConsentRepository(
    initialConsent: TelemetryConsent = TelemetryConsent.default(),
) : TelemetryConsentRepository {

    private val consentFlow = MutableStateFlow(initialConsent)

    var resetConsentCallCount: Int = 0
        private set

    val currentConsent: TelemetryConsent
        get() = consentFlow.value

    fun setConsent(consent: TelemetryConsent) {
        consentFlow.value = consent
    }

    override fun observeConsent(): Flow<TelemetryConsent> = consentFlow

    override suspend fun updateAnalyticsConsent(decision: ConsentDecision) {
        consentFlow.update { it.copy(analytics = decision) }
    }

    override suspend fun updateDiagnosticsConsent(decision: ConsentDecision) {
        consentFlow.update { it.copy(diagnostics = decision) }
    }

    override suspend fun resetConsent() {
        resetConsentCallCount++
        consentFlow.value = TelemetryConsent.default()
    }
}
