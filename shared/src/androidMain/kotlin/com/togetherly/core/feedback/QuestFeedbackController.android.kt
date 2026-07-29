package com.togetherly.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * A single short, restrained vibration per cue — never a repeating or continuous pattern. Uses the
 * modern [VibratorManager] on API 31+ and falls back to the deprecated [Vibrator] system service
 * below that (this project's `minSdk` is 26). Every platform call is wrapped so a missing haptics
 * engine, a silent/do-not-disturb device policy, or any other platform quirk degrades to "no
 * feedback happened," never a crash.
 */
internal class AndroidQuestFeedbackController(
    private val context: Context,
) : QuestFeedbackController {

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    override fun timerFinished() = vibrateOnce(TIMER_FINISHED_MILLIS)

    override fun questCompleted() = vibrateOnce(QUEST_COMPLETED_MILLIS)

    private fun vibrateOnce(durationMillis: Long) {
        runCatching {
            val activeVibrator = vibrator ?: return@runCatching
            if (!activeVibrator.hasVibrator()) return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeVibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                activeVibrator.vibrate(durationMillis)
            }
        }
    }

    private companion object {
        const val TIMER_FINISHED_MILLIS = 200L
        const val QUEST_COMPLETED_MILLIS = 300L
    }
}
