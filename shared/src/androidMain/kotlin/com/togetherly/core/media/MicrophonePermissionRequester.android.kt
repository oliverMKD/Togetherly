package com.togetherly.core.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
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

/**
 * `shouldShowRequestPermissionRationale` is `false` both before ever asking and once the system
 * considers the permission permanently denied (explicit "don't ask again", or enough prior
 * denials) — checking it only *after* a real denial (never before the first request) is what
 * makes those two cases distinguishable: a first-ever denial makes the platform start returning
 * `true` from that point on, so `false` after a denial reliably means permanently denied.
 */
@Composable
actual fun rememberMicrophonePermissionRequester(
    onResult: (MicrophonePermissionResult) -> Unit,
): MicrophonePermissionRequester {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onResult(MicrophonePermissionResult.Granted)
        } else {
            val activity = context.findActivity()
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ?: true
            onResult(if (canAskAgain) MicrophonePermissionResult.Denied else MicrophonePermissionResult.PermanentlyDenied)
        }
    }

    return remember(context) {
        MicrophonePermissionRequester {
            val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                onResult(MicrophonePermissionResult.Granted)
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
