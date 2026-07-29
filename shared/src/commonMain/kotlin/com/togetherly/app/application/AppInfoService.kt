package com.togetherly.app.application

import com.togetherly.app.foundation.PlatformInfoProvider

class AppInfoService(
    private val configuration: AppConfiguration,
    private val platformInfoProvider: PlatformInfoProvider,
) {
    fun appDescription(): String =
        "${configuration.applicationName} on ${platformInfoProvider.platformName()}"
}
