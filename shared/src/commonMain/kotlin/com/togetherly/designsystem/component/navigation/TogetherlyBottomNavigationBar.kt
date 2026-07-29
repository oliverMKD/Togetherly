package com.togetherly.designsystem.component.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyTypography

/**
 * The closed set of top-level destinations the app's bottom navigation can point at. Deliberately
 * an enum here in the design system, not a reference to the app's own navigation-route sealed
 * class: the navigation shell (built later) maps its routes onto these four values, so this
 * component never imports, and is never coupled to, that shell's types.
 */
enum class TogetherlyDestination {
    Today,
    Explore,
    Journey,
    Family,
}

/**
 * One bottom-navigation entry. [label] and [icon] are supplied by the caller rather than looked up
 * from [destination] internally — this keeps the design system out of the business of owning
 * production copy or icon assets (neither exists yet at this step) while still letting
 * [TogetherlyBottomNavigationBar] iterate a strongly-typed, exactly-four-destination list.
 */
@Immutable
data class TogetherlyNavigationItem(
    val destination: TogetherlyDestination,
    val label: String,
    val selected: Boolean,
    val icon: @Composable () -> Unit,
)

/**
 * Renders [items] as a bottom navigation bar. Holds no selection state itself — [onItemSelected]
 * is the only way a click reaches the caller, which owns deciding what "selected" means (current
 * nav-graph destination) and updating [items] accordingly on the next recomposition.
 */
@Composable
fun TogetherlyBottomNavigationBar(
    items: List<TogetherlyNavigationItem>,
    onItemSelected: (TogetherlyDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.togetherlyColors

    NavigationBar(
        modifier = modifier,
        containerColor = colors.backgroundSurface,
        contentColor = colors.foregroundPrimary,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = { onItemSelected(item.destination) },
                icon = item.icon,
                label = {
                    Text(text = item.label, style = MaterialTheme.togetherlyTypography.labelM)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.actionPrimary,
                    selectedTextColor = colors.actionPrimary,
                    indicatorColor = colors.backgroundElevated,
                    unselectedIconColor = colors.foregroundSecondary,
                    unselectedTextColor = colors.foregroundSecondary,
                ),
            )
        }
    }
}
