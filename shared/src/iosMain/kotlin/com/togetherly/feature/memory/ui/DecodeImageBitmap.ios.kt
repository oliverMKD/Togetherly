package com.togetherly.feature.memory.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

internal actual fun decodeToImageBitmap(bytes: ByteArray): ImageBitmap? =
    if (bytes.isEmpty()) null else runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
