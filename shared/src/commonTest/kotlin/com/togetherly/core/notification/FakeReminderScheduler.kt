package com.togetherly.core.notification

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.ReminderPreference

class FakeReminderScheduler : ReminderScheduler {

    val scheduledCalls: MutableList<ReminderPreference> = mutableListOf()
    val refreshedCalls: MutableList<ReminderPreference> = mutableListOf()
    var cancelCallCount: Int = 0
        private set

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    override suspend fun schedule(preference: ReminderPreference): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        scheduledCalls += preference
        return DataResult.Success(Unit)
    }

    override suspend fun cancel(): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        cancelCallCount++
        return DataResult.Success(Unit)
    }

    override suspend fun refresh(preference: ReminderPreference): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        refreshedCalls += preference
        return DataResult.Success(Unit)
    }
}
