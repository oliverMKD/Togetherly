package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.ActiveQuestSessionEntity
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ActiveQuestSessionMapperTest {

    private val mapper = ActiveQuestSessionMapper()

    @Test
    fun activeSessionRoundTripsThroughItsEntity() {
        val domain = ActiveQuestSession(
            completionId = CompletionId("completion-1"),
            familyId = FamilyId("family-1"),
            questId = QuestId("quest-1"),
            questVersion = 1,
            startedAt = Instant.fromEpochMilliseconds(1_690_000_000_000L),
        )

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        assertEquals(ACTIVE_QUEST_SESSION_SLOT_ID, entity.slotId)

        val roundTripped = (mapper.toDomain(entity) as DataResult.Success).value
        assertEquals(domain, roundTripped)
    }

    @Test
    fun unexpectedSlotIdIsCorruptedStorage() {
        val entity = ActiveQuestSessionEntity(
            slotId = ACTIVE_QUEST_SESSION_SLOT_ID + 1,
            completionId = "completion-1",
            familyId = "family-1",
            questId = "quest-1",
            questVersion = 1,
            startedAtEpochMillis = 1_000L,
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(entity))
    }
}
