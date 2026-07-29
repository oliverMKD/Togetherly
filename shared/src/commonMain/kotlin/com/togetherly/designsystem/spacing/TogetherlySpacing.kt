package com.togetherly.designsystem.spacing

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A consistent 4/8dp rhythm — every step is a multiple of 4dp, and the common steps ([xs], [m],
 * [l]) land on 8dp multiples. Feature code reads `MaterialTheme.togetherlySpacing.m`, never a
 * hardcoded `16.dp`, so the whole app's density can shift from one file.
 */
@Immutable
data class TogetherlySpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

internal val LocalTogetherlySpacing = staticCompositionLocalOf {
    TogetherlySpacing()
}
