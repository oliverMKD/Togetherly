package com.togetherly.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AppDispatchers {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/**
 * `Dispatchers.IO` is declared per-platform in kotlinx.coroutines (JVM vs. Native), and on the
 * configured iOS targets it resolves to an internal symbol that isn't accessible from consumer
 * code, so the io dispatcher is supplied per-platform via this expect/actual instead.
 */
internal expect val ioDispatcher: CoroutineDispatcher

internal class DefaultAppDispatchers : AppDispatchers {
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = ioDispatcher
    override val main: CoroutineDispatcher get() = Dispatchers.Main
}
