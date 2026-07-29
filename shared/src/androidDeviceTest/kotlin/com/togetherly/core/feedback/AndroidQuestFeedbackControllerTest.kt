package com.togetherly.core.feedback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A real device/emulator has a real (or absent, on some emulators) vibrator — either way, both
 * methods must never throw (see [QuestFeedbackController]'s own KDoc). This is a smoke test, not
 * an assertion that vibration actually occurred (there's no reliable, permission-free way to
 * observe that from the calling process).
 */
@RunWith(AndroidJUnit4::class)
internal class AndroidQuestFeedbackControllerTest {

    private val controller = AndroidQuestFeedbackController(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun timerFinishedNeverThrows() {
        controller.timerFinished()
    }

    @Test
    fun questCompletedNeverThrows() {
        controller.questCompleted()
    }

    @Test
    fun repeatedCallsNeverThrow() {
        repeat(5) {
            controller.timerFinished()
            controller.questCompleted()
        }
    }
}
