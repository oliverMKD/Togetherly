package com.togetherly.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.color.DarkTogetherlyColors
import com.togetherly.designsystem.color.LightTogetherlyColors
import com.togetherly.designsystem.color.TogetherlyColors
import com.togetherly.designsystem.spacing.TogetherlySpacing
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves every `MaterialTheme.togetherly*` accessor actually resolves once real composition is
 * running under [TogetherlyTheme] — not just that the CompositionLocals compile, which a purely
 * unit-level test could never catch (a forgotten `CompositionLocalProvider` entry still compiles
 * fine; it only breaks at runtime).
 *
 * Instrumented rather than `commonTest`, matching this project's established pattern for anything
 * that needs a real Android runtime (see `docs/architecture.md`'s "Known environment limitations"):
 * [runComposeUiTest] reads `android.os.Build.FINGERPRINT` on Android to decide its idling strategy,
 * which is `null` (no Robolectric shadow, deliberately not configured in this project) under the
 * plain JVM `androidHostTest` target — a real device/emulator, as used here, has a real value. The
 * iOS-target `iosSimulatorArm64Test` run of this same logic (when it briefly lived in `commonTest`)
 * passed without this issue; it moved here anyway so the test exists in exactly one place rather
 * than two near-duplicates per target.
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlyThemeCompositionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessorsResolveToTheLightPaletteInsideComposition() = runComposeUiTest {
        lateinit var colors: TogetherlyColors
        lateinit var spacing: TogetherlySpacing
        var reduceMotion = true

        setContent {
            TogetherlyTheme(darkTheme = false, reduceMotion = false) {
                colors = MaterialTheme.togetherlyColors
                spacing = MaterialTheme.togetherlySpacing
                reduceMotion = MaterialTheme.togetherlyReduceMotion
            }
        }
        waitForIdle()

        assertEquals(LightTogetherlyColors, colors)
        assertEquals(TogetherlySpacing(), spacing)
        assertFalse(reduceMotion)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessorsResolveToTheDarkPaletteInsideComposition() = runComposeUiTest {
        lateinit var colors: TogetherlyColors

        setContent {
            TogetherlyTheme(darkTheme = true) {
                colors = MaterialTheme.togetherlyColors
            }
        }
        waitForIdle()

        assertEquals(DarkTogetherlyColors, colors)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reduceMotionFlagPropagatesThroughComposition() = runComposeUiTest {
        var reduceMotion = false

        setContent {
            TogetherlyTheme(reduceMotion = true) {
                reduceMotion = MaterialTheme.togetherlyReduceMotion
            }
        }
        waitForIdle()

        assertTrue(reduceMotion)
    }
}
