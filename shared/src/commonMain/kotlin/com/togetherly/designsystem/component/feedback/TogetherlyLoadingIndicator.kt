package com.togetherly.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_loading

/**
 * A generic "something is loading" indicator — a spinner plus an optional caption. [label]
 * defaults to a design-system-owned "Loading" string (see `strings.xml`'s own note on
 * component-owned vs. feature copy) rather than requiring every call site to pass one for the
 * common case; a caller with more specific context (e.g. "Loading today's quest") can override it.
 */
@Composable
fun TogetherlyLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val loadingLabel = label ?: stringResource(Res.string.ds_component_loading)

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = loadingLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(MaterialTheme.togetherlySize.iconL),
            color = MaterialTheme.togetherlyColors.actionPrimary,
        )
        Text(
            text = loadingLabel,
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}
