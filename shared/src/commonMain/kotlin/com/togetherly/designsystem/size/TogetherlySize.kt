package com.togetherly.designsystem.size

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable, cross-screen size tokens only — never a screen- or component-specific size like
 * `onboardingCardHeight`. If a size is only ever used in one place, it belongs as a local constant
 * in that component, not here.
 *
 * [minimumTouchTarget] is a hard floor, not a suggestion: 48dp is the touch-target minimum both
 * Android's accessibility guidance and WCAG 2.5.5 (Target Size, AAA — treated as a hard
 * requirement here regardless, given the 10-13 age range's still-developing fine motor control)
 * call for. [buttonHeight]/[inputHeight] are defined at-or-above it deliberately.
 */
@Immutable
data class TogetherlySize(
    val minimumTouchTarget: Dp = 48.dp,
    val iconS: Dp = 16.dp,
    val iconM: Dp = 24.dp,
    val iconL: Dp = 32.dp,
    val buttonHeight: Dp = 52.dp,
    val inputHeight: Dp = 52.dp,
    val navigationBarHeight: Dp = 64.dp,
    val contentMaxWidth: Dp = 560.dp,
)

internal val LocalTogetherlySize = staticCompositionLocalOf {
    TogetherlySize()
}
