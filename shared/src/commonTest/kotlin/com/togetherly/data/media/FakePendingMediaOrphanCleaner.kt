package com.togetherly.data.media

import com.togetherly.core.result.DataResult
import kotlin.time.Duration
import kotlin.time.Instant

class FakePendingMediaOrphanCleaner : PendingMediaOrphanCleaner {

    val calls = mutableListOf<Instant>()
    var nextResult: DataResult<Int> = DataResult.Success(0)

    override suspend fun deleteExpiredPending(now: Instant, thresholdAge: Duration): DataResult<Int> {
        calls += now
        return nextResult
    }
}
