package com.togetherly.core.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.togetherly.core.result.DataResult
import com.togetherly.data.media.AndroidPhotoImportSource
import com.togetherly.data.media.PhotoPickerResult
import com.togetherly.data.media.PrivateMediaStorage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * [ActivityResultContracts.PickVisualMedia] is the system Photo Picker contract — on API levels
 * below 33 it's transparently backed by the Google-Play-services backport already pulled in
 * transitively by `androidx.activity`, so no extra dependency or permission is needed even though
 * this app's minSdk is 26. A `null` result means the user backed out of the picker, which is
 * reported as [PhotoPickerResult.Cancelled], not a failure.
 */
@Composable
actual fun rememberPhotoPickerLauncher(
    onResult: (PhotoPickerResult) -> Unit,
): PhotoPickerLauncher {
    val mediaStorage = koinInject<PrivateMediaStorage>()
    val scope = rememberCoroutineScope()

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            onResult(PhotoPickerResult.Cancelled)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            when (val result = mediaStorage.createPendingPhoto(AndroidPhotoImportSource(uri))) {
                is DataResult.Error -> onResult(PhotoPickerResult.Failure(result.error))
                is DataResult.Success -> onResult(PhotoPickerResult.Imported(result.value))
            }
        }
    }

    return remember(activityLauncher) {
        PhotoPickerLauncher {
            activityLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
