package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import com.togetherly.data.purchase.RevenueCatApiKeyProvider
import com.togetherly.data.telemetry.PostHogApiKeyProvider
import com.togetherly.data.telemetry.SentryDsnProvider
import org.koin.dsl.module
import platform.Foundation.NSBundle

private fun infoPlistString(key: String): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String)
        ?.takeUnless { it.isBlank() || it.startsWith("$(") }

fun initializeTogetherlyIos(debug: Boolean) {
    // Reads from Info.plist's REVENUECAT_API_KEY entry, which Xcode substitutes from a local,
    // gitignored xcconfig at build time — never hardcoded here. See
    // iosApp/Configuration/RevenueCat.local.xcconfig.example and docs/revenuecat-setup.md for
    // exactly where to place the real value. This is the only place the iOS public key is read.
    val revenueCatKeyModule = module {
        single<RevenueCatApiKeyProvider> {
            RevenueCatApiKeyProvider { infoPlistString("REVENUECAT_API_KEY") }
        }
    }

    // Same pattern for PostHog (Step 14.2) — Info.plist's POSTHOG_PROJECT_KEY/POSTHOG_HOST
    // entries, substituted from iosApp/Configuration/PostHog.local.xcconfig at build time. See
    // PostHog.local.xcconfig.example and docs/analytics-setup.md. This is the only place the iOS
    // PostHog project key is read.
    val posthogKeyModule = module {
        single<PostHogApiKeyProvider> {
            object : PostHogApiKeyProvider {
                override fun projectKey(): String? = infoPlistString("POSTHOG_PROJECT_KEY")
                override fun host(): String? = infoPlistString("POSTHOG_HOST")
            }
        }
    }

    // Same pattern for Sentry (Step 14.5) — Info.plist's SENTRY_DSN entry, substituted from
    // iosApp/Configuration/Sentry.local.xcconfig at build time. See Sentry.local.xcconfig.example
    // and docs/sentry-setup.md. This is the only place the iOS DSN value is read.
    val sentryKeyModule = module {
        single<SentryDsnProvider> {
            SentryDsnProvider { infoPlistString("SENTRY_DSN") }
        }
    }

    initKoin(
        appConfiguration = AppConfiguration(
            applicationName = "Togetherly",
            debug = debug,
        ),
        additionalModules = listOf(revenueCatKeyModule, posthogKeyModule, sentryKeyModule),
    )
}
