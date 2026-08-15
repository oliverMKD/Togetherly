package com.togetherly.data.media

import android.graphics.BitmapFactory
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Instant

private val PRIVATE_MEDIA_STORAGE_CONTEXT = DiagnosticContext(mapOf("feature" to "media", "operation" to "private_media_storage"))

/**
 * The Android implementation of both [PrivateMediaStorage] and [PendingMediaOrphanCleaner] —
 * combined in one class because both need the same private-root/path-resolution logic, and Room's
 * "one class, several small contracts" precedent (see `RoomCompletionRepository`) already
 * establishes that's fine as long as each contract's own callers only depend on the interface,
 * never this concrete type.
 */
internal class AndroidPrivateMediaStorage(
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

    private fun absolutePath(relativeReference: String): String = "${mediaRoot.rootPath()}/$relativeReference"

    private fun writeBytes(relativeReference: String, bytes: ByteArray) {
        val file = File(absolutePath(relativeReference))
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun deleteIfExists(relativeReference: String) {
        File(absolutePath(relativeReference)).delete()
    }

    private fun uniquePendingPhotoRelativeReference(): String {
        val baseId = idGenerator.generate()
        var attempt = 0
        while (true) {
            val id = if (attempt == 0) baseId else "$baseId-$attempt"
            val candidate = PrivateMediaPaths.pendingPhotoRelativeReference(id)
            val file = File(absolutePath(candidate))
            if (!file.exists() && !File(absolutePath(PrivateMediaPaths.dimensionsSidecarReference(candidate))).exists()) {
                return candidate
            }
            attempt++
        }
    }

    private fun decodeBounds(bytes: ByteArray): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Could not read image bounds" }
        return options.outWidth to options.outHeight
    }

    private fun writeDimensionsSidecar(relativeReference: String, width: Int, height: Int) {
        writeBytes(PrivateMediaPaths.dimensionsSidecarReference(relativeReference), "$width $height".encodeToByteArray())
    }

    /** Falls back to a real decode only if the sidecar is missing or unreadable. */
    private fun readDimensions(relativeReference: String, bytes: ByteArray): Pair<Int, Int> {
        val sidecar = File(absolutePath(PrivateMediaPaths.dimensionsSidecarReference(relativeReference)))
        val fromSidecar = sidecar.takeIf { it.exists() }?.let { file ->
            val parts = file.readText().trim().split(" ")
            val width = parts.getOrNull(0)?.toIntOrNull()
            val height = parts.getOrNull(1)?.toIntOrNull()
            if (width != null && height != null) width to height else null
        }
        return fromSidecar ?: decodeBounds(bytes)
    }

    private fun parentDirectoryOf(relativeReference: String): String {
        val lastSlash = relativeReference.lastIndexOf('/')
        return if (lastSlash >= 0) relativeReference.substring(0, lastSlash) else ""
    }

    private fun deleteDirectoryIfEmpty(relativeDirectory: String) {
        if (relativeDirectory.isBlank()) return
        val dir = File(absolutePath(relativeDirectory))
        if (dir.isDirectory && dir.listFiles()?.isEmpty() == true) {
            dir.delete()
        }
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
            var relativeReference: String? = null
            try {
                relativeReference = uniquePendingPhotoRelativeReference()
                writeBytes(relativeReference, normalized.bytes)
                writeDimensionsSidecar(relativeReference, normalized.width, normalized.height)
                DataResult.Success(
                    PendingPhoto(
                        reference = PendingMediaReference(relativeReference),
                        width = normalized.width,
                        height = normalized.height,
                        sizeBytes = normalized.bytes.size.toLong(),
                    ),
                )
            } catch (cancellation: CancellationException) {
                relativeReference?.let {
                    deleteIfExists(it)
                    deleteIfExists(PrivateMediaPaths.dimensionsSidecarReference(it))
                }
                throw cancellation
            } catch (t: Throwable) {
                relativeReference?.let {
                    deleteIfExists(it)
                    deleteIfExists(PrivateMediaPaths.dimensionsSidecarReference(it))
                }
                diagnostics.captureHandledException(t, PRIVATE_MEDIA_STORAGE_CONTEXT)
                DataResult.Error(AppError.Storage(StorageError.WRITE_FAILED, t))
            }
        }

    /**
     * Writes the photo, then the thumbnail, then deletes the pending source — in that order, so a
     * failure partway through never leaves a database-referenceable file behind (any file already
     * written this call is removed) while the original pending file survives for a retry. The
     * pending file is only ever removed once both permanent files exist.
     */
    override suspend fun commitPhoto(
        pending: PendingMediaReference,
        completionId: CompletionId,
        mediaId: MemoryMediaId,
    ): DataResult<CommittedPhoto> = withContext(dispatchers.io) {
        if (!PrivateMediaPaths.isSafeRelativeReference(pending.value)) {
            return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }
        val pendingFile = File(absolutePath(pending.value))
        if (!pendingFile.exists()) {
            return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        }

        val bytes = try {
            pendingFile.readBytes()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            diagnostics.captureHandledException(t, PRIVATE_MEDIA_STORAGE_CONTEXT)
            return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED, t))
        }

        val bounds = try {
            readDimensions(pending.value, bytes)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_MEDIA))
        }

        val photoRelative = PrivateMediaPaths.photoRelativeReference(completionId, mediaId)
        val thumbRelative = PrivateMediaPaths.thumbnailRelativeReference(completionId, mediaId)

        try {
            writeBytes(photoRelative, bytes)

            val thumbnailBytes = when (val result = thumbnailGenerator.generateThumbnail(bytes)) {
                is DataResult.Error -> {
                    deleteIfExists(photoRelative)
                    return@withContext result
                }
                is DataResult.Success -> result.value
            }

            writeBytes(thumbRelative, thumbnailBytes)
            pendingFile.delete()
            File(absolutePath(PrivateMediaPaths.dimensionsSidecarReference(pending.value))).delete()

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
            deleteIfExists(photoRelative)
            deleteIfExists(thumbRelative)
            throw cancellation
        } catch (t: Throwable) {
            deleteIfExists(photoRelative)
            deleteIfExists(thumbRelative)
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
        val pendingFile = File(absolutePath(pending.value))
        if (!pendingFile.exists()) {
            return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
        }
        val voiceRelative = PrivateMediaPaths.voiceRelativeReference(completionId, mediaId)
        try {
            val bytes = pendingFile.readBytes()
            writeBytes(voiceRelative, bytes)
            pendingFile.delete()
            DataResult.Success(CommittedVoice(reference = MediaReference(voiceRelative), duration = duration, sizeBytes = bytes.size.toLong()))
        } catch (cancellation: CancellationException) {
            deleteIfExists(voiceRelative)
            throw cancellation
        } catch (t: Throwable) {
            deleteIfExists(voiceRelative)
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
                File(absolutePath(reference.value)).delete()
                File(absolutePath(PrivateMediaPaths.dimensionsSidecarReference(reference.value))).delete()
                Unit
            }
        }

    override suspend fun deleteCommitted(reference: MediaReference): DataResult<Unit> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            runCatchingMediaStorage {
                File(absolutePath(reference.value)).delete()
                PrivateMediaPaths.thumbnailReferenceFor(reference)?.let { thumbnail ->
                    File(absolutePath(thumbnail.value)).delete()
                }
                deleteDirectoryIfEmpty(parentDirectoryOf(reference.value))
                Unit
            }
        }

    override suspend fun openPhoto(reference: MediaReference): DataResult<PrivatePhotoData> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            val file = File(absolutePath(reference.value))
            if (!file.exists()) {
                return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            }
            runCatchingMediaStorage {
                val bytes = file.readBytes()
                val (width, height) = decodeBounds(bytes)
                PrivatePhotoData(bytes = bytes, width = width, height = height)
            }
        }

    override suspend fun openPendingPhoto(reference: PendingMediaReference): DataResult<PrivatePhotoData> =
        withContext(dispatchers.io) {
            if (!PrivateMediaPaths.isSafeRelativeReference(reference.value)) {
                return@withContext DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
            }
            val file = File(absolutePath(reference.value))
            if (!file.exists()) {
                return@withContext DataResult.Error(AppError.Storage(StorageError.READ_FAILED))
            }
            runCatchingMediaStorage {
                val bytes = file.readBytes()
                val (width, height) = readDimensions(reference.value, bytes)
                PrivatePhotoData(bytes = bytes, width = width, height = height)
            }
        }

    override suspend fun deleteExpiredPending(now: Instant, thresholdAge: Duration): DataResult<Int> =
        withContext(dispatchers.io) {
            runCatchingMediaStorage {
                val pendingDirectory = File(mediaRoot.rootPath(), PrivateMediaPaths.PENDING_DIRECTORY)
                val files = pendingDirectory.listFiles() ?: emptyArray()
                var deletedCount = 0
                for (file in files) {
                    val age = now.toEpochMilliseconds() - file.lastModified()
                    if (age >= thresholdAge.inWholeMilliseconds && file.delete()) {
                        deletedCount++
                        if (file.name.endsWith(".jpg") || file.name.endsWith(".m4a")) {
                            File(file.absolutePath + ".dims").delete()
                        }
                    }
                }
                deletedCount
            }
        }
}
