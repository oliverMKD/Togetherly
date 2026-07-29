package com.togetherly.core.feedback

/**
 * The one platform boundary for Quest Mode's haptic/sound cues — resolved via Koin at the
 * `Route`/platform UI boundary (see [com.togetherly.feature.questmode.presentation.QuestModeRoute]),
 * never injected into [com.togetherly.feature.questmode.presentation.QuestModeViewModel]. Shared
 * presentation code (the ViewModel, `QuestModeScreen`) never imports `android.os.Vibrator`,
 * `UIKit`'s haptic generators, or any other platform audio/haptics API directly — everything platform
 * -specific lives behind this interface's two `actual` implementations.
 *
 * Both methods must never throw — a failure here (an unsupported device, a missing haptics engine,
 * silent mode) is not something Quest Mode's own flow should ever be interrupted by; implementations
 * catch and swallow their own platform errors internally.
 */
interface QuestFeedbackController {

    fun timerFinished()

    fun questCompleted()
}
