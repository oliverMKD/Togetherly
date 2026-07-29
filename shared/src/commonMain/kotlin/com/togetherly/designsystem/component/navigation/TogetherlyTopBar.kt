package com.togetherly.designsystem.component.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyTypography

/**
 * A screen's top chrome. Built on Material's own [TopAppBar] so its window-inset handling (status
 * bar padding, per platform) is reused rather than reimplemented — this wrapper's own job is
 * mapping [title]/[navigationIcon]/[actions] onto Togetherly's tokens and offering [transparent]
 * for screens that want their content to show through (e.g. a hero image behind the bar) instead
 * of a solid surface color.
 *
 * Owns no navigation: [navigationIcon] is a caller-supplied slot (back arrow, close, none) so this
 * component never assumes what "back" means for a given screen or reaches into a nav controller
 * itself.
 */
@Composable
fun TogetherlyTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    transparent: Boolean = false,
) {
    val colors = MaterialTheme.togetherlyColors
    val containerColor = if (transparent) Color.Transparent else colors.backgroundSurface

    TopAppBar(
        modifier = modifier,
        title = {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.togetherlyTypography.titleL,
                    color = colors.foregroundPrimary,
                )
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = colors.foregroundPrimary,
            navigationIconContentColor = colors.foregroundPrimary,
            actionIconContentColor = colors.foregroundPrimary,
        ),
    )
}
