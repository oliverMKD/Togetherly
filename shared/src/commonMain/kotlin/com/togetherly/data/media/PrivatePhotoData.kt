package com.togetherly.data.media

/**
 * Decoded bytes of a private photo (full-size or thumbnail, whichever [MediaReference] was
 * opened), meant to be handed straight to a platform image-loading seam and discarded — never
 * held in persistent UI state. `equals`/`hashCode` are overridden so [bytes] compares by content
 * rather than by array identity, since the default `data class` equality would not do that.
 */
class PrivatePhotoData(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean =
        other is PrivatePhotoData && width == other.width && height == other.height && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * (31 * bytes.contentHashCode() + width) + height
}
