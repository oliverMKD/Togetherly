package com.togetherly.feature.memory.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodeToImageBitmap(bytes: ByteArray): ImageBitmap? =
    if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
