package com.togetherly.feature.memory.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Android's [ImageBitmap] is backed by `android.graphics.Bitmap`; iOS's by Skia — there is no
 * single common decode API, so each platform supplies its own. Returns `null` for empty/invalid
 * bytes rather than throwing, since a decode failure here is just "nothing to preview," not a
 * crash-worthy condition.
 */
internal expect fun decodeToImageBitmap(bytes: ByteArray): ImageBitmap?
