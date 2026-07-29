package com.togetherly.core.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        val application = UIApplication.sharedApplication
        val previousValue = application.idleTimerDisabled
        if (enabled) {
            application.idleTimerDisabled = true
        }
        onDispose {
            application.idleTimerDisabled = previousValue
        }
    }
}
