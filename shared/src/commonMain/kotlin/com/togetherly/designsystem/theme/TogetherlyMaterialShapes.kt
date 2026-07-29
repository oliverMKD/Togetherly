package com.togetherly.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.togetherly.designsystem.shape.TogetherlyCornerRadii

/**
 * Maps our corner radii onto Material 3's five-slot [Shapes] (which needs
 * [androidx.compose.foundation.shape.CornerBasedShape] specifically, not the general
 * [androidx.compose.ui.graphics.Shape] [com.togetherly.designsystem.shape.TogetherlyShapes]'s
 * public fields use — see [TogetherlyCornerRadii]'s own KDoc) for stock Material components.
 * [com.togetherly.designsystem.shape.TogetherlyShapes.pill]/`.circular` have no Material slot to
 * map to — they're for call sites that reach for `MaterialTheme.togetherlyShapes` directly, e.g. a
 * tag or avatar.
 */
internal fun toMaterialShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(TogetherlyCornerRadii.small),
    small = RoundedCornerShape(TogetherlyCornerRadii.small),
    medium = RoundedCornerShape(TogetherlyCornerRadii.medium),
    large = RoundedCornerShape(TogetherlyCornerRadii.large),
    extraLarge = RoundedCornerShape(TogetherlyCornerRadii.card),
)
