package com.togetherly.core.net

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExternalUrlLauncher(): ExternalUrlLauncher {
    val context = LocalContext.current
    return remember(context) {
        ExternalUrlLauncher { url ->
            if (!isValidExternalUrl(url)) return@ExternalUrlLauncher
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // No app available to handle this URL — nothing more we can safely do here.
            }
        }
    }
}
