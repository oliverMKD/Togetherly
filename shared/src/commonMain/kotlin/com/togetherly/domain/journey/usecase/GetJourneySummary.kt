package com.togetherly.domain.journey.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.journey.JourneySummary
import com.togetherly.domain.journey.deriveJourneySummary
import com.togetherly.domain.journey.repository.JourneyRepository
import kotlinx.datetime.TimeZone

/**
 * [timeZone] is passed per call rather than injected, since a fixed constructor-owned timezone
 * would go stale the moment a family's timezone changes — see [deriveJourneySummary]. No streak
 * behavior is computed here; only totals, per-category counts, and active dates.
 */
class GetJourneySummary(
    private val journeyRepository: JourneyRepository,
) {
    suspend operator fun invoke(timeZone: TimeZone): DataResult<JourneySummary> =
        journeyRepository.getJourney().map { entries -> deriveJourneySummary(entries, timeZone) }
}
