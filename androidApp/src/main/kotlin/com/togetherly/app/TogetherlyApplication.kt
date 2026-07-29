package com.togetherly.app

import android.app.Application
import android.content.Context
import com.togetherly.app.application.AppConfiguration
import com.togetherly.app.di.initKoin
import com.togetherly.data.purchase.RevenueCatApiKeyProvider
import com.togetherly.data.telemetry.PostHogApiKeyProvider
import com.togetherly.data.telemetry.SentryDsnProvider
import org.koin.dsl.module

class TogetherlyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Registered here (application context, never an Activity) so shared code can resolve a
        // Context through Koin instead of relying on any form of global/static access.
        val androidContextModule = module {
            single<Context> { applicationContext }
        }

        // BuildConfig.REVENUECAT_API_KEY comes from local.properties (see local.properties.example
        // and docs/revenuecat-setup.md) — never hardcoded, never committed. This is the only place
        // the Android public key value is read.
        val revenueCatKeyModule = module {
            single<RevenueCatApiKeyProvider> {
                RevenueCatApiKeyProvider { BuildConfig.REVENUECAT_API_KEY.ifBlank { null } }
            }
        }

        // BuildConfig.POSTHOG_PROJECT_KEY/POSTHOG_HOST come from local.properties (see
        // local.properties.example and docs/analytics-setup.md) — never hardcoded, never
        // committed. This is the only place the Android PostHog project key value is read.
        val posthogKeyModule = module {
            single<PostHogApiKeyProvider> {
                object : PostHogApiKeyProvider {
                    override fun projectKey(): String? = BuildConfig.POSTHOG_PROJECT_KEY.ifBlank { null }
                    override fun host(): String? = BuildConfig.POSTHOG_HOST.ifBlank { null }
                }
            }
        }

        // BuildConfig.SENTRY_DSN comes from local.properties (see local.properties.example and
        // docs/sentry-setup.md) — never hardcoded, never committed. A DSN, never a Sentry auth
        // token. This is the only place the Android DSN value is read.
        val sentryKeyModule = module {
            single<SentryDsnProvider> {
                SentryDsnProvider { BuildConfig.SENTRY_DSN.ifBlank { null } }
            }
        }

        initKoin(
            appConfiguration = AppConfiguration(
                applicationName = "Togetherly",
                debug = BuildConfig.DEBUG,
            ),
            additionalModules = listOf(androidContextModule, revenueCatKeyModule, posthogKeyModule, sentryKeyModule),
        )
    }
}
