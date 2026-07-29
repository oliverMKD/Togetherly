package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.daily.DismissedQuestEntity
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.quest.QuestId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DismissedQuestMapperTest {

    private val mapper = DismissedQuestMapper()

    @Test
    fun dismissalRoundTripsThroughItsEntity() {
        val domain = DismissedQuest(
            questId = QuestId("quest-1"),
            dismissedAt = Instant.fromEpochMilliseconds(1_690_000_000_000L),
            localDate = LocalDate(2026, 7, 24),
        )

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        assertEquals("2026-07-24", entity.localDate)

        val roundTripped = (mapper.toDomain(entity) as DataResult.Success).value
        assertEquals(domain, roundTripped)
    }

    @Test
    fun invalidLocalDateIsCorruptedStorage() {
        val entity = DismissedQuestEntity(questId = "quest-1", dismissedAtEpochMillis = 1_000L, localDate = "not-a-date")

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(entity))
    }
}
