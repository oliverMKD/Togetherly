package com.togetherly.feature.memory.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.PendingMediaReference

/**
 * No width/height here — the private-photo loader decodes the real bytes when it renders, so the
 * UI sizes itself from the actual decoded image rather than a cached value that could drift from
 * what's really on disk.
 */
@Immutable
data class MemoryPhotoUi(
    val reference: PhotoPreviewReference,
)

/**
 * Distinguishes a freshly-imported, not-yet-saved photo from one the completion already had when
 * the screen opened — both need to render the same way, but only [Committed] survives if the user
 * skips/backs out without saving.
 */
sealed interface PhotoPreviewReference {
    data class Pending(val reference: PendingMediaReference) : PhotoPreviewReference
    data class Committed(val reference: MediaReference) : PhotoPreviewReference
}
