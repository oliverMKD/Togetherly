package com.togetherly.designsystem.motion

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Duration tokens only — no easing curves, specs, or actual animated composables yet (that's a
 * later step; see this package's own scope note in `TogetherlyTheme`'s own KDoc). Every duration
 * is in milliseconds, matching `AnimationSpec`'s `durationMillis` so a future animation call site
 * can use these directly without a unit conversion.
 *
 * [celebration] is intentionally the longest and named for its purpose (a quest-completion
 * moment), not for what it looks like — the animation itself doesn't exist yet, only the budget
 * for how long it's allowed to take. [reveal] is the Today Mystery→Revealed transition's own
 * budget (Step 8.4) — a distinct token from [celebration] since a reveal is not a completion
 * moment, even though their durations happen to be close.
 */
@Immutable
data class TogetherlyMotion(
    val instant: Int = 0,
    val fast: Int = 120,
    val standard: Int = 200,
    val slow: Int = 320,
    val reveal: Int = 500,
    val celebration: Int = 600,
)

internal val LocalTogetherlyMotion = staticCompositionLocalOf {
    TogetherlyMotion()
}

/**
 * The shared reduced-motion decision seam: every future animated composable should resolve its
 * duration through this instead of reading a [TogetherlyMotion] value directly, so "the user (or
 * OS) asked for reduced motion" only ever needs to be honored in one place. [TogetherlyTheme][com.togetherly.designsystem.theme.TogetherlyTheme]'s
 * own `reduceMotion` parameter is what ultimately sets this — see its KDoc for why there is no
 * automatic OS-level detection wired in yet.
 */
internal val LocalReduceMotion = staticCompositionLocalOf { false }

/** Resolves [duration] against the reduced-motion decision: `0` (instant) when reduced motion is active, [duration] otherwise. */
internal fun resolveMotionDuration(duration: Int, reduceMotion: Boolean): Int =
    if (reduceMotion) 0 else duration
