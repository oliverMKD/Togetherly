package com.togetherly.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermission
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted

/**
 * iOS only ever shows its own system microphone prompt once per install — every later call to
 * `requestRecordPermission` just replays the stored answer without prompting again. That's why a
 * `denied` [AVAudioSession.recordPermission] is reported as [MicrophonePermissionResult.PermanentlyDenied]
 * outright rather than [MicrophonePermissionResult.Denied]: there is no "ask again" on this
 * platform, only Settings.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMicrophonePermissionRequester(
    onResult: (MicrophonePermissionResult) -> Unit,
): MicrophonePermissionRequester = remember {
    MicrophonePermissionRequester {
        val session = AVAudioSession.sharedInstance()
        when (session.recordPermission) {
            AVAudioSessionRecordPermissionGranted -> onResult(MicrophonePermissionResult.Granted)
            AVAudioSessionRecordPermissionDenied -> onResult(MicrophonePermissionResult.PermanentlyDenied)
            else -> session.requestRecordPermission { granted ->
                onResult(if (granted) MicrophonePermissionResult.Granted else MicrophonePermissionResult.Denied)
            }
        }
    }
}
