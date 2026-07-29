package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.application.LegalConfiguration
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.feature.family.model.AboutAction
import com.togetherly.feature.family.model.AboutEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeVersionInfoProvider(
    private val versionName: String = "1.0",
    private val buildNumber: String = "1",
) : VersionInfoProvider {
    override fun versionName(): String = versionName
    override fun buildNumber(): String = buildNumber
}

private fun legalConfiguration(supportContactUrl: String? = null) = LegalConfiguration(
    privacyPolicyUrl = "https://example.com/togetherly/privacy",
    termsOfUseUrl = "https://example.com/togetherly/terms",
    subscriptionTermsUrl = "https://example.com/togetherly/subscription-terms",
    supportContactUrl = supportContactUrl,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapsAppConfigurationAndVersionInfoIntoUiState() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(versionName = "2.3", buildNumber = "42"),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertEquals("Togetherly", model.uiState.value.applicationName)
        assertEquals("2.3", model.uiState.value.versionName)
        assertEquals("42", model.uiState.value.buildNumber)
    }

    @Test
    fun debugBuildShowsEnvironmentLabel() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertTrue(model.uiState.value.showEnvironmentLabel)
    }

    @Test
    fun releaseBuildHidesEnvironmentLabel() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertFalse(model.uiState.value.showEnvironmentLabel)
    }

    @Test
    fun unconfiguredSupportContactIsNull() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(supportContactUrl = null),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertNull(model.uiState.value.supportContactUrl)
    }

    @Test
    fun supportClickedEmitsOpenExternalUrlWhenConfigured() = runTest {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(supportContactUrl = "mailto:support@example.com"),
            diagnostics = FakeOperationalDiagnostics(),
        )

        model.events.test {
            model.onAction(AboutAction.SupportClicked)
            assertEquals(AboutEvent.OpenExternalUrl("mailto:support@example.com"), awaitItem())
        }
    }

    @Test
    fun backClickedEmitsNavigateBack() = runTest {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        model.events.test {
            model.onAction(AboutAction.BackClicked)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun debugBuildShowsDiagnosticsTestAction() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertTrue(model.uiState.value.showDiagnosticsTestAction)
    }

    @Test
    fun releaseBuildNeverShowsDiagnosticsTestAction() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertFalse(model.uiState.value.showDiagnosticsTestAction)
    }

    @Test
    fun sendTestDiagnosticClickedCapturesAHandledExceptionOnADebugBuild() {
        val diagnostics = FakeOperationalDiagnostics()
        diagnostics.setCollectionEnabled(true)
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = diagnostics,
        )

        model.onAction(AboutAction.SendTestDiagnosticClicked)

        assertEquals(1, diagnostics.capturedExceptions.size)
        assertTrue(model.uiState.value.diagnosticsTestJustSent)
    }

    @Test
    fun sendTestDiagnosticClickedDoesNothingOnAReleaseBuild() {
        val diagnostics = FakeOperationalDiagnostics()
        diagnostics.setCollectionEnabled(true)
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = diagnostics,
        )

        model.onAction(AboutAction.SendTestDiagnosticClicked)

        assertTrue(diagnostics.capturedExceptions.isEmpty())
        assertFalse(model.uiState.value.diagnosticsTestJustSent)
    }

    @Test
    fun debugBuildShowsDebugTelemetryAction() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertTrue(model.uiState.value.showDebugTelemetryAction)
    }

    @Test
    fun releaseBuildNeverShowsDebugTelemetryAction() {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        assertFalse(model.uiState.value.showDebugTelemetryAction)
    }

    @Test
    fun openDebugTelemetryClickedEmitsOpenDebugTelemetryOnADebugBuild() = runTest {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = true),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        model.events.test {
            model.onAction(AboutAction.OpenDebugTelemetryClicked)
            assertEquals(AboutEvent.OpenDebugTelemetry, awaitItem())
        }
    }

    @Test
    fun openDebugTelemetryClickedDoesNothingOnAReleaseBuild() = runTest {
        val model = AboutViewModel(
            appConfiguration = AppConfiguration(applicationName = "Togetherly", debug = false),
            versionInfoProvider = FakeVersionInfoProvider(),
            legalConfiguration = legalConfiguration(),
            diagnostics = FakeOperationalDiagnostics(),
        )

        model.events.test {
            model.onAction(AboutAction.OpenDebugTelemetryClicked)
            model.onAction(AboutAction.BackClicked)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }
}
