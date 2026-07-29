package com.togetherly.designsystem.component.button

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Covers the click/disabled/loading contract shared by every label-based Togetherly button — see
 * [TogetherlyPrimaryButton]'s own KDoc for why loading blocks clicks structurally via `enabled`
 * rather than an ad-hoc debounce. [TogetherlyPrimaryButton] stands in for the family here since
 * [TogetherlySecondaryButton]/[TogetherlyTextButton] share the same [TogetherlyButtonContent] and
 * enable/loading wiring.
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlyButtonTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickInvokesCallbackWhenEnabled() = runComposeUiTest {
        var clicks = 0
        setContent {
            TogetherlyTheme {
                TogetherlyPrimaryButton(label = "Continue", onClick = { clicks++ })
            }
        }

        onNodeWithText("Continue").performClick()
        waitForIdle()

        assertEquals(1, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun disabledButtonIgnoresClicksAndIsMarkedNotEnabled() = runComposeUiTest {
        var clicks = 0
        setContent {
            TogetherlyTheme {
                TogetherlyPrimaryButton(label = "Continue", onClick = { clicks++ }, enabled = false)
            }
        }

        onNodeWithText("Continue")
            .assertIsNotEnabled()
            .performClick()
        waitForIdle()

        assertEquals(0, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadingButtonPreventsRepeatedClicksButKeepsLabelVisible() = runComposeUiTest {
        var clicks = 0
        setContent {
            TogetherlyTheme {
                TogetherlyPrimaryButton(label = "Continue", onClick = { clicks++ }, loading = true)
            }
        }

        val button = onNodeWithText("Continue")
        button.performClick()
        button.performClick()
        button.performClick()
        waitForIdle()

        assertEquals(0, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun iconButtonRequiresContentDescriptionAndMeetsMinimumTouchTarget() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyIconButton(icon = {}, contentDescription = "Close", onClick = {})
            }
        }

        onNodeWithContentDescription("Close")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun primaryButtonMeetsMinimumTouchTarget() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyPrimaryButton(label = "Continue", onClick = {})
            }
        }

        onNodeWithText("Continue")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
