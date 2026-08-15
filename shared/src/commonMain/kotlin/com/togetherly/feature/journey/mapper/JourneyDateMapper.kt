package com.togetherly.feature.journey.mapper

import com.togetherly.core.datetime.localizedDateDisplay
import com.togetherly.core.datetime.localizedTimeDisplay
import kotlinx.datetime.LocalDateTime

internal fun LocalDateTime.toJourneyDateDisplay(): String = localizedDateDisplay()

internal fun LocalDateTime.toJourneyTimeDisplay(): String = localizedTimeDisplay()
