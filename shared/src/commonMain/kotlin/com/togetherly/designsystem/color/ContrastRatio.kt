package com.togetherly.designsystem.color

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x minimum contrast ratio for normal-weight body text against its background. */
internal const val WCAG_AA_NORMAL_TEXT = 4.5

/** WCAG 2.x minimum contrast ratio for large-scale text (≥18pt, or ≥14pt bold) and graphical UI components. */
internal const val WCAG_AA_LARGE_TEXT = 3.0

/**
 * The WCAG 2.x contrast ratio between two colors, order-independent — 1.0 (no contrast) to 21.0
 * (black on white). Formula per [W3C's definition](https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio):
 * `(L1 + 0.05) / (L2 + 0.05)`, where `L1` is the lighter color's relative luminance.
 */
internal fun contrastRatio(a: Color, b: Color): Double {
    val luminanceA = a.relativeLuminance()
    val luminanceB = b.relativeLuminance()
    val lighter = max(luminanceA, luminanceB)
    val darker = min(luminanceA, luminanceB)
    return (lighter + 0.05) / (darker + 0.05)
}

/** Per WCAG's `sRGB` relative luminance definition — gamma-expands each channel before weighting it. */
private fun Color.relativeLuminance(): Double =
    0.2126 * red.toDouble().linearize() + 0.7152 * green.toDouble().linearize() + 0.0722 * blue.toDouble().linearize()

private fun Double.linearize(): Double =
    if (this <= 0.03928) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)
