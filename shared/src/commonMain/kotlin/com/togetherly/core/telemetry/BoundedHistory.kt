package com.togetherly.core.telemetry

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A thread-safe, fixed-capacity, oldest-evicted-first history — the shared building block behind
 * [TelemetryDebugRecorder]'s three independent histories (events/breadcrumbs/provider errors).
 * [record] is a compare-and-swap loop over an immutable [List] snapshot rather than a lock, since
 * every caller ([TelemetryDebugRecorder]'s own methods) is a plain, non-suspend function that may
 * be invoked from any thread a real provider's own SDK calls back on.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class BoundedHistory<T>(private val maxSize: Int) {

    private val state = AtomicReference<List<T>>(emptyList())

    fun record(item: T) {
        while (true) {
            val current = state.load()
            val updated = (current + item).takeLast(maxSize)
            if (state.compareAndSet(current, updated)) return
        }
    }

    fun snapshot(): List<T> = state.load()

    fun clear() {
        state.store(emptyList())
    }
}
