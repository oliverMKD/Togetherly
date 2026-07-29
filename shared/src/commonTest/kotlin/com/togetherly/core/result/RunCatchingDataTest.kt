package com.togetherly.core.result

import com.togetherly.core.error.AppError
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RunCatchingDataTest {

    @Test
    fun cancellationIsRethrownNotWrapped() {
        assertFailsWith<CancellationException> {
            runCatchingData<Int> {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun rawExceptionMessagesAreNotUsedAsDomainErrorMessages() {
        val thrown = IllegalStateException("raw internal detail: connection string xyz")

        val result = runCatchingData<Int> { throw thrown }

        val error = (result as DataResult.Error).error
        assertIs<AppError.Unexpected>(error)
        assertEquals(thrown, error.cause)
    }
}
