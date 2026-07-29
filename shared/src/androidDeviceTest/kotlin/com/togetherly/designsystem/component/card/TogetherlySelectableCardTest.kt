package com.togetherly.designsystem.component.card

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Confirms selection is exposed through real accessibility semantics (`selected` +
 * [Role.RadioButton]), not only through the visual border/indicator this component also draws —
 * see [TogetherlySelectableCard]'s own KDoc.
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlySelectableCardTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectedCardExposesSelectedSemantics() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlySelectableCard(selected = true, onClick = {}, title = "Option A")
            }
        }

        onNodeWithText("Option A")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unselectedCardExposesNotSelectedSemantics() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlySelectableCard(selected = false, onClick = {}, title = "Option A")
            }
        }

        onNodeWithText("Option A").assertIsNotSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickInvokesCallback() = runComposeUiTest {
        var clicks = 0
        setContent {
            TogetherlyTheme {
                TogetherlySelectableCard(selected = false, onClick = { clicks++ }, title = "Option A")
            }
        }

        onNodeWithText("Option A").performClick()
        waitForIdle()

        assertEquals(1, clicks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun meetsMinimumTouchTarget() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlySelectableCard(selected = false, onClick = {}, title = "Option A")
            }
        }

        onNodeWithText("Option A").assertHeightIsAtLeast(48.dp)
    }
}
