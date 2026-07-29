package com.togetherly.feature.explore.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.input.TogetherlyTextField
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_search_clear_content_description
import togetherly.shared.generated.resources.explore_search_label

/**
 * [label] (not a manual `contentDescription` override) is what gives this field a meaningful
 * accessibility name — [TogetherlyTextField]'s underlying Material `OutlinedTextField` already
 * exposes its label as the field's accessible name, so overriding semantics here would fight that
 * default rather than improve it.
 */
@Composable
internal fun ExploreSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = stringResource(Res.string.explore_search_label),
        singleLine = true,
        trailingIcon = if (query.isNotEmpty()) {
            {
                TogetherlyIconButton(
                    icon = { Text(text = "✕", style = MaterialTheme.togetherlyTypography.titleM) },
                    contentDescription = stringResource(Res.string.explore_search_clear_content_description),
                    onClick = onClear,
                )
            }
        } else {
            null
        },
    )
}
