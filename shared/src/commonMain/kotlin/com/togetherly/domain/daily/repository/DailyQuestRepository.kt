package com.togetherly.domain.daily.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Stores daily quest selections and dismissal history — it never chooses a quest itself; that
 * belongs to a future recommendation use case. Exactly one selection exists per [LocalDate];
 * [saveDailyQuest] replaces whatever selection already exists for that date. Dismissal history
 * is tracked separately from the current selection and is unaffected by [clearDailyQuest].
 * [clearDailyQuest] is idempotent. Every date and instant is supplied by the caller — this
 * repository never calls the system clock itself.
 */
interface DailyQuestRepository {

    fun observeToday(
        localDate: LocalDate,
    ): Flow<DataResult<DailyQuest?>>

    suspend fun getToday(
        localDate: LocalDate,
    ): DataResult<DailyQuest?>

    suspend fun saveDailyQuest(
        dailyQuest: DailyQuest,
    ): DataResult<Unit>

    suspend fun recordDismissal(
        dismissedQuest: DismissedQuest,
    ): DataResult<Unit>

    suspend fun getRecentDismissals(
        since: Instant,
    ): DataResult<List<DismissedQuest>>

    suspend fun clearDailyQuest(
        localDate: LocalDate,
    ): DataResult<Unit>
}
