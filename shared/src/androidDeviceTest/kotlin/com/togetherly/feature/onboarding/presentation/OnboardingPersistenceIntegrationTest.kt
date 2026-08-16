package com.togetherly.feature.onboarding.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.telemetry.FakeProductAnalytics
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.family.usecase.CreateFamilyProfile
import com.togetherly.domain.purchase.repository.FakeCustomerAttributesRepository
import com.togetherly.domain.quest.QuestCategory
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/**
 * The full pipeline, end to end: real onboarding screens → [OnboardingViewModel] → the real
 * [CreateFamilyProfile] use case → a repository. [OnboardingViewModelTest] (`commonTest`) already
 * proves the ViewModel's own action/state logic in isolation; this proves the *screens* correctly
 * capture what the user actually taps/types into that same pipeline, with nothing faked along the
 * way except the repository/clock/ID-generator boundary (exactly [CreateFamilyProfile]'s own unit
 * tests do too).
 */
@RunWith(AndroidJUnit4::class)
internal class OnboardingPersistenceIntegrationTest {

    private fun viewModel(repository: FakeFamilyRepository) = OnboardingViewModel(
        CreateFamilyProfile(
            familyRepository = repository,
            clock = TestAppClock(NOW),
            idGenerator = SequentialIdGenerator(prefix = "family"),
        ),
        FakeProductAnalytics().apply { setCollectionEnabled(true) },
        FakeCustomerAttributesRepository(),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completingOnboardingWithANameCreatesTheExpectedFamilyProfile() = runComposeUiTest {
        val repository = FakeFamilyRepository()
        setContent {
            TogetherlyTheme {
                OnboardingRoute(viewModel = viewModel(repository), onNavigateBack = {}, onFamilyCreated = {})
            }
        }
        waitForIdle()

        onNodeWithText("Create our family").performClick()
        waitForIdle()
        onNodeWithText("Family or team name").performTextInput("Riverside Crew")
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("6–8").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Create").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("10 minutes").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Not now").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Start our first adventure").performClick()
        waitForIdle()

        val profile = repository.savedProfiles.single()
        assertEquals(FamilyId("family-0"), profile.id)
        assertEquals(FamilyDisplayName("Riverside Crew"), profile.displayName)
        assertEquals(setOf(AgeBand.AGE_6_TO_8), profile.childAgeBands)
        assertEquals(setOf(QuestCategory.CREATE), profile.interests)
        assertEquals(setOf(DurationBand.TEN_MINUTES), profile.preferredDurations)
        assertNull(profile.reminderPreference)
        assertEquals(NOW, profile.createdAt)
        assertEquals(NOW, profile.updatedAt)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun skippingTheFamilyNameStepPersistsANullDisplayName() = runComposeUiTest {
        val repository = FakeFamilyRepository()
        setContent {
            TogetherlyTheme {
                OnboardingRoute(viewModel = viewModel(repository), onNavigateBack = {}, onFamilyCreated = {})
            }
        }
        waitForIdle()

        onNodeWithText("Create our family").performClick()
        waitForIdle()
        onNodeWithText("Skip for now").performClick()
        waitForIdle()
        onNodeWithText("6–8").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Create").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("10 minutes").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Not now").performClick()
        onNodeWithText("Continue").performClick()
        waitForIdle()
        onNodeWithText("Start our first adventure").performClick()
        waitForIdle()

        assertNull(repository.savedProfiles.single().displayName)
    }
}
