package com.togetherly.core.datetime

import kotlinx.datetime.TimeZone

/**
 * The one place `TimeZone.currentSystemDefault()` is called from application/presentation logic —
 * a `ViewModel` (e.g. [com.togetherly.feature.today.presentation.TodayViewModel]) injects this
 * instead of calling [TimeZone.currentSystemDefault] inline, so tests can supply a fixed
 * [TimeZone] without depending on the host JVM/device's actual configured zone.
 */
interface AppTimeZoneProvider {
    fun current(): TimeZone
}

internal class DefaultAppTimeZoneProvider : AppTimeZoneProvider {
    override fun current(): TimeZone = TimeZone.currentSystemDefault()
}
