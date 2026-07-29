package com.togetherly.app.di

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.posthog.kmp.PostHogContext
import com.togetherly.app.foundation.PlatformInfoProvider
import com.togetherly.app.foundation.VersionInfoProvider
import com.togetherly.core.feedback.AndroidQuestFeedbackController
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.core.notification.AndroidNotificationPermissionStatusProvider
import com.togetherly.core.notification.AndroidReminderScheduler
import com.togetherly.core.notification.NotificationPermissionStatusProvider
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.data.media.AndroidImageNormalizer
import com.togetherly.data.media.AndroidPhotoImporter
import com.togetherly.data.media.AndroidPrivateMediaRoot
import com.togetherly.data.media.AndroidPrivateMediaStorage
import com.togetherly.data.media.AndroidThumbnailGenerator
import com.togetherly.data.media.AndroidVoicePlaybackController
import com.togetherly.data.media.AndroidVoiceRecorder
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

internal class AndroidPlatformInfoProvider : PlatformInfoProvider {
    override fun platformName(): String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Reads the installed APK's own version info via [android.content.pm.PackageManager] rather than
 * `BuildConfig` — `shared` is a library module, and `BuildConfig.VERSION_NAME`/`VERSION_CODE`
 * belong to `androidApp`'s generated class, which `shared` cannot depend on (dependency points the
 * other way). This works regardless of which module the version was actually declared in.
 */
internal class AndroidVersionInfoProvider(private val context: Context) : VersionInfoProvider {
    private val packageInfo: PackageInfo by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    override fun versionName(): String = packageInfo.versionName.orEmpty()

    override fun buildNumber(): String {
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return code.toString()
    }
}

actual val platformModule: Module = module {
    single<PlatformInfoProvider> {
        AndroidPlatformInfoProvider()
    }
    single<VersionInfoProvider> {
        AndroidVersionInfoProvider(context = get())
    }
    single<PostHogContext> {
        // The bound Context is always the real Application instance (applicationContext, read
        // from inside TogetherlyApplication.onCreate — see androidContextModule) — PostHogContext
        // itself requires Application specifically, not any Context.
        PostHogContext(application = get<Context>() as Application)
    }
    single<QuestFeedbackController> {
        AndroidQuestFeedbackController(get())
    }
    single<ReminderScheduler> {
        AndroidReminderScheduler(context = get(), clock = get(), timeZoneProvider = get(), diagnostics = get())
    }
    single<NotificationPermissionStatusProvider> {
        AndroidNotificationPermissionStatusProvider(get())
    }

    single<PrivateMediaRoot> { AndroidPrivateMediaRoot() }
    single<PhotoImporter> { AndroidPhotoImporter() }
    single<ImageNormalizer> { AndroidImageNormalizer() }
    single<ThumbnailGenerator> { AndroidThumbnailGenerator() }
    single {
        AndroidPrivateMediaStorage(
            mediaRoot = get(),
            photoImporter = get(),
            imageNormalizer = get(),
            thumbnailGenerator = get(),
            idGenerator = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }
    single<PrivateMediaStorage> { get<AndroidPrivateMediaStorage>() }
    single<PendingMediaOrphanCleaner> { get<AndroidPrivateMediaStorage>() }

    single<VoiceRecorder> {
        AndroidVoiceRecorder(
            mediaRoot = get(),
            idGenerator = get(),
            clock = get(),
            dispatchers = get(),
        )
    }
    single<VoicePlaybackController> {
        AndroidVoicePlaybackController(
            mediaRoot = get(),
            dispatchers = get(),
        )
    }
}
