package com.togetherly.core.media

import androidx.compose.runtime.Composable
import com.togetherly.data.media.PhotoPickerResult

fun interface PhotoPickerLauncher {
    fun launch()
}

/**
 * Presents the system photo picker (images only, single selection, no broad photo-library
 * permission) and imports the result into app-private pending storage before [onResult] ever sees
 * it — [onResult] only ever receives a [PhotoPickerResult] built from an already-private
 * [com.togetherly.data.media.PendingPhoto], never a platform picker URI. This is the Route-boundary
 * seam the spec calls for: a screen calls [launch] in response to a user tap, never anything
 * picker-related from its ViewModel.
 */
@Composable
expect fun rememberPhotoPickerLauncher(
    onResult: (PhotoPickerResult) -> Unit,
): PhotoPickerLauncher
