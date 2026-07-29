package com.togetherly.core.feedback

import androidx.compose.runtime.Composable

/**
 * A platform-safe Composable effect — never a raw `Activity`/`Window`/UIKit call from shared
 * presentation code. [enabled] is the *whole* decision the caller has to make: Quest Mode calls
 * this with `false` while phone-down is active or the quest has no
 * [com.togetherly.domain.quest.QuestTimer.keepScreenOn] request, and the platform `actual`
 * guarantees whatever it changed is restored the instant this leaves composition or [enabled]
 * turns `false` — never a permanent window/idle-timer state change.
 */
@Composable
expect fun KeepScreenOnEffect(enabled: Boolean)
