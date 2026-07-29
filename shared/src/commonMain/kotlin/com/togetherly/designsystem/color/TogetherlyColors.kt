package com.togetherly.designsystem.color

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Togetherly's semantic color roles — what each color is *for*, never what hue it happens to be.
 * Every value here is a [TogetherlyPrimitives] reference (see [LightTogetherlyColors]/
 * [DarkTogetherlyColors]); feature UI reads this class (via `MaterialTheme.togetherlyColors`),
 * never [TogetherlyPrimitives] directly.
 *
 * Roles are named for their *purpose*, not for a screen or feature — `actionPrimary`, not
 * `onboardingButtonColor`. A screen-specific color need is a sign a new semantic role belongs
 * here, not that an existing role should be repurposed or a primitive reached for directly.
 *
 * The seven `category*` roles back the quest-catalogue's [categories][com.togetherly.domain.quest.QuestCategory]
 * (talk, create, move, kindness, discover, silly, memories) — one recognizably distinct hue each,
 * verified in [LightTogetherlyColorsTest]/[DarkTogetherlyColorsTest].
 */
@Immutable
data class TogetherlyColors(
    val backgroundCanvas: Color,
    val backgroundSurface: Color,
    val backgroundElevated: Color,
    val foregroundPrimary: Color,
    val foregroundSecondary: Color,
    val foregroundOnAccent: Color,
    val borderSubtle: Color,
    val actionPrimary: Color,
    val actionPrimaryContent: Color,
    val actionSecondary: Color,
    val actionSecondaryContent: Color,
    val positive: Color,
    val warning: Color,
    val error: Color,
    val categoryTalk: Color,
    val categoryCreate: Color,
    val categoryMove: Color,
    val categoryKindness: Color,
    val categoryDiscover: Color,
    val categorySilly: Color,
    val categoryMemories: Color,
) {
    /** Every category role, for exhaustive checks (uniqueness, contrast) without listing all seven by hand. */
    val categoryRoles: List<Color>
        get() = listOf(
            categoryTalk,
            categoryCreate,
            categoryMove,
            categoryKindness,
            categoryDiscover,
            categorySilly,
            categoryMemories,
        )
}

/**
 * No default instance: resolving this outside [com.togetherly.designsystem.theme.TogetherlyTheme]
 * is a real bug (an un-themed color silently leaking into UI), not a case to paper over with a
 * fallback palette.
 */
internal val LocalTogetherlyColors = staticCompositionLocalOf<TogetherlyColors> {
    error("No TogetherlyColors provided. Wrap this content in TogetherlyTheme { ... }.")
}
