package com.togetherly.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage

/**
 * Shared UIKit decode/redraw helpers used by both [IosImageNormalizer] and [IosThumbnailGenerator].
 * Redrawing through [UIGraphicsImageRenderer] both bakes [UIImage.imageOrientation] into the
 * output pixels (the renderer always produces an upright image) and drops every EXIF field —
 * the result is a fresh bitmap built only from decoded pixels, never a copy of the source bytes.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosImageUtils {

    fun redrawUpright(image: UIImage): UIImage {
        val (width, height) = image.size.useContents { width to height }
        val renderer = UIGraphicsImageRenderer(size = image.size)
        return renderer.imageWithActions { image.drawInRect(CGRectMake(0.0, 0.0, width, height)) }
    }

    /** Never upscales — returns [image] unchanged if it's already within [maxDimension]. */
    fun downscaleIfNeeded(image: UIImage, maxDimension: Int): UIImage {
        val (width, height) = image.size.useContents { width to height }
        val longestSide = maxOf(width, height)
        if (longestSide <= maxDimension) return image
        val scale = maxDimension / longestSide
        val newWidth = width * scale
        val newHeight = height * scale
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(newWidth, newHeight))
        return renderer.imageWithActions { image.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight)) }
    }

    fun dimensions(image: UIImage): Pair<Int, Int> =
        image.size.useContents { width.toInt() to height.toInt() }
}
