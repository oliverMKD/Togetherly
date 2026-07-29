package com.togetherly.designsystem.typography

import androidx.compose.material3.Typography

/**
 * Maps our 11 roles onto Material 3's 15-slot [Typography] so stock Material components (which
 * read `MaterialTheme.typography`, not our tokens) still use Togetherly type. Material has four
 * slots we have no dedicated role for (`displaySmall`, `headlineSmall`, `titleSmall`,
 * `labelSmall`); each borrows the nearest role below it rather than inventing a 12th product role
 * with no other purpose than filling a Material slot.
 */
internal fun TogetherlyTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayL,
    displayMedium = displayM,
    displaySmall = headlineL,
    headlineLarge = headlineL,
    headlineMedium = headlineM,
    headlineSmall = titleL,
    titleLarge = titleL,
    titleMedium = titleM,
    titleSmall = bodyL,
    bodyLarge = bodyL,
    bodyMedium = bodyM,
    bodySmall = bodyS,
    labelLarge = labelL,
    labelMedium = labelM,
    labelSmall = labelM,
)
