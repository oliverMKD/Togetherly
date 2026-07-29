package com.togetherly.data.media

import com.togetherly.domain.completion.MediaReference

/**
 * [thumbnailReference] is committed alongside [reference] in the same call so a completion's
 * photo and its Journey-sized thumbnail are always created and deleted together — see
 * [PrivateMediaPaths.thumbnailReferenceFor], which derives one from the other by naming
 * convention rather than storing them as two independent, driftable values.
 */
data class CommittedPhoto(
    val reference: MediaReference,
    val thumbnailReference: MediaReference,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)
