package com.togetherly.designsystem.typography

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Togetherly's type roles, each a purpose ("the largest hero moment on a screen") rather than a
 * raw size — feature code reads `MaterialTheme.togetherlyTypography.titleL`, never a hardcoded
 * `22.sp`, so a future global type-scale adjustment is one file, not a grep-and-replace.
 *
 * Every [TextStyle] uses [sp] for both size and line height (never [androidx.compose.ui.unit.dp]),
 * so every role scales with the platform's system font-size/accessibility setting rather than
 * staying visually fixed while surrounding system UI grows — non-negotiable for a family app
 * where a grandparent's larger system font setting is a real, common case to support.
 *
 * Line heights sit around 1.3–1.45x the font size throughout (looser than tight display type,
 * tighter than a novel) — comfortable for the 10-13 age range's still-developing reading fluency
 * without feeling babyish or airy. No [androidx.compose.ui.text.font.FontFamily] is set: system
 * fonts only for now (see this class's own package KDoc) — a future licensed font only ever
 * changes the handful of `fontFamily` args here, never call sites.
 */
@Immutable
data class TogetherlyTypography(
    val displayL: TextStyle,
    val displayM: TextStyle,
    val headlineL: TextStyle,
    val headlineM: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
)

internal val DefaultTogetherlyTypography = TogetherlyTypography(
    displayL = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    displayM = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineL = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineM = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleL = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleM = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyL = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyM = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyS = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelL = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelM = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
)

internal val LocalTogetherlyTypography = staticCompositionLocalOf {
    DefaultTogetherlyTypography
}
