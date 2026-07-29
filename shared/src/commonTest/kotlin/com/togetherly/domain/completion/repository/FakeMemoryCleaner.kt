package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult

class FakeMemoryCleaner : MemoryCleaner {

    var clearCallCount: Int = 0
        private set

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    override suspend fun clearAllMemoryContent(): DataResult<Unit> {
        clearCallCount++
        nextError?.let {
            nextError = null
            return DataResult.Error(it)
        }
        return DataResult.Success(Unit)
    }
}
