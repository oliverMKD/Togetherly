package com.togetherly.feature.today.mapper

import com.togetherly.core.ui.UiText
import kotlinx.datetime.LocalTime
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_greeting_afternoon
import togetherly.shared.generated.resources.today_greeting_evening
import togetherly.shared.generated.resources.today_greeting_morning

/**
 * Pure time-of-day → copy mapping — the ViewModel resolves [LocalTime] once (via
 * [com.togetherly.core.datetime.AppClock] and the injected timezone), never a Composable calling
 * the system clock directly. Boundaries: before noon is morning, before 5pm is afternoon,
 * otherwise evening — a deliberately simple, non-configurable split; there is no per-family
 * "morning starts at 6am" preference to honor.
 */
fun greetingFor(localTime: LocalTime): UiText = UiText.Resource(
    when {
        localTime.hour < 12 -> Res.string.today_greeting_morning
        localTime.hour < 17 -> Res.string.today_greeting_afternoon
        else -> Res.string.today_greeting_evening
    },
)
