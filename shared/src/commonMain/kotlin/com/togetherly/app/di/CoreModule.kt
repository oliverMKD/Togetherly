package com.togetherly.app.di

import com.togetherly.app.application.AppInfoService
import com.togetherly.app.application.LegalConfiguration
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.coroutines.DefaultAppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.datetime.DefaultAppClock
import com.togetherly.core.datetime.DefaultAppTimeZoneProvider
import com.togetherly.core.id.DefaultIdGenerator
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.logging.AppLogger
import com.togetherly.core.logging.createPlatformLogger
import com.togetherly.data.purchase.RevenueCatConfigurator
import org.koin.dsl.module

val coreModule = module {
    single<AppDispatchers> {
        DefaultAppDispatchers()
    }

    single<AppLogger> {
        createPlatformLogger()
    }

    single<AppClock> {
        DefaultAppClock()
    }

    single<AppTimeZoneProvider> {
        DefaultAppTimeZoneProvider()
    }

    single<IdGenerator> {
        DefaultIdGenerator()
    }

    single {
        AppInfoService(
            configuration = get(),
            platformInfoProvider = get(),
        )
    }

    single {
        RevenueCatConfigurator(
            apiKeyProvider = get(),
            logger = get(),
            diagnostics = get(),
        )
    }

    single {
        LegalConfiguration.placeholder()
    }
}
