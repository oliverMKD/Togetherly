package com.togetherly.designsystem.color

import com.togetherly.designsystem.color.TogetherlyPrimitives as P

/**
 * A deliberately designed dark palette, not [LightTogetherlyColors] inverted: surfaces step
 * through the [P.charcoal900]→[P.charcoal600] scale (canvas darkest, elevated lightest — the
 * opposite direction of light theme, matching how elevation reads in dark UI generally), and
 * category roles switch to each hue's brighter `400` shade because the `800` shades light theme
 * uses would be nearly invisible against a dark canvas. [error] gets its own `red400` primitive
 * (as opposed to reusing light theme's `red600`) for the same reason — see
 * [P.red400]'s own KDoc.
 *
 * [actionPrimary]/[actionPrimaryContent] intentionally match light theme's values: the brand
 * accent (coral) stays constant across themes, on the same reasoning as [error] above except in
 * the opposite direction — `coral400` already contrasts safely against both a light canvas
 * (as a filled button, dark content) and a dark canvas (as a swatch/text tint) without needing a
 * second variant.
 *
 * Every foreground/background and content/accent pair here is asserted against
 * [WCAG_AA_NORMAL_TEXT] in `DarkTogetherlyColorsTest`.
 */
internal val DarkTogetherlyColors = TogetherlyColors(
    backgroundCanvas = P.charcoal900,
    backgroundSurface = P.charcoal800,
    backgroundElevated = P.charcoal700,
    foregroundPrimary = P.cream100,
    foregroundSecondary = P.slate300,
    foregroundOnAccent = P.charcoal900,
    borderSubtle = P.charcoal600,
    actionPrimary = P.coral400,
    actionPrimaryContent = P.charcoal900,
    actionSecondary = P.charcoal700,
    actionSecondaryContent = P.cream100,
    positive = P.mint400,
    warning = P.yellow400,
    error = P.red400,
    categoryTalk = P.blue400,
    categoryCreate = P.lavender400,
    categoryMove = P.coral400,
    categoryKindness = P.mint400,
    categoryDiscover = P.yellow400,
    categorySilly = P.rose400,
    categoryMemories = P.teal400,
)
