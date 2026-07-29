package com.togetherly.domain.completion

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActiveQuestSessionTest {

    @Test
    fun validActiveSessionIsAccepted() {
        val session = validActiveQuestSession()

        assertEquals(1, session.questVersion)
    }

    @Test
    fun invalidQuestVersionIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validActiveQuestSession(questVersion = 0)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }
}
