package com.togetherly.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.togetherly.designsystem.color.DarkTogetherlyColors
import com.togetherly.designsystem.color.LightTogetherlyColors
import com.togetherly.designsystem.color.LocalTogetherlyColors
import com.togetherly.designsystem.color.TogetherlyColors
import com.togetherly.designsystem.color.toMaterialColorScheme
import com.togetherly.designsystem.motion.LocalReduceMotion
import com.togetherly.designsystem.motion.LocalTogetherlyMotion
import com.togetherly.designsystem.motion.TogetherlyMotion
import com.togetherly.designsystem.shape.DefaultTogetherlyShapes
import com.togetherly.designsystem.shape.LocalTogetherlyShapes
import com.togetherly.designsystem.shape.TogetherlyShapes
import com.togetherly.designsystem.size.LocalTogetherlySize
import com.togetherly.designsystem.size.TogetherlySize
import com.togetherly.designsystem.spacing.LocalTogetherlySpacing
import com.togetherly.designsystem.spacing.TogetherlySpacing
import com.togetherly.designsystem.typography.DefaultTogetherlyTypography
import com.togetherly.designsystem.typography.LocalTogetherlyTypography
import com.togetherly.designsystem.typography.TogetherlyTypography
import com.togetherly.designsystem.typography.toMaterialTypography

/**
 * Togetherly's single design-system entry point. Both the Android and iOS apps render the same
 * shared `App()` composable (see `com.togetherly.app.application.App`), which wraps its content in
 * this — there is no separate per-platform theme.
 *
 * Configures, from one place: semantic colors ([TogetherlyColors], light or dark depending on
 * [darkTheme]), Material's own [MaterialTheme.colorScheme] (a derived mapping — see
 * [toMaterialColorScheme]), [TogetherlyTypography] (and Material's [MaterialTheme.typography]),
 * [TogetherlyShapes] (and Material's [MaterialTheme.shapes]), [TogetherlySpacing], [TogetherlySize],
 * and [TogetherlyMotion] — plus the [reduceMotion] decision every future animated composable
 * should consult (see [LocalReduceMotion]'s own KDoc) instead of reading motion durations raw.
 *
 * [reduceMotion] defaults to `false` rather than reading a platform "prefers reduced motion"
 * signal automatically: this step establishes the seam (the parameter, and
 * [LocalReduceMotion] for consumers), not the platform-specific `expect`/`actual` detection behind
 * it — see this project's own step history for why that's deliberately out of scope here. Until
 * that detection exists, a caller that already knows the platform signal (e.g. from a future
 * `expect`/`actual` accessibility service) can pass it straight through.
 */
@Composable
fun TogetherlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val togetherlyColors = if (darkTheme) DarkTogetherlyColors else LightTogetherlyColors
    val materialColorScheme = togetherlyColors.toMaterialColorScheme(darkTheme)
    val materialTypography = DefaultTogetherlyTypography.toMaterialTypography()
    val materialShapes = toMaterialShapes()

    CompositionLocalProvider(
        LocalTogetherlyColors provides togetherlyColors,
        LocalTogetherlyTypography provides DefaultTogetherlyTypography,
        LocalTogetherlySpacing provides TogetherlySpacing(),
        LocalTogetherlySize provides TogetherlySize(),
        LocalTogetherlyShapes provides DefaultTogetherlyShapes,
        LocalTogetherlyMotion provides TogetherlyMotion(),
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            shapes = materialShapes,
            content = content,
        )
    }
}

/** Togetherly's semantic color roles for the current theme — see [TogetherlyColors]'s own KDoc. */
val MaterialTheme.togetherlyColors: TogetherlyColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlyColors.current

/** Togetherly's type roles for the current theme — see [TogetherlyTypography]'s own KDoc. */
val MaterialTheme.togetherlyTypography: TogetherlyTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlyTypography.current

/** Togetherly's 4/8dp spacing rhythm — see [TogetherlySpacing]'s own KDoc. */
val MaterialTheme.togetherlySpacing: TogetherlySpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlySpacing.current

/** Togetherly's reusable size tokens — see [TogetherlySize]'s own KDoc. */
val MaterialTheme.togetherlySize: TogetherlySize
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlySize.current

/** Togetherly's shape tokens — see [TogetherlyShapes]'s own KDoc. */
val MaterialTheme.togetherlyShapes: TogetherlyShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlyShapes.current

/** Togetherly's motion duration tokens — see [TogetherlyMotion]'s own KDoc. */
val MaterialTheme.togetherlyMotion: TogetherlyMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalTogetherlyMotion.current

/** The current reduced-motion decision — see [LocalReduceMotion]'s own KDoc. */
val MaterialTheme.togetherlyReduceMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReduceMotion.current
