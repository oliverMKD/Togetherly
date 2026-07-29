package com.togetherly.domain.completion

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestCompletionTest {

    @Test
    fun completingAStartedSessionProducesAValidCompletion() {
        val session = validActiveQuestSession()

        val completion = session.complete(completedAt = COMPLETED_AT)

        assertEquals(session.completionId, completion.id)
        assertEquals(session.startedAt, completion.startedAt)
        assertEquals(COMPLETED_AT, completion.completedAt)
    }

    @Test
    fun completionWithoutOptionalMemoryContentIsValid() {
        val completion = validQuestCompletion(note = null, reactions = emptySet(), media = emptyList())

        assertEquals(null, completion.note)
        assertEquals(emptySet(), completion.reactions)
        assertEquals(emptyList(), completion.media)
    }

    @Test
    fun completionTimeEarlierThanStartIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validQuestCompletion(startedAt = COMPLETED_AT, completedAt = STARTED_AT)
        }
        assertEquals(DomainValidationReason.INVALID_ORDER, exception.reason)
    }

    @Test
    fun positiveQuestVersionIsRequired() {
        val exception = assertFailsWith<DomainValidationException> {
            validQuestCompletion(questVersion = 0)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun reactionsArePreserved() {
        val completion = validQuestCompletion(reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY))

        assertEquals(setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY), completion.reactions)
    }

    @Test
    fun questVersionIsPreserved() {
        val session = validActiveQuestSession(questVersion = 3)

        val completion = session.complete(completedAt = COMPLETED_AT)

        assertEquals(3, completion.questVersion)
    }
}
