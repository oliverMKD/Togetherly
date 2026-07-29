package com.togetherly.designsystem.color

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Maps [TogetherlyColors] onto Material 3's [ColorScheme] so Material components (which read
 * `MaterialTheme.colorScheme`, not our tokens) still look on-brand. This is deliberately a lossy,
 * best-effort mapping, not a 1:1 rename: Material's ~26 fixed slots have no room for
 * Togetherly-specific concepts like the seven category colors, and forcing every product role
 * into a Material slot (e.g. jamming `categoryTalk` into `tertiary`) would make that slot's
 * meaning unpredictable wherever a stock Material component uses it. Product code should prefer
 * `MaterialTheme.togetherlyColors` for anything not genuinely about a Material component's own
 * chrome (buttons, text fields, dialogs); this mapping exists for those stock components, not as
 * a second way to reach the same tokens.
 */
internal fun TogetherlyColors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    // categoryDiscover and error each invert from a deep shade (light theme, needs light content)
    // to a bright one (dark theme, needs dark content) between palettes — unlike actionPrimary,
    // whose color stays constant across themes, so actionPrimaryContent needs no such flip.
    val contentOnInvertedAccent = if (darkTheme) foregroundOnAccent else backgroundSurface

    return if (darkTheme) {
        darkColorScheme(
            primary = actionPrimary,
            onPrimary = actionPrimaryContent,
            secondary = actionSecondary,
            onSecondary = actionSecondaryContent,
            tertiary = categoryDiscover,
            onTertiary = contentOnInvertedAccent,
            background = backgroundCanvas,
            onBackground = foregroundPrimary,
            surface = backgroundSurface,
            onSurface = foregroundPrimary,
            surfaceVariant = backgroundElevated,
            onSurfaceVariant = foregroundSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle,
            error = error,
            onError = contentOnInvertedAccent,
        )
    } else {
        lightColorScheme(
            primary = actionPrimary,
            onPrimary = actionPrimaryContent,
            secondary = actionSecondary,
            onSecondary = actionSecondaryContent,
            tertiary = categoryDiscover,
            onTertiary = contentOnInvertedAccent,
            background = backgroundCanvas,
            onBackground = foregroundPrimary,
            surface = backgroundSurface,
            onSurface = foregroundPrimary,
            surfaceVariant = backgroundElevated,
            onSurfaceVariant = foregroundSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle,
            error = error,
            onError = contentOnInvertedAccent,
        )
    }
}
