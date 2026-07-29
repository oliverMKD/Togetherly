package com.togetherly.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing

@Composable
private fun PreviewNavIcon(tint: Color) {
    Box(modifier = Modifier.size(MaterialTheme.togetherlySize.iconM).background(tint, CircleShape))
}

@Composable
private fun TopBarShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
        TogetherlyTopBar(title = "Today")
        TogetherlyTopBar(
            title = "Family settings",
            navigationIcon = {
                TogetherlyIconButton(
                    icon = { PreviewNavIcon(MaterialTheme.togetherlyColors.foregroundPrimary) },
                    contentDescription = "Back",
                    onClick = {},
                )
            },
        )
        TogetherlyTopBar(transparent = true)
    }
}

@Composable
private fun BottomNavShowcase() {
    val items = listOf(
        TogetherlyNavigationItem(
            destination = TogetherlyDestination.Today,
            label = "Today",
            selected = true,
            icon = { PreviewNavIcon(MaterialTheme.togetherlyColors.actionPrimary) },
        ),
        TogetherlyNavigationItem(
            destination = TogetherlyDestination.Explore,
            label = "Explore",
            selected = false,
            icon = { PreviewNavIcon(MaterialTheme.togetherlyColors.foregroundSecondary) },
        ),
        TogetherlyNavigationItem(
            destination = TogetherlyDestination.Journey,
            label = "Journey",
            selected = false,
            icon = { PreviewNavIcon(MaterialTheme.togetherlyColors.foregroundSecondary) },
        ),
        TogetherlyNavigationItem(
            destination = TogetherlyDestination.Family,
            label = "Family",
            selected = false,
            icon = { PreviewNavIcon(MaterialTheme.togetherlyColors.foregroundSecondary) },
        ),
    )
    TogetherlyBottomNavigationBar(items = items, onItemSelected = {})
}

@Preview
@Composable
private fun TopBarShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) {
        Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) { TopBarShowcase() }
    }
}

@Preview
@Composable
private fun TopBarShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) {
        Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) { TopBarShowcase() }
    }
}

@Preview
@Composable
private fun BottomNavShowcaseLightPreview() {
    TogetherlyTheme(darkTheme = false) { BottomNavShowcase() }
}

@Preview
@Composable
private fun BottomNavShowcaseDarkPreview() {
    TogetherlyTheme(darkTheme = true) { BottomNavShowcase() }
}
