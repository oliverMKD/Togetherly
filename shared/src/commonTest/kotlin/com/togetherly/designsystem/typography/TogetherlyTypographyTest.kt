package com.togetherly.designsystem.typography

import kotlin.test.Test
import kotlin.test.assertTrue

class TogetherlyTypographyTest {

    @Test
    fun fontSizeHierarchyDescendsFromDisplayToLabel() {
        val typography = DefaultTogetherlyTypography
        val descendingSizes = listOf(
            typography.displayL,
            typography.displayM,
            typography.headlineL,
            typography.headlineM,
            typography.titleL,
            typography.titleM,
            typography.bodyL,
            typography.bodyM,
            typography.bodyS,
        ).map { it.fontSize.value }

        descendingSizes.zipWithNext().forEach { (larger, smaller) ->
            assertTrue(larger >= smaller, "Expected $larger >= $smaller in the display-to-body hierarchy")
        }
    }

    @Test
    fun everyRoleUsesScalableSpUnits() {
        val typography = DefaultTogetherlyTypography
        listOf(
            typography.displayL, typography.displayM, typography.headlineL, typography.headlineM,
            typography.titleL, typography.titleM, typography.bodyL, typography.bodyM, typography.bodyS,
            typography.labelL, typography.labelM,
        ).forEach { style ->
            assertTrue(style.fontSize.isSp, "Font size must be defined in sp so it scales with system font size")
            assertTrue(style.lineHeight.isSp, "Line height must be defined in sp for the same reason")
        }
    }
}
