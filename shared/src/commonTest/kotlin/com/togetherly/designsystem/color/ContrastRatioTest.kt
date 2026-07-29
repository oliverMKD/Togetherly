package com.togetherly.designsystem.color

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastRatioTest {

    @Test
    fun blackOnWhiteIsMaximumContrast() {
        assertApproximately(21.0, contrastRatio(Color.Black, Color.White), tolerance = 0.01)
    }

    @Test
    fun identicalColorsHaveNoContrast() {
        assertApproximately(1.0, contrastRatio(Color(0xFF6750A4), Color(0xFF6750A4)), tolerance = 0.001)
    }

    @Test
    fun contrastRatioIsOrderIndependent() {
        val a = Color(0xFF252331)
        val b = Color(0xFFFFF8ED)
        assertApproximately(contrastRatio(a, b), contrastRatio(b, a), tolerance = 0.0001)
    }

    @Test
    fun knownAccessiblePairMeetsAaNormalText() {
        // Charcoal 900 on Cream 50 — Togetherly's own primary foreground/background pair.
        val ratio = contrastRatio(Color(0xFF252331), Color(0xFFFFF8ED))
        assertTrue(ratio >= WCAG_AA_NORMAL_TEXT, "Expected >= $WCAG_AA_NORMAL_TEXT, was $ratio")
    }

    @Test
    fun knownInaccessiblePairFailsAaNormalText() {
        // White text on the brand's own bright coral — the exact pairing this token set avoids
        // (actionPrimaryContent is charcoal, not white, on actionPrimary).
        val ratio = contrastRatio(Color.White, Color(0xFFF27D72))
        assertTrue(ratio < WCAG_AA_NORMAL_TEXT, "Expected < $WCAG_AA_NORMAL_TEXT, was $ratio")
    }

    private fun assertApproximately(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "Expected $expected +/- $tolerance, was $actual",
        )
    }
}
