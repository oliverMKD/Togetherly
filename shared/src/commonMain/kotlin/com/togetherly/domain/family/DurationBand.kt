package com.togetherly.domain.family

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class DurationBand(val minimum: Duration) {
    FIVE_MINUTES(5.minutes),
    TEN_MINUTES(10.minutes),
    TWENTY_MINUTES(20.minutes),
    THIRTY_PLUS_MINUTES(30.minutes),
}
