package com.togetherly.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.togetherly.core.result.DataResult
import com.togetherly.data.media.IosPhotoImportSource
import com.togetherly.data.media.PhotoPickerResult
import com.togetherly.data.media.PrivateMediaStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject

/**
 * `PHPickerViewController.delegate` is a weak reference, so without an external strong reference
 * ARC could deallocate a delegate between presenting the picker and its completion callback —
 * [activeDelegates] is that strong reference, holding each delegate until its own callback removes
 * it.
 */
private val activeDelegates = mutableListOf<PhotoPickerDelegate>()

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val onFinished: (NSData?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider

        if (provider == null || !provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
            onFinished(null)
            return
        }

        provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, _ ->
            onFinished(data)
        }
    }
}

/**
 * `PHPickerViewController` (iOS 14+) never grants this app direct photo-library access — it runs
 * out-of-process and only ever hands back the one item the user picked, so no
 * `NSPhotoLibraryUsageDescription` is required. [NSItemProvider.loadDataRepresentationForTypeIdentifier]'s
 * completion handler may run on a background queue, so every callback here funnels through
 * [scope] rather than touching Compose state directly from that queue.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPhotoPickerLauncher(
    onResult: (PhotoPickerResult) -> Unit,
): PhotoPickerLauncher {
    val mediaStorage = koinInject<PrivateMediaStorage>()
    val scope = rememberCoroutineScope()

    return remember(mediaStorage, scope) {
        PhotoPickerLauncher {
            launchPhotoPicker(mediaStorage, scope, onResult)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun launchPhotoPicker(
    mediaStorage: PrivateMediaStorage,
    scope: CoroutineScope,
    onResult: (PhotoPickerResult) -> Unit,
) {
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 1
        filter = PHPickerFilter.imagesFilter()
    }
    val pickerController = PHPickerViewController(configuration = configuration)

    lateinit var delegate: PhotoPickerDelegate
    delegate = PhotoPickerDelegate { data ->
        activeDelegates.remove(delegate)
        scope.launch(Dispatchers.Main) {
            if (data == null) {
                onResult(PhotoPickerResult.Cancelled)
                return@launch
            }
            when (val result = mediaStorage.createPendingPhoto(IosPhotoImportSource(data))) {
                is DataResult.Error -> onResult(PhotoPickerResult.Failure(result.error))
                is DataResult.Success -> onResult(PhotoPickerResult.Imported(result.value))
            }
        }
    }
    activeDelegates += delegate
    pickerController.delegate = delegate

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(pickerController, animated = true, completion = null)
}
