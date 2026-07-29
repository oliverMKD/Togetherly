package com.togetherly.core.coroutines

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TestAppDispatchersTest {

    @Test
    fun coroutineExecutesUnderSuppliedTestDispatcher() = runTest {
        val dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler))
        var executed = false

        launch(dispatchers.default) {
            executed = true
        }
        advanceUntilIdle()

        assertTrue(executed)
    }
}
