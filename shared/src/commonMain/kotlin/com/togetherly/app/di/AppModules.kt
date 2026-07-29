package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformModule: Module

fun appModules(
    appConfiguration: AppConfiguration,
): List<Module> {
    val configurationModule = module {
        single { appConfiguration }
    }

    return listOf(
        configurationModule,
        coreModule,
        platformModule,
        contentModule,
        databaseModule,
        repositoryModule,
        readyUseCaseModule,
        presentationModule,
        telemetryModule,
    )
}
