package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.data.local.saved.SavedQuestEntity
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.saved.SavedQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SavedQuestMapperTest {

    private val mapper = SavedQuestMapper()

    @Test
    fun savedQuestRoundTripsThroughItsEntity() {
        val domain = SavedQuest(questId = QuestId("quest-1"), savedAt = Instant.fromEpochMilliseconds(1_690_000_123L))

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        val roundTripped = (mapper.toDomain(entity) as DataResult.Success).value

        assertEquals(domain, roundTripped)
    }

    @Test
    fun timestampPrecisionIsPreservedExactlyIncludingSubSecondMillis() {
        val preciseInstant = Instant.fromEpochMilliseconds(1_690_000_123_456L)
        val domain = SavedQuest(questId = QuestId("quest-1"), savedAt = preciseInstant)

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        assertEquals(1_690_000_123_456L, entity.savedAtEpochMillis)

        val roundTripped = (mapper.toDomain(entity) as DataResult.Success).value
        assertEquals(preciseInstant, roundTripped.savedAt)
    }
}
