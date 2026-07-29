package com.togetherly.feature.family.presentation

import app.cash.turbine.test
import com.togetherly.feature.family.model.OpenSourceLicense
import com.togetherly.feature.family.model.OpenSourceLicensesAction
import com.togetherly.feature.family.model.OpenSourceLicensesEvent
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
class OpenSourceLicensesViewModelTest {

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
    fun backClickedEmitsNavigateBack() = runTest {
        val model = OpenSourceLicensesViewModel()

        model.events.test {
            model.onAction(OpenSourceLicensesAction.BackClicked)
            assertEquals(OpenSourceLicensesEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun licenseClickedEmitsOpenExternalUrlForValidUrl() = runTest {
        val model = OpenSourceLicensesViewModel()
        val license = OpenSourceLicense("Example", "Apache License 2.0", "https://example.com/license")

        model.events.test {
            model.onAction(OpenSourceLicensesAction.LicenseClicked(license))
            assertEquals(OpenSourceLicensesEvent.OpenExternalUrl(license.url), awaitItem())
        }
    }

    @Test
    fun licenseClickedWithInvalidUrlEmitsNothing() = runTest {
        val model = OpenSourceLicensesViewModel()
        val license = OpenSourceLicense("Example", "Apache License 2.0", "not-a-valid-url")

        model.events.test {
            model.onAction(OpenSourceLicensesAction.LicenseClicked(license))
            expectNoEvents()
        }
    }
}
