package com.togetherly.data.media

import com.togetherly.domain.completion.PendingMediaReference

data class PendingPhoto(
    val reference: PendingMediaReference,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)
