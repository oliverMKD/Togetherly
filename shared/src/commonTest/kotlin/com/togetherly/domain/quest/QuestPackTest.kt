package com.togetherly.domain.quest

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestPackTest {

    @Test
    fun validPackIsAccepted() {
        val pack = validQuestPack()

        assertEquals(listOf(QuestId("quest-1"), QuestId("quest-2")), pack.questIds)
    }

    @Test
    fun emptyQuestListIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validQuestPack(questIds = emptyList())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun duplicateQuestIdsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validQuestPack(questIds = listOf(QuestId("quest-1"), QuestId("quest-1")))
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun negativeSortOrderIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validQuestPack(sortOrder = -1)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun questOrderingIsPreserved() {
        val ordered = listOf(QuestId("quest-3"), QuestId("quest-1"), QuestId("quest-2"))
        val pack = validQuestPack(questIds = ordered)

        assertEquals(ordered, pack.questIds)
    }
}
