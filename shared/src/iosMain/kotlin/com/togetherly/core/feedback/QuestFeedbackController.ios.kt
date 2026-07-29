package com.togetherly.core.feedback

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/**
 * A single restrained haptic per cue via `UIKit`'s feedback generators — never a repeating pattern,
 * never a bundled sound file (see this feature's own KDoc for why sound is optional and, here,
 * skipped rather than adding a media-player dependency for one tiny cue). Every call is wrapped so
 * a simulator (no haptics engine) or any other platform quirk never crashes Quest Mode.
 */
internal class IosQuestFeedbackController : QuestFeedbackController {

    override fun timerFinished() {
        runCatching {
            UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
        }
    }

    override fun questCompleted() {
        runCatching {
            UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        }
    }
}
