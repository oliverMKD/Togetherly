package com.togetherly.designsystem.color

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.togetherly.designsystem.color.TogetherlyPrimitives as P

class TogetherlyColorsTest {

    @Test
    fun lightPaletteProvidesAllTokens() {
        assertEquals(P.cream50, LightTogetherlyColors.backgroundCanvas)
        assertEquals(P.white, LightTogetherlyColors.backgroundSurface)
        assertEquals(P.cream100, LightTogetherlyColors.backgroundElevated)
        assertEquals(P.charcoal900, LightTogetherlyColors.foregroundPrimary)
    }

    @Test
    fun darkPaletteProvidesAllTokens() {
        assertEquals(P.charcoal900, DarkTogetherlyColors.backgroundCanvas)
        assertEquals(P.charcoal800, DarkTogetherlyColors.backgroundSurface)
        assertEquals(P.charcoal700, DarkTogetherlyColors.backgroundElevated)
        assertEquals(P.cream100, DarkTogetherlyColors.foregroundPrimary)
    }

    @Test
    fun lightAndDarkPalettesAreDistinct() {
        assertTrue(LightTogetherlyColors != DarkTogetherlyColors)
    }

    @Test
    fun lightCategoryRolesAreAllDistinct() {
        assertAllDistinct(LightTogetherlyColors.categoryRoles)
    }

    @Test
    fun darkCategoryRolesAreAllDistinct() {
        assertAllDistinct(DarkTogetherlyColors.categoryRoles)
    }

    private fun assertAllDistinct(colors: List<Color>) {
        assertEquals(7, colors.size, "Expected exactly 7 category roles")
        assertEquals(colors.size, colors.toSet().size, "Category roles must all be visually distinct: $colors")
    }

    /**
     * The contrast-check utility ([contrastRatio]) exercised against every foreground/background
     * and content/accent pair [TogetherlyColors] actually defines — not a hand-picked few. A
     * failure here means an inaccessible pairing shipped, not a hypothetical one.
     */
    @Test
    fun lightPairsMeetWcagAaContrast() {
        assertNoContrastFailures(pairsFor(LightTogetherlyColors))
    }

    @Test
    fun darkPairsMeetWcagAaContrast() {
        assertNoContrastFailures(pairsFor(DarkTogetherlyColors))
    }

    private fun pairsFor(colors: TogetherlyColors): List<Triple<String, Color, Color>> = buildList {
        add(Triple("foregroundPrimary on backgroundCanvas", colors.foregroundPrimary, colors.backgroundCanvas))
        add(Triple("foregroundPrimary on backgroundSurface", colors.foregroundPrimary, colors.backgroundSurface))
        add(Triple("foregroundPrimary on backgroundElevated", colors.foregroundPrimary, colors.backgroundElevated))
        add(Triple("foregroundSecondary on backgroundCanvas", colors.foregroundSecondary, colors.backgroundCanvas))
        add(Triple("actionPrimaryContent on actionPrimary", colors.actionPrimaryContent, colors.actionPrimary))
        add(Triple("actionSecondaryContent on actionSecondary", colors.actionSecondaryContent, colors.actionSecondary))
        add(Triple("positive on backgroundCanvas", colors.positive, colors.backgroundCanvas))
        add(Triple("warning on backgroundCanvas", colors.warning, colors.backgroundCanvas))
        add(Triple("error on backgroundCanvas", colors.error, colors.backgroundCanvas))
        colors.categoryRoles.forEachIndexed { index, category ->
            add(Triple("categoryRoles[$index] on backgroundCanvas", category, colors.backgroundCanvas))
        }
    }

    private fun assertNoContrastFailures(pairs: List<Triple<String, Color, Color>>) {
        val failures = pairs
            .map { (label, foreground, background) -> label to contrastRatio(foreground, background) }
            .filter { (_, ratio) -> ratio < WCAG_AA_NORMAL_TEXT }
            .map { (label, ratio) -> "$label: $ratio (needs >= $WCAG_AA_NORMAL_TEXT)" }

        assertTrue(failures.isEmpty(), "WCAG AA contrast failures:\n${failures.joinToString("\n")}")
    }
}
