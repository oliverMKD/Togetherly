package com.togetherly.designsystem.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_retry

/**
 * A small, inline "this failed" state — never a full-screen error page (that's a future
 * composition of this plus [com.togetherly.designsystem.component.layout.TogetherlyScreen], not
 * something this component does itself). [message] must already be a user-safe, translated string
 * — this component has no `Throwable`/exception parameter, so there's no way to accidentally
 * surface a stack trace or raw exception message here; the feature layer is responsible for
 * mapping its error into that string before calling this.
 */
@Composable
fun TogetherlyInlineError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { error(message) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(
            text = message,
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.error,
        )
        if (onRetry != null) {
            TogetherlyTextButton(label = stringResource(Res.string.ds_component_retry), onClick = onRetry)
        }
    }
}
