package com.togetherly.designsystem.shape

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Rounded with restraint: corners soften Togetherly's look without tipping into the
 * every-corner-a-bubble style of a toddler app. [pill] and [circular] are reserved for shapes that
 * are genuinely a pill/circle (a tag, an avatar) — not a substitute for [large] on things that are
 * just "very rounded rectangles."
 */
@Immutable
data class TogetherlyShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val card: Shape,
    val pill: Shape,
    val circular: Shape,
)

/**
 * The corner radii backing [small]/[medium]/[large]/[card], exposed separately (rather than only
 * as the [Shape]-typed values above) because Material 3's own [androidx.compose.material3.Shapes]
 * needs [androidx.compose.foundation.shape.CornerBasedShape] specifically — a type
 * [DefaultTogetherlyShapes]'s properties can't carry once widened to the [Shape] the public token
 * type calls for. See `TogetherlyMaterialShapes.kt`'s own KDoc.
 */
internal object TogetherlyCornerRadii {
    val small = 8.dp
    val medium = 12.dp
    val large = 20.dp
    val card = 16.dp
}

internal val DefaultTogetherlyShapes = TogetherlyShapes(
    small = RoundedCornerShape(TogetherlyCornerRadii.small),
    medium = RoundedCornerShape(TogetherlyCornerRadii.medium),
    large = RoundedCornerShape(TogetherlyCornerRadii.large),
    card = RoundedCornerShape(TogetherlyCornerRadii.card),
    pill = RoundedCornerShape(percent = 50),
    circular = CircleShape,
)

internal val LocalTogetherlyShapes = staticCompositionLocalOf {
    DefaultTogetherlyShapes
}
