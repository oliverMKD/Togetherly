package com.togetherly.core.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Below API 33 there is no runtime notification permission at all — [NotificationPermissionState.NotRequired]
 * reflects that honestly rather than claiming [NotificationPermissionState.Granted] for something
 * the family never actually granted. `shouldShowRequestPermissionRationale` needs an `Activity`,
 * not just a [Context], so this status check alone can't distinguish [NotificationPermissionState.Denied]
 * from [NotificationPermissionState.PermanentlyDenied] on API 33+ before the first request — both
 * report as [NotificationPermissionState.Denied] here; [rememberNotificationPermissionController]'s
 * own request flow is what actually makes that distinction, the same way
 * [com.togetherly.core.media.MicrophonePermissionRequester]'s Android actual does.
 */
internal class AndroidNotificationPermissionStatusProvider(
    private val context: Context,
) : NotificationPermissionStatusProvider {

    override suspend fun currentStatus(): NotificationPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return NotificationPermissionState.NotRequired
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) NotificationPermissionState.Granted else NotificationPermissionState.Denied
    }
}
