package com.togetherly.domain.journey.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.journey.JourneyEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeJourneyRepository : JourneyRepository {

    private val journeyFlow = MutableStateFlow<DataResult<List<JourneyEntry>>>(DataResult.Success(emptyList()))

    fun setEntries(entries: List<JourneyEntry>) {
        journeyFlow.value = DataResult.Success(entries)
    }

    fun setError(error: AppError) {
        journeyFlow.value = DataResult.Error(error)
    }

    override fun observeJourney(): Flow<DataResult<List<JourneyEntry>>> = journeyFlow

    override suspend fun getJourney(): DataResult<List<JourneyEntry>> = journeyFlow.value
}
