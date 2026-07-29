package com.togetherly.designsystem.color

import com.togetherly.designsystem.color.TogetherlyPrimitives as P

/**
 * Warm cream canvas with charcoal ink — deliberately not stark white-on-black: the goal is a
 * cozy, storybook feel, not a clinical/corporate one. Category roles use each hue's `800` (deep,
 * muted) shade rather than its brighter `400` — legible directly as text/icon color on
 * [backgroundCanvas] *and* as a filled chip background under white content, and a deliberate step
 * away from primary-color-toy-bin brightness that would read as a toddler app.
 *
 * Every foreground/background and content/accent pair here is asserted against
 * [WCAG_AA_NORMAL_TEXT] in `LightTogetherlyColorsTest` — this file is not the source of truth for
 * "is this accessible," that test is.
 */
internal val LightTogetherlyColors = TogetherlyColors(
    backgroundCanvas = P.cream50,
    backgroundSurface = P.white,
    backgroundElevated = P.cream100,
    foregroundPrimary = P.charcoal900,
    foregroundSecondary = P.slate600,
    foregroundOnAccent = P.charcoal900,
    borderSubtle = P.warmGray200,
    actionPrimary = P.coral400,
    actionPrimaryContent = P.charcoal900,
    actionSecondary = P.warmGray200,
    actionSecondaryContent = P.charcoal900,
    positive = P.mint800,
    warning = P.yellow800,
    error = P.red600,
    categoryTalk = P.blue800,
    categoryCreate = P.lavender800,
    categoryMove = P.coral700,
    categoryKindness = P.mint800,
    categoryDiscover = P.yellow800,
    categorySilly = P.rose800,
    categoryMemories = P.teal800,
)
