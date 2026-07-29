package com.togetherly.core.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Same `shouldShowRequestPermissionRationale`-after-denial trick as [com.togetherly.core.media.MicrophonePermissionRequester]'s Android actual — see that file's own KDoc for why it reliably distinguishes [NotificationPermissionState.Denied] from [NotificationPermissionState.PermanentlyDenied]. */
@Composable
actual fun rememberNotificationPermissionController(
    onResult: (NotificationPermissionState) -> Unit,
): NotificationPermissionController {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onResult(NotificationPermissionState.Granted)
        } else {
            val activity = context.findActivity()
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: true
            onResult(if (canAskAgain) NotificationPermissionState.Denied else NotificationPermissionState.PermanentlyDenied)
        }
    }

    return remember(context) {
        NotificationPermissionController {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(NotificationPermissionState.NotRequired)
                return@NotificationPermissionController
            }
            val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                onResult(NotificationPermissionState.Granted)
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
