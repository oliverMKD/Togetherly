package com.togetherly.domain.quest

import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

class FamilyQuestTest {

    @Test
    fun validFreeQuestIsAccepted() {
        val quest = validFamilyQuest(access = QuestAccess.Free)

        assertEquals(QuestAccess.Free, quest.access)
    }

    @Test
    fun validPremiumQuestIsAccepted() {
        val quest = validFamilyQuest(access = QuestAccess.Premium(EntitlementId("premium")))

        assertEquals(QuestAccess.Premium(EntitlementId("premium")), quest.access)
    }

    @Test
    fun emptyInstructionsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(instructions = emptyList())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun emptyAgeBandsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(ageBands = emptySet())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun invalidVersionIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(version = 0)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun invalidDurationIsRejected() {
        val zero = assertFailsWith<DomainValidationException> {
            validFamilyQuest(durationMinutes = 0)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, zero.reason)

        val tooLong = assertFailsWith<DomainValidationException> {
            validFamilyQuest(durationMinutes = FamilyQuest.MAX_DURATION_MINUTES + 1)
        }
        assertEquals(DomainValidationReason.VALUE_TOO_LONG, tooLong.reason)
    }

    @Test
    fun negativeCooldownIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(cooldownDays = -1)
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun duplicateMaterialsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(materials = listOf(MaterialName("Tape"), MaterialName("Tape")))
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun duplicateHintsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyQuest(hints = listOf(InstructionText("Look up"), InstructionText("Look up")))
        }
        assertEquals(DomainValidationReason.DUPLICATE_VALUE, exception.reason)
    }

    @Test
    fun timerIsOptional() {
        val withoutTimer = validFamilyQuest(timer = null)
        val withTimer = validFamilyQuest(timer = QuestTimer(duration = 5.minutes, keepScreenOn = true))

        assertEquals(null, withoutTimer.timer)
        assertEquals(5.minutes, withTimer.timer?.duration)
    }

    @Test
    fun safetyNoteIsOptional() {
        val withoutNote = validFamilyQuest(safetyNote = null)
        val withNote = validFamilyQuest(safetyNote = SafetyNote("Adult supervision required."))

        assertEquals(null, withoutNote.safetyNote)
        assertEquals(SafetyNote("Adult supervision required."), withNote.safetyNote)
    }
}
