package com.togetherly.feature.completion.mapper

import com.togetherly.core.datetime.localizedDateTimeDisplay
import kotlinx.datetime.LocalDateTime

internal fun LocalDateTime.toCelebrationDisplay(): String = localizedDateTimeDisplay()
