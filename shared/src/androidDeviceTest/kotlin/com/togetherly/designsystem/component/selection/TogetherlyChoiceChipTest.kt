package com.togetherly.designsystem.component.selection

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class TogetherlyChoiceChipTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectedChipExposesSelectedSemantics() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyChoiceChip(label = "Talk", selected = true, onClick = {})
            }
        }

        onNodeWithText("Talk").assertIsSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unselectedChipExposesNotSelectedSemantics() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyChoiceChip(label = "Talk", selected = false, onClick = {})
            }
        }

        onNodeWithText("Talk").assertIsNotSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickInvokesCallback() = runComposeUiTest {
        var clicks = 0
        setContent {
            TogetherlyTheme {
                TogetherlyChoiceChip(label = "Talk", selected = false, onClick = { clicks++ })
            }
        }

        onNodeWithText("Talk").performClick()
        waitForIdle()

        assertEquals(1, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun touchTargetMeetsMinimumEvenThoughVisualChipIsCompact() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyChoiceChip(label = "Talk", selected = false, onClick = {})
            }
        }

        onNodeWithText("Talk")
            .assertWidthIsAtLeast(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
    }
}
