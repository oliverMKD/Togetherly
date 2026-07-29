package com.togetherly.designsystem.size

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

class TogetherlySizeTest {

    @Test
    fun minimumTouchTargetMeetsAccessibilityFloor() {
        assertTrue(TogetherlySize().minimumTouchTarget >= 48.dp)
    }

    @Test
    fun interactiveHeightsMeetTheMinimumTouchTarget() {
        val size = TogetherlySize()
        assertTrue(size.buttonHeight >= size.minimumTouchTarget)
        assertTrue(size.inputHeight >= size.minimumTouchTarget)
    }

    @Test
    fun iconSizesIncreaseMonotonically() {
        val size = TogetherlySize()
        assertTrue(size.iconS < size.iconM)
        assertTrue(size.iconM < size.iconL)
    }
}
