package com.togetherly.designsystem.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography

/**
 * A card that is one option among several (e.g. an onboarding pick-one-of-N choice) rather than a
 * plain container — see [com.togetherly.designsystem.component.card.TogetherlyCard] for that case.
 *
 * Selection is never color-only: a selected card gets a thicker [MaterialTheme.togetherlyColors]
 * `actionPrimary` border, a filled [SelectionIndicator] dot (an outline-only ring when
 * unselected), *and* the [Role.RadioButton] `selected` semantic that
 * [androidx.compose.foundation.selection.selectable] sets — three redundant signals, so
 * color-blindness, a screen reader, or a grayscale screenshot each still convey which card is
 * picked.
 *
 * [Role.RadioButton] is the default (a single-select "pick one of N" picker); pass
 * [multiSelect]`= true` for a "pick any of N" picker (e.g. onboarding's age-band or interest
 * choices), which switches the semantic role to [Role.Checkbox] instead — a screen reader must
 * never describe a multi-select group of cards as mutually exclusive. The visual treatment
 * (border, [SelectionIndicator] dot) is identical either way; only the announced role changes.
 */
@Composable
fun TogetherlySelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiSelect: Boolean = false,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.togetherlyColors
    val borderColor = if (selected) colors.actionPrimary else colors.borderSubtle
    val borderWidth = if (selected) 2.dp else 1.dp

    Card(
        modifier = modifier
            .heightIn(min = MaterialTheme.togetherlySize.minimumTouchTarget)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            ),
        shape = MaterialTheme.togetherlyShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundSurface,
            contentColor = colors.foregroundPrimary,
        ),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.size(MaterialTheme.togetherlySpacing.m))
            }
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(text = title, style = MaterialTheme.togetherlyTypography.titleM, color = colors.foregroundPrimary)
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.togetherlyTypography.bodyM,
                        color = colors.foregroundSecondary,
                    )
                }
            }
            Spacer(Modifier.size(MaterialTheme.togetherlySpacing.s))
            SelectionIndicator(selected = selected, color = borderColor)
        }
    }
}

/**
 * A purely decorative selection dot — [selected] is already exposed to accessibility services via
 * the [Role.RadioButton]/[Role.Checkbox] semantics [TogetherlySelectableCard] sets, so this
 * indicator clears its own semantics rather than duplicating that announcement.
 */
@Composable
private fun SelectionIndicator(selected: Boolean, color: Color) {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .clearAndSetSemantics { },
    ) {
        drawCircle(color = color, style = Stroke(width = 1.5.dp.toPx()))
        if (selected) {
            drawCircle(color = color, radius = size.minDimension / 2f - 4.dp.toPx())
        }
    }
}
