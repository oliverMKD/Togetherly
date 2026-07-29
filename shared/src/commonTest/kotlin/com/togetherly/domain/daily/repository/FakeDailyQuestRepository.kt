package com.togetherly.domain.daily.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeDailyQuestRepository : DailyQuestRepository {

    private val selectionsByDate = mutableMapOf<LocalDate, MutableStateFlow<DataResult<DailyQuest?>>>()
    private val dismissals = mutableListOf<DismissedQuest>()

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    private fun flowFor(localDate: LocalDate): MutableStateFlow<DataResult<DailyQuest?>> =
        selectionsByDate.getOrPut(localDate) { MutableStateFlow(DataResult.Success(null)) }

    override fun observeToday(localDate: LocalDate): Flow<DataResult<DailyQuest?>> = flowFor(localDate)

    override suspend fun getToday(localDate: LocalDate): DataResult<DailyQuest?> {
        consumeError()?.let { return DataResult.Error(it) }
        return flowFor(localDate).value
    }

    override suspend fun saveDailyQuest(dailyQuest: DailyQuest): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        flowFor(dailyQuest.localDate).value = DataResult.Success(dailyQuest)
        return DataResult.Success(Unit)
    }

    override suspend fun recordDismissal(dismissedQuest: DismissedQuest): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        dismissals += dismissedQuest
        return DataResult.Success(Unit)
    }

    /** Newest first, matching the Room-backed implementation's `ORDER BY dismissedAtEpochMillis DESC`. */
    override suspend fun getRecentDismissals(since: Instant): DataResult<List<DismissedQuest>> {
        consumeError()?.let { return DataResult.Error(it) }
        return DataResult.Success(dismissals.filter { it.dismissedAt >= since }.sortedByDescending { it.dismissedAt })
    }

    override suspend fun clearDailyQuest(localDate: LocalDate): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        flowFor(localDate).value = DataResult.Success(null)
        return DataResult.Success(Unit)
    }
}
