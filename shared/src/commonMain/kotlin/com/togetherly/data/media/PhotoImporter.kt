package com.togetherly.data.media

import com.togetherly.core.result.DataResult

/** Raw, not-yet-validated bytes read from an opaque [PhotoImportSource]. */
data class RawImageBytes(
    val bytes: ByteArray,
    val sizeBytes: Long,
)

/**
 * Reads a platform picker result into portable bytes — the only piece of the private-photo
 * pipeline that touches a platform picker type at all, since it unwraps [PhotoImportSource]'s
 * platform-only payload internally. Does no decoding, validation, or normalization of its own.
 */
interface PhotoImporter {
    suspend fun import(source: PhotoImportSource): DataResult<RawImageBytes>
}
