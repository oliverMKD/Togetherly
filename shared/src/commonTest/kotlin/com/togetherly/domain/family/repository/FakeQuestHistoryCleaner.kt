package com.togetherly.domain.family.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult

class FakeQuestHistoryCleaner : QuestHistoryCleaner {

    var resetCallCount: Int = 0
        private set

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    override suspend fun resetQuestHistory(): DataResult<Unit> {
        resetCallCount++
        nextError?.let {
            nextError = null
            return DataResult.Error(it)
        }
        return DataResult.Success(Unit)
    }
}
