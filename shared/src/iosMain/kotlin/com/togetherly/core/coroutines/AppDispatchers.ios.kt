package com.togetherly.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val ioDispatcher: CoroutineDispatcher
    get() = Dispatchers.Default.limitedParallelism(64)
