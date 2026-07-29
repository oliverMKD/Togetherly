package com.togetherly.data.media

import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.runCatchingData
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.toDiagnosticThrowable
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIImage
import kotlin.time.Duration
import kotlin.time.Instant

/** Seconds between the Unix epoch (1970-01-01) and NSDate's reference date (2001-01-01). */
internal const val REFERENCE_DATE_OFFSET_SECONDS = 978307200.0

private val PRIVATE_MEDIA_STORAGE_CONTEXT = DiagnosticContext(mapOf("feature" to "media", "operation" to "private_media_storage"))

/**
 * The iOS counterpart to [AndroidPrivateMediaStorage] — same directory layout, same commit/delete
 * ordering and compensating cleanup on partial failure, implemented against [NSFileManager]
 * instead of `java.io.File`.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosPrivateMediaStorage(
    private val mediaRoot: PrivateMediaRoot,
    private val photoImporter: PhotoImporter,
    private val imageNormalizer: ImageNormalizer,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val idGenerator: IdGenerator,
    private val dispatchers: AppDispatchers,
    private val diagnostics: OperationalDiagnostics,
) : PrivateMediaStorage, PendingMediaOrphanCleaner {

    /** [runCatchingData] shared by other media-pipeline classes too — capturing here, not there, keeps this the one place a "private media storage failure" is reported. */
    private inline fun <T> runCatchingMediaStorage(block: () -> T): DataResult<T> =
        runCatchingData(block).also { result ->
            if (result is DataResult.Error) {
                diagnostics.captureHandledException(result.error.toDiagnosticThrowable(), PRIVATE_MEDIA_STORAGE_CONTEXT)
            }
        }

    private val fileManager get() = NSFileManager.defaultManager

    private fun absolutePath(relativeReference: String): String = "${mediaRoot.rootPath()}/$relativeReference"

    private fun parentDirectoryOf(absolutePath: String): String {
        val lastSlash = absolutePath.lastIndexOf('/')
        return if (lastSlash >= 0) absolutePath.substring(0, lastSlash) else absolutePath
    }

    private fun writeBytes(relativeReference: String, bytes: ByteArray) {
        val path = absolutePath(relativeReference)
        fileManager.createDirectoryAtPath(
            path = parentDirectoryOf(path),
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val written = fileManager.createFileAtPath(path, contents = bytes.toNSData(), attributes = null)
        check(written) { "Could not write to $path" }
    }

    private fun readBytes(relativeReference: String): ByteArray? {
        val data: NSData? = NSData.dataWithContentsOfFile(absolutePath(relativeReference))
        return data?.toByteArray()
    }

    private fun deleteFile(relativeReference: String) {
        fileManager.removeItemAtPath(absolutePath(relativeReference), error = null)
    }

    private fun deleteDirectoryIfEmpty(relativeDirectory: String) {
        if (relativeDirectory.isBlank()) return
        val path = absolutePath(relativeDirectory)
        val contents = fileManager.contentsOfDirectoryAtPath(path, error = null)
        if (contents != null && contents.isEmpty()) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    private fun parentDirectoryOfReference(relativeReference: String): String {
        val lastSlash = relativeReference.lastIndexOf('/')
        return if (lastSlash >= 0) relativeReference.substring(0, lastSlash) else ""
    }

    private fun decodeBounds(bytes: ByteArray): Pair<Int, Int> {
        val image = UIImage.imageWithData(bytes.toNSData()) ?: throw IllegalArgumentException("Could not read image bounds")
        return IosImageUtils.dimensions(image)
    }

    private fun writeDimensionsSidecar(relativeReference: String, width: Int, height: Int) {
        writeBytes(PrivateMediaPaths.dimensionsSidecarReference(relativeReference), "$width $height".encodeToByteArray())
    }

    /** Falls back to a real decode only if the sidecar is missing or unreadable — see [AndroidPrivateMediaStorage]'s equivalent. */
    private fun readDimensions(relativeReference: String, bytes: ByteArray): Pair<Int, Int> {
        val sidecarBytes = readBytes(PrivateMediaPaths.dimensionsSidecarReference(relativeReference))
        val fromSidecar = sidecarBytes?.let { sidecar ->
            val parts = sidecar.decodeToString().trim().split(" ")
            val width = parts.getOrNull(0)?.toIntOrNull()
            val height = parts.getOrNull(1)?.toIntOrNull()
            if (width != null && height != null) width to height else null
        }
        return fromSidecar ?: decodeBounds(bytes)
    }

    override suspend fun createPendingPhoto(source: PhotoImportSource): DataResult<PendingPhoto> =
        withContext(dispatchers.io) {
            val raw = when (val result = photoImporter.import(source)) {
                is DataResult.Error -> return@withContext result
                is DataResult.Success -> result.value
            }
            val normalized = when (val result = imageNormalizer.normalize(raw)) {
                is DataResult.Error -> return@withContext result
                is DataResult.Success -> result.value
            }
            runCatchingMediaStorage {
                val relativeReference = PrivateMediaPaths.pendingPhotoRelativeReference(idGenerator.generate())
                writeBytes(relativeReference, normalized.bytes)
                writeDimensionsSidecar(relativeReference, normalized.width, normalized.height)
                PendingPhoto(
                    reference = PendingMediaReference(relativeReference),
                    width = normalized.width,
                    height = normalized.height,
                    sizeBytes = normalized.bytes.size.toLong(),
                )
            }
        }

    override suspend fun commitPhoto(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<CommittedPhoto> = withContext(dispatchers.io) {
        if (!PrivateMediaPaths.isSafeRelativeReference(pending.value)) {
            return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val bytes = readBytes(pending.value)
            ?: return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))

        val bounds = try {
            readDimensions(pending.value, bytes)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
        }

        val photoRelative = PrivateMediaPaths.photoRelativeReference(completionId, mediaId)
        val thumbRelative = PrivateMediaPaths.thumbnailRelativeReference(completionId, mediaId)
        var photoWritten = false

        try {
            writeBytes(photoRelative, bytes)
            photoWritten = true

            val thumbnailBytes = when (val result = thumbnailGenerator.generateThumbnail(bytes)) {
                is DataResult.Error -> {
                    deleteFile(photoRelative)
                    return@withContext result
                }
                is DataResult.Success -> result.value
            }

            writeBytes(thumbRelative, thumbnailBytes)
            deleteFile(pending.value)
            deleteFile(PrivateMediaPaths.dimensionsSidecarReference(pending.value))

            DataResult.Success(
                CommittedPhoto(
                    reference = MediaReference(photoRelative),
                    thumbnailReference = MediaReference(thumbRelative),
                    width = bounds.first,
                    height = bounds.second,
                    sizeBytes = bytes.size.toLong(),
                ),
            )
        } catch (cancellation: CancellationException) {
            if (photoWritten) deleteFile(photoRelative)
            deleteFile(thumbRelative)
            throw cancellation
        } catch (t: Throwable) {
            if (photoWritten) deleteFile(photoRelative)
            deleteFile(thumbRelative)
            diagnostics.captureHandledException(t, PRIVATE_MEDIA_STORAGE_CONTEXT)
            DataResult.Error(AppError.Storage(StorageError.WRITE_FAILED, t))
        }
    }

    /** No normalization needed — the pending file is already the exact bytes [VoiceRecorder] captured. */
    override suspend fun commitVoice(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
        duration: Duration,
    ): DataResult<CommittedVoice> = withContext(dispatchers.io) {
        if (!PrivateMediaPaths.isSafeRelativeReference(pending.value)) {
            return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }
        val bytes = readBytes(pending.value)
            ?: return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        val voiceRelative = PrivateMediaPaths.voiceRelativeReference(completionId, mediaId)
        try {
            writeBytes(voiceRelative, bytes)
            deleteFile(pending.value)
            DataResult.Success(CommittedVoice(reference = MediaReference(voiceRelative), duration = duration, sizeBytes = bytes.size.toLong()))
        } catch (cancellation: CancellationException) {
            deleteFile(voiceRelative)
            throw cancellation
        } catch (t: Throwable) {
            deleteFile(voiceRelative)
            diagnostics.captureHandledException(t, PRIVATE_MEDIA_STORAGE_CONTEXT)
            DataResult.Error(AppError.Storage(StorageError.WRITE_FAILED, t))
        }
    }

    override suspend fun deletePending(reference: PendingMediaReference): DataResult<Unit> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            runCatchingMediaStorage {
                deleteFile(reference.value)
                deleteFile(PrivateMediaPaths.dimensionsSidecarReference(reference.value))
            }
        }

    override suspend fun deleteCommitted(reference: MediaReference): DataResult<Unit> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            runCatchingMediaStorage {
                deleteFile(reference.value)
                PrivateMediaPaths.thumbnailReferenceFor(reference)?.let { thumbnail -> deleteFile(thumbnail.value) }
                deleteDirectoryIfEmpty(parentDirectoryOfReference(reference.value))
            }
        }

    override suspend fun openPhoto(reference: MediaReference): DataResult<PrivatePhotoData> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            val bytes = readBytes(reference.value)
                ?: return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            runCatchingMediaStorage {
                val (width, height) = decodeBounds(bytes)
                PrivatePhotoData(bytes = bytes, width = width, height = height)
            }
        }

    override suspend fun openPendingPhoto(reference: PendingMediaReference): DataResult<PrivatePhotoData> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            val bytes = readBytes(reference.value)
                ?: return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            runCatchingMediaStorage {
                val (width, height) = readDimensions(reference.value, bytes)
                PrivatePhotoData(bytes = bytes, width = width, height = height)
            }
        }

    override suspend fun deleteExpiredPending(now: Instant, thresholdAge: Duration): DataResult<Int> =
        withContext(dispatchers.io) {
            runCatchingMediaStorage {
                val pendingDirectory = "${mediaRoot.rootPath()}/${PrivateMediaPaths.PENDING_DIRECTORY}"
                val fileNames = fileManager.contentsOfDirectoryAtPath(pendingDirectory, error = null) ?: emptyList<Any?>()
                // NSDate's only directly-declared time-interval property is relative to its own
                // reference date (2001-01-01T00:00:00Z), not the Unix epoch [now] is expressed
                // in — REFERENCE_DATE_OFFSET_SECONDS is that fixed 1970→2001 offset, so both
                // sides of the comparison end up in the same unit without constructing or
                // comparing NSDate instances at all.
                val cutoffEpochSeconds = (now.toEpochMilliseconds() - thresholdAge.inWholeMilliseconds) / 1000.0
                val cutoffReferenceSeconds = cutoffEpochSeconds - REFERENCE_DATE_OFFSET_SECONDS
                var deletedCount = 0
                for (fileName in fileNames) {
                    val name = fileName as? String ?: continue
                    val path = "$pendingDirectory/$name"
                    val attributes = fileManager.attributesOfItemAtPath(path, error = null)
                    val modifiedAt = attributes?.get(NSFileModificationDate) as? NSDate
                    val isExpired = modifiedAt != null && modifiedAt.timeIntervalSinceReferenceDate <= cutoffReferenceSeconds
                    if (isExpired && fileManager.removeItemAtPath(path, error = null)) {
                        deletedCount++
                    }
                }
                deletedCount
            }
        }
}
