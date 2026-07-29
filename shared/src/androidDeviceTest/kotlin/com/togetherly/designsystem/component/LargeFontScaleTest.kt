package com.togetherly.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * At a large system font-scale setting (a real, common case for this app's grandparent users —
 * see [com.togetherly.designsystem.typography.TogetherlyTypography]'s own KDoc), a screen's
 * primary action must stay visible and tappable rather than being pushed off-screen or clipped.
 * [TogetherlyPrimaryButton] stands in for "an essential action" here.
 */
@RunWith(AndroidJUnit4::class)
internal class LargeFontScaleTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun primaryButtonStaysVisibleAndClickableAtLargeFontScale() = runComposeUiTest {
        var clicks = 0
        setContent {
            val doubledDensity = Density(
                density = LocalDensity.current.density,
                fontScale = 2f,
            )
            CompositionLocalProvider(LocalDensity provides doubledDensity) {
                TogetherlyTheme {
                    TogetherlyPrimaryButton(label = "Continue", onClick = { clicks++ })
                }
            }
        }

        onNodeWithText("Continue").performClick()
        waitForIdle()

        assertEquals(1, clicks)
    }
}
