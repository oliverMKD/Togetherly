package com.togetherly.app.di

import com.posthog.kmp.PostHogContext
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.feedback.IosQuestFeedbackController
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.core.notification.IosNotificationCenterAdapter
import com.togetherly.core.notification.RealIosNotificationCenterAdapter
import com.togetherly.core.notification.IosNotificationPermissionStatusProvider
import com.togetherly.core.notification.IosReminderScheduler
import com.togetherly.core.notification.NotificationPermissionStatusProvider
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.data.media.IosImageNormalizer
import com.togetherly.data.media.IosPhotoImporter
import com.togetherly.data.media.IosPrivateMediaRoot
import com.togetherly.data.media.IosPrivateMediaStorage
import com.togetherly.data.media.IosThumbnailGenerator
import com.togetherly.data.media.IosVoicePlaybackController
import com.togetherly.data.media.IosVoiceRecorder
import com.togetherly.data.media.ImageNormalizer
import com.togetherly.data.media.PendingMediaOrphanCleaner
import com.togetherly.data.media.PhotoImporter
import com.togetherly.data.media.PrivateMediaRoot
import com.togetherly.data.media.PrivateMediaStorage
import com.togetherly.data.media.ThumbnailGenerator
import com.togetherly.data.media.VoicePlaybackController
import com.togetherly.data.media.VoiceRecorder
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

internal class IosPlatformInfoProvider : PlatformInfoProvider {
    override fun platformName(): String =
        "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}

/**
 * Reads `CFBundleShortVersionString`/`CFBundleVersion` from the compiled Info.plist — synthesized
 * at build time from `iosApp/Configuration/Config.xcconfig`'s `MARKETING_VERSION`/
 * `CURRENT_PROJECT_VERSION` (`GENERATE_INFOPLIST_FILE = YES`), same
 * [NSBundle.objectForInfoDictionaryKey] pattern already used for `REVENUECAT_API_KEY` in
 * `TogetherlyIosInitializer.kt`.
 */
internal class IosVersionInfoProvider : VersionInfoProvider {
    override fun versionName(): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String).orEmpty()

    override fun buildNumber(): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String).orEmpty()
}

actual val platformModule: Module = module {
    single<PlatformInfoProvider> {
        IosPlatformInfoProvider()
    }
    single<VersionInfoProvider> {
        IosVersionInfoProvider()
    }
    single<PostHogContext> {
        // No platform-specific context is required on iOS — see PostHogContext's own KDoc.
        PostHogContext()
    }
    single<QuestFeedbackController> {
        IosQuestFeedbackController()
    }
    single<ReminderScheduler> {
        IosReminderScheduler(notifications = get(), diagnostics = get())
    }
    single<IosNotificationCenterAdapter> {
        RealIosNotificationCenterAdapter()
    }
    single<NotificationPermissionStatusProvider> {
        IosNotificationPermissionStatusProvider(get())
    }

    single<PrivateMediaRoot> { IosPrivateMediaRoot() }
    single<PhotoImporter> { IosPhotoImporter() }
    single<ImageNormalizer> { IosImageNormalizer() }
    single<ThumbnailGenerator> { IosThumbnailGenerator() }
    single {
        IosPrivateMediaStorage(
            mediaRoot = get(),
            photoImporter = get(),
            imageNormalizer = get(),
            thumbnailGenerator = get(),
            idGenerator = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }
    single<PrivateMediaStorage> { get<IosPrivateMediaStorage>() }
    single<PendingMediaOrphanCleaner> { get<IosPrivateMediaStorage>() }

    single<VoiceRecorder> {
        IosVoiceRecorder(
            mediaRoot = get(),
            idGenerator = get(),
            clock = get(),
            dispatchers = get(),
        )
    }
    single<VoicePlaybackController> {
        IosVoicePlaybackController(
            mediaRoot = get(),
            dispatchers = get(),
        )
    }
}
