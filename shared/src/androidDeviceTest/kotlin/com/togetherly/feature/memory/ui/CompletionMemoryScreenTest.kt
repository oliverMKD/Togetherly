package com.togetherly.feature.memory.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.model.MemoryPhotoUi
import com.togetherly.feature.memory.model.PhotoPreviewReference
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class CompletionMemoryScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun privatePhotoReferenceDoesNotLeakIntoSemanticsWhileTheImageIsLabeled() = runComposeUiTest {
        setContent {
            TogetherlyTheme {
                CompletionMemoryScreen(
                    state = CompletionMemoryUiState(
                        completionId = CompletionId("completion-1"),
                        questTitle = "Secret quest",
                        photo = MemoryPhotoUi(
                            reference = PhotoPreviewReference.Pending(PendingMediaReference("pending-photo")),
                        ),
                        allowPhotos = true,
                        allowVoiceMemories = false,
                        allowTextNotes = false,
                        reactions = persistentSetOf(),
                    ),
                    onAction = {},
                    loadPhotoBytes = { tinyPngBytes() },
                )
            }
        }

        waitForIdle()

        onNodeWithContentDescription("Your added photo").assertExists()
        assertPrivateValueDoesNotLeak("pending-photo")
    }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.assertPrivateValueDoesNotLeak(value: String) {
        val tree = onRoot().printToString()
        assertTrue(value !in tree, "Expected \"$value\" to stay out of the accessibility tree")
    }

    private fun tinyPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.MAGENTA)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }
}
