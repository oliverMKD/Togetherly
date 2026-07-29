package com.togetherly.designsystem.component.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing

/**
 * The background/inset/padding scaffolding every screen shares — deliberately built from [Box]/
 * [Column] rather than [androidx.compose.material3.Scaffold], since `Scaffold` bundles assumptions
 * (its own inset handling, its own slot set) that would get in the way of a screen that wants to be
 * fully edge-to-edge (a hero image behind a transparent [topBar], say) — a future screen composes
 * around this, it doesn't fight it.
 *
 * [topBar] and [bottomBar] are expected to already handle their own window insets when they need
 * to (as [com.togetherly.designsystem.component.navigation.TogetherlyTopBar] and
 * [com.togetherly.designsystem.component.navigation.TogetherlyBottomNavigationBar] do) — when
 * either is absent, [content] itself absorbs that edge's safe-drawing inset instead, so content
 * never renders under a status bar or a gesture nav bar. Horizontal safe-drawing insets (a
 * display cutout in landscape) are always applied to [content].
 *
 * [content] is width-capped at [MaterialTheme.togetherlySize.contentMaxWidth] and centered, so a
 * large window (a tablet, a resized desktop window) doesn't stretch text into an unreadable line
 * length.
 */
@Composable
fun TogetherlyScreen(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    var contentInsetSides = WindowInsetsSides.Horizontal
    if (topBar == null) contentInsetSides += WindowInsetsSides.Top
    if (bottomBar == null) contentInsetSides += WindowInsetsSides.Bottom

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.togetherlyColors.backgroundCanvas,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar?.invoke()

            var contentAreaModifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(contentInsetSides))
            if (scrollable) {
                contentAreaModifier = contentAreaModifier.verticalScroll(rememberScrollState())
            }

            Box(modifier = contentAreaModifier, contentAlignment = Alignment.TopCenter) {
                Box(
                    modifier = Modifier
                        .widthIn(max = MaterialTheme.togetherlySize.contentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.togetherlySpacing.m),
                ) {
                    content()
                }
            }

            bottomBar?.invoke()
        }
    }
}
