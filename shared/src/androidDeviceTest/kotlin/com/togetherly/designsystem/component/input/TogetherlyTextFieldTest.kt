package com.togetherly.designsystem.component.input

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Confirms error state is exposed through the platform's own error semantics
 * ([SemanticsProperties.Error]) rather than only through the (feature-supplied) error text's
 * color — [TogetherlyTextField] never decides validity itself, only renders whatever [errorText]
 * it's given (see its own KDoc).
 */
@RunWith(AndroidJUnit4::class)
internal class TogetherlyTextFieldTest {

    private fun hasError(expected: String) =
        SemanticsMatcher("has error '$expected'") {
            it.config.getOrNull(SemanticsProperties.Error) == expected
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun errorTextIsExposedAsAccessibilityError() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyTextField(
                    value = "",
                    onValueChange = {},
                    label = "Family name",
                    errorText = "A family name is required",
                )
            }
        }

        onNodeWithText("A family name is required").assertExists()
        onNodeWithText("Family name").assert(hasError("A family name is required"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noErrorMeansNoAccessibilityError() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                TogetherlyTextField(value = "Rivera", onValueChange = {}, label = "Family name")
            }
        }

        onNodeWithText("Family name").assert(
            SemanticsMatcher("has no error") { it.config.getOrNull(SemanticsProperties.Error) == null },
        )
    }
}
