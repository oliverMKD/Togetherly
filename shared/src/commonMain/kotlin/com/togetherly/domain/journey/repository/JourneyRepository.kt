package com.togetherly.domain.journey.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.journey.JourneyEntry
import kotlinx.coroutines.flow.Flow

/**
 * Journey is derived, read-only data — completion changes happen exclusively through
 * [com.togetherly.domain.completion.repository.CompletionRepository]; this contract has no
 * save/update/delete methods.
 *
 * The eventual data-layer implementation is expected to combine reads from
 * [com.togetherly.domain.completion.repository.CompletionRepository] and
 * [com.togetherly.domain.quest.repository.QuestRepository] in memory (joining each completion
 * to its quest content by [com.togetherly.domain.quest.QuestId]) rather than a database join —
 * Journey has no persisted storage of its own to join against.
 */
interface JourneyRepository {

    fun observeJourney(): Flow<DataResult<List<JourneyEntry>>>

    suspend fun getJourney(): DataResult<List<JourneyEntry>>
}
