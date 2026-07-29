package com.togetherly.data.media

import android.net.Uri

/**
 * The Android side of the [PhotoImportSource] marker — [uri] is the system Photo Picker's result,
 * read once by [AndroidPhotoImporter] and never retained past that read; it is never stored as
 * domain state or passed to a ViewModel.
 */
internal class AndroidPhotoImportSource(
    val uri: Uri,
) : PhotoImportSource
