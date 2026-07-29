package com.togetherly.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

class TestAppDispatchers(
    dispatcher: CoroutineDispatcher,
) : AppDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}
