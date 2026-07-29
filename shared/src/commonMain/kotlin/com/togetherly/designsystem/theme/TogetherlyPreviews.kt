package com.togetherly.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A living catalogue of the design-system foundation itself — colors, type, spacing, shapes —
 * never a preview of actual product UI. Once real screens exist, their previews belong next to
 * them, not appended here.
 */

@Preview
@Composable
private fun TogetherlyThemeLightPreview() {
    TogetherlyTheme(darkTheme = false) { ThemeShowcase() }
}

@Preview
@Composable
private fun TogetherlyThemeDarkPreview() {
    TogetherlyTheme(darkTheme = true) { ThemeShowcase() }
}

@Composable
private fun ThemeShowcase() {
    Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = "Togetherly",
                style = MaterialTheme.togetherlyTypography.displayM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = "One small family adventure every day.",
                style = MaterialTheme.togetherlyTypography.bodyL,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            Surface(
                color = MaterialTheme.togetherlyColors.actionPrimary,
                shape = MaterialTheme.togetherlyShapes.pill,
            ) {
                Text(
                    text = "Start today's quest",
                    style = MaterialTheme.togetherlyTypography.labelL,
                    color = MaterialTheme.togetherlyColors.actionPrimaryContent,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.togetherlySpacing.l,
                        vertical = MaterialTheme.togetherlySpacing.s,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TypographyHierarchyPreview() {
    TogetherlyTheme {
        Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
            Column(
                modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                val typography = MaterialTheme.togetherlyTypography
                val foreground = MaterialTheme.togetherlyColors.foregroundPrimary
                Text("Display L", style = typography.displayL, color = foreground)
                Text("Display M", style = typography.displayM, color = foreground)
                Text("Headline L", style = typography.headlineL, color = foreground)
                Text("Headline M", style = typography.headlineM, color = foreground)
                Text("Title L", style = typography.titleL, color = foreground)
                Text("Title M", style = typography.titleM, color = foreground)
                Text("Body L — the quick brown fox jumps", style = typography.bodyL, color = foreground)
                Text("Body M — the quick brown fox jumps", style = typography.bodyM, color = foreground)
                Text("Body S — the quick brown fox jumps", style = typography.bodyS, color = foreground)
                Text("LABEL L", style = typography.labelL, color = foreground)
                Text("LABEL M", style = typography.labelM, color = foreground)
            }
        }
    }
}

@Preview
@Composable
private fun ColorSwatchesLightPreview() {
    TogetherlyTheme(darkTheme = false) { ColorSwatches() }
}

@Preview
@Composable
private fun ColorSwatchesDarkPreview() {
    TogetherlyTheme(darkTheme = true) { ColorSwatches() }
}

@Composable
private fun ColorSwatches() {
    val colors = MaterialTheme.togetherlyColors
    Surface(color = colors.backgroundCanvas) {
        Column(
            modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
        ) {
            Swatch("backgroundCanvas", colors.backgroundCanvas)
            Swatch("backgroundSurface", colors.backgroundSurface)
            Swatch("backgroundElevated", colors.backgroundElevated)
            Swatch("actionPrimary", colors.actionPrimary)
            Swatch("actionSecondary", colors.actionSecondary)
            Swatch("positive", colors.positive)
            Swatch("warning", colors.warning)
            Swatch("error", colors.error)
            Swatch("categoryTalk", colors.categoryTalk)
            Swatch("categoryCreate", colors.categoryCreate)
            Swatch("categoryMove", colors.categoryMove)
            Swatch("categoryKindness", colors.categoryKindness)
            Swatch("categoryDiscover", colors.categoryDiscover)
            Swatch("categorySilly", colors.categorySilly)
            Swatch("categoryMemories", colors.categoryMemories)
        }
    }
}

@Composable
private fun Swatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, RoundedCornerShape(8.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
            modifier = Modifier.padding(start = MaterialTheme.togetherlySpacing.xs),
        )
    }
}

@Preview
@Composable
private fun SpacingRhythmPreview() {
    TogetherlyTheme {
        Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
            val spacing = MaterialTheme.togetherlySpacing
            val steps = listOf(
                "xxs" to spacing.xxs,
                "xs" to spacing.xs,
                "s" to spacing.s,
                "m" to spacing.m,
                "l" to spacing.l,
                "xl" to spacing.xl,
                "xxl" to spacing.xxl,
            )
            Column(
                modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
            ) {
                steps.forEach { (label, dp) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.togetherlyTypography.labelM,
                            color = MaterialTheme.togetherlyColors.foregroundSecondary,
                            modifier = Modifier.width(32.dp),
                        )
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(dp)
                                .background(MaterialTheme.togetherlyColors.actionPrimary),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShapesPreview() {
    TogetherlyTheme {
        Surface(color = MaterialTheme.togetherlyColors.backgroundCanvas) {
            val shapes = MaterialTheme.togetherlyShapes
            Row(
                modifier = Modifier.padding(MaterialTheme.togetherlySpacing.m),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                listOf(
                    "small" to shapes.small,
                    "medium" to shapes.medium,
                    "large" to shapes.large,
                    "card" to shapes.card,
                    "pill" to shapes.pill,
                    "circular" to shapes.circular,
                ).forEach { (label, shape) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.togetherlyColors.actionSecondary, shape),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.togetherlyTypography.labelM,
                            color = MaterialTheme.togetherlyColors.foregroundSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
