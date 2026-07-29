package com.togetherly.core.net

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberExternalUrlLauncher(): ExternalUrlLauncher = remember {
    ExternalUrlLauncher { url ->
        if (!isValidExternalUrl(url)) return@ExternalUrlLauncher
        NSURL.URLWithString(url)?.let { nsUrl ->
            if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
                UIApplication.sharedApplication.openURL(nsUrl)
            }
        }
    }
}
