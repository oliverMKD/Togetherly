package com.togetherly.designsystem.color

import androidx.compose.ui.graphics.Color

/**
 * Raw, context-free palette values. Nothing here carries meaning — [TogetherlyPrimitives] never
 * says what a color is *for*, only what it *is*. Feature UI must never reference these directly;
 * go through [TogetherlyColors][com.togetherly.designsystem.color.TogetherlyColors] (via
 * `MaterialTheme.togetherlyColors`) instead, so a future palette adjustment only ever touches
 * [LightTogetherlyColors]/[DarkTogetherlyColors], never call sites. `internal` visibility is the
 * only enforcement Kotlin gives us for that inside a single module — treat it as a hard rule
 * anyway.
 *
 * Two hue families extend the brief's suggested primitives ([teal400]/[teal800],
 * [rose400]/[rose800]) purely so all seven [TogetherlyColors] category roles can be distinct,
 * recognizable hues rather than two categories sharing a hue at different shades. [red400] is
 * likewise an addition: [red600] was tuned for dark text on a light surface and fails contrast
 * as a light-on-dark error color, so dark theme needs its own lighter red.
 */
internal object TogetherlyPrimitives {

    val cream50 = Color(0xFFFFF8ED)
    val cream100 = Color(0xFFF7EEDF)
    val white = Color(0xFFFFFFFF)

    // A charcoal *scale*, darkest to lightest, gives dark theme deliberately distinct surface
    // tones instead of one dark color reused everywhere or a literal light-scale inversion.
    val charcoal900 = Color(0xFF252331)
    val charcoal800 = Color(0xFF2E2B3B)
    val charcoal700 = Color(0xFF38344A)
    val charcoal600 = Color(0xFF46415A)

    val slate600 = Color(0xFF666271)
    val slate300 = Color(0xFFA9A3B8)
    val warmGray200 = Color(0xFFE7DED1)

    val coral400 = Color(0xFFF27D72)
    val coral700 = Color(0xFFA83F39)

    val mint400 = Color(0xFF8ED9B6)
    val mint800 = Color(0xFF276448)

    val yellow400 = Color(0xFFF4CB67)
    val yellow800 = Color(0xFF6B5000)

    val lavender400 = Color(0xFFB9A7E8)
    val lavender800 = Color(0xFF4F3B7C)

    val blue400 = Color(0xFF8DBDE8)
    val blue800 = Color(0xFF294F75)

    val teal400 = Color(0xFF7FD1C7)
    val teal800 = Color(0xFF1D5C55)

    val rose400 = Color(0xFFE89CB5)
    val rose800 = Color(0xFF7A2F49)

    val red400 = Color(0xFFF0958D)
    val red600 = Color(0xFFB83A3A)
}
