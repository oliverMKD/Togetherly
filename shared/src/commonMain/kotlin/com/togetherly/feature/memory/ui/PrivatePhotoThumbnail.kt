package com.togetherly.feature.memory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes

/**
 * The private-photo-reader seam this feature needs (see Step 10.2's own spec) — decodes bytes
 * handed to it by [loadBytes] into an [ImageBitmap] purely as transient Compose state via
 * [decodeToImageBitmap], never stored in [com.togetherly.feature.memory.model.CompletionMemoryUiState]
 * itself, so no raw image bytes ever enter persistent UI state. [loadBytes] is supplied by the
 * Route/Screen (backed by [com.togetherly.data.media.PrivateMediaStorage], resolved once at the
 * Route boundary), not resolved here — this composable has no Koin dependency of its own.
 */
@Composable
internal fun PrivatePhotoThumbnail(
    reference: Any?,
    loadBytes: suspend () -> ByteArray?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var imageBitmap by remember(reference) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(reference) {
        imageBitmap = loadBytes()?.let { decodeToImageBitmap(it) }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.togetherlyShapes.medium)
            .background(MaterialTheme.togetherlyColors.backgroundSurface),
    ) {
        imageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier,
                contentScale = ContentScale.Crop,
            )
        }
    }
}
