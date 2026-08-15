package com.togetherly.feature.family.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.feature.family.model.FamilyAction
import com.togetherly.feature.family.model.FamilyUiState
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class FamilyScreenTest {

    private val loadedState = FamilyUiState(
        isLoading = false,
        familyName = "Team Firefly",
        ageBands = persistentSetOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
        preferredDurations = persistentSetOf(DurationBand.TEN_MINUTES, DurationBand.TWENTY_MINUTES),
        locationPreference = LocationPreference.BOTH,
    )

    @Composable
    private fun NoFamilyPlusSection() {
        Text("family-plus-slot", style = MaterialTheme.typography.bodySmall)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun assertRowEmits(rowText: String, expected: FamilyAction) = runComposeUiTest {
        val actions = mutableListOf<FamilyAction>()
        setContent {
            TogetherlyTheme {
                FamilyScreen(state = loadedState, onAction = actions::add, familyPlusSection = { NoFamilyPlusSection() })
            }
        }

        onNodeWithText(rowText).performScrollTo().performClick()

        assertEquals(listOf(expected), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun questPreferencesRowEmitsQuestPreferencesClicked() =
        assertRowEmits("Quest preferences", FamilyAction.QuestPreferencesClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reminderRowEmitsReminderClicked() =
        assertRowEmits("Reminders", FamilyAction.ReminderClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun memorySettingsRowEmitsMemorySettingsClicked() =
        assertRowEmits("Memory settings", FamilyAction.MemorySettingsClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun privacyRowEmitsPrivacyClicked() =
        assertRowEmits("Privacy", FamilyAction.PrivacyClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dataManagementRowEmitsDataManagementClicked() =
        assertRowEmits("Manage your data", FamilyAction.DataManagementClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun legalRowEmitsLegalClicked() =
        assertRowEmits("Legal", FamilyAction.LegalClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aboutRowEmitsAboutClicked() =
        assertRowEmits("About", FamilyAction.AboutClicked)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editProfileEmitsEditProfileClicked() = runComposeUiTest {
        val actions = mutableListOf<FamilyAction>()
        setContent {
            TogetherlyTheme {
                FamilyScreen(state = loadedState, onAction = actions::add, familyPlusSection = { NoFamilyPlusSection() })
            }
        }

        onNodeWithText("Edit").performScrollTo().performClick()

        assertEquals(listOf<FamilyAction>(FamilyAction.EditProfileClicked), actions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun everySectionHeaderAndSubtitleIsDisplayed() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                FamilyScreen(state = loadedState, onAction = {}, familyPlusSection = { NoFamilyPlusSection() })
            }
        }

        onNodeWithText("Your family").assertIsDisplayed()
        onNodeWithText("Memories & privacy").assertIsDisplayed()
        onNodeWithText("Data").assertIsDisplayed()
        onNodeWithText("Support").assertIsDisplayed()
        onNodeWithText("Duration, energy, and location signals").assertIsDisplayed()
        onNodeWithText("family-plus-slot").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun decorativeGlyphsAndChevronsAreHiddenFromAccessibilityTree() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                FamilyScreen(state = loadedState, onAction = {}, familyPlusSection = { NoFamilyPlusSection() })
            }
        }

        // The row's own title/subtitle carry its accessible label; a bare "⚙" node would mean the
        // glyph leaked into the tree unmerged instead of being cleared as decorative.
        onNodeWithContentDescription("⚙").assertDoesNotExist()
        onNodeWithContentDescription("›").assertDoesNotExist()
    }
}
