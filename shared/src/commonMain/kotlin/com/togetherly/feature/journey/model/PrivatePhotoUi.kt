package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.MediaReference

/**
 * [reference] is the same [com.togetherly.domain.completion.MemoryMedia.Photo.localReference] the
 * completion itself was saved with — deriving the actual thumbnail *file* from it is the data
 * layer's job, not this mapper's; the Route always reads this through
 * [com.togetherly.data.media.PrivateMediaStorage.openPhotoThumbnail], never `openPhoto`, so the
 * timeline never decodes a full-resolution image just to render a small thumbnail.
 */
@Immutable
data class PrivatePhotoUi(
    val reference: MediaReference,
)
