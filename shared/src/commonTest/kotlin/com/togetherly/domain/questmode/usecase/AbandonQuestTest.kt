package com.togetherly.domain.questmode.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.validActiveQuestSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AbandonQuestTest {

    @Test
    fun explicitAbandonClearsTheActiveSession() = runTest {
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(validActiveQuestSession()) }
        val useCase = AbandonQuest(completionRepository)

        val result = useCase()

        assertEquals(DataResult.Success(Unit), result)
        val activeSessionResult = completionRepository.getActiveSession()
        assertNull((activeSessionResult as DataResult.Success).value)
    }

    @Test
    fun abandonFailureRemainsTyped() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveActiveSession(validActiveQuestSession())
            setNextError(AppError.Storage(StorageError.WRITE_FAILED))
        }
        val useCase = AbandonQuest(completionRepository)

        val result = useCase()

        assertEquals(DataResult.Error(AppError.Storage(StorageError.WRITE_FAILED)), result)
    }
}
