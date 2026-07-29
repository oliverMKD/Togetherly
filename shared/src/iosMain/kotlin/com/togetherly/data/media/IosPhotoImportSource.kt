package com.togetherly.data.media

import platform.Foundation.NSData

/**
 * The iOS side of the [PhotoImportSource] marker. Holds already-read [data] rather than the
 * picker's temporary file `NSURL` — `PHPickerViewController`'s
 * `loadFileRepresentationForTypeIdentifier` only guarantees that URL is valid for the duration of
 * its own completion handler, so the picker bridge reads it into [data] immediately, before this
 * source object is ever constructed or handed off asynchronously.
 */
internal class IosPhotoImportSource(
    val data: NSData,
) : PhotoImportSource
