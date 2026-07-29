package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.app.application.LegalConfiguration
import com.togetherly.feature.family.model.LegalAction
import com.togetherly.feature.family.model.LegalEvent
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

@OptIn(ExperimentalCoroutinesApi::class)
class LegalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(configuration: LegalConfiguration = LegalConfiguration.placeholder()) = LegalViewModel(configuration)

    @Test
    fun privacyPolicyClickedEmitsOpenExternalUrlForConfiguredUrl() = runTest {
        val configuration = LegalConfiguration.placeholder()
        val model = viewModel(configuration)

        model.events.test {
            model.onAction(LegalAction.PrivacyPolicyClicked)
            assertEquals(LegalEvent.OpenExternalUrl(configuration.privacyPolicyUrl), awaitItem())
        }
    }

    @Test
    fun invalidUrlNeverEmitsOpenExternalUrl() = runTest {
        val model = viewModel(
            LegalConfiguration(
                privacyPolicyUrl = "not-a-valid-url",
                termsOfUseUrl = "https://example.com/togetherly/terms",
                subscriptionTermsUrl = "https://example.com/togetherly/subscription-terms",
                supportContactUrl = null,
            ),
        )

        model.events.test {
            model.onAction(LegalAction.PrivacyPolicyClicked)
            model.onAction(LegalAction.TermsOfUseClicked)
            assertEquals(LegalEvent.OpenExternalUrl("https://example.com/togetherly/terms"), awaitItem())
        }
    }

    @Test
    fun openSourceLicensesClickedEmitsOpenOpenSourceLicenses() = runTest {
        val model = viewModel()

        model.events.test {
            model.onAction(LegalAction.OpenSourceLicensesClicked)
            assertEquals(LegalEvent.OpenOpenSourceLicenses, awaitItem())
        }
    }

    @Test
    fun backClickedEmitsNavigateBack() = runTest {
        val model = viewModel()

        model.events.test {
            model.onAction(LegalAction.BackClicked)
            assertEquals(LegalEvent.NavigateBack, awaitItem())
        }
    }
}
