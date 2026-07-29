package com.togetherly.designsystem.component.progress

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
internal class TogetherlyStepProgressTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exposesStepXOfYContentDescription() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyStepProgress(currentStep = 2, totalSteps = 4)
            }
        }

        onNodeWithContentDescription("Step 2 of 4").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rejectsCurrentStepOutOfRange() = runComposeUiTest {
        assertFailsWith<IllegalArgumentException> {
            setContent {
                TogetherlyTheme {
                    TogetherlyStepProgress(currentStep = 5, totalSteps = 3)
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rejectsZeroTotalSteps() = runComposeUiTest {
        assertFailsWith<IllegalArgumentException> {
            setContent {
                TogetherlyTheme {
                    TogetherlyStepProgress(currentStep = 1, totalSteps = 0)
                }
            }
        }
    }
}
