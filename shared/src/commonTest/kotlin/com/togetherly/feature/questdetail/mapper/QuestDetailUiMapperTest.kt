package com.togetherly.feature.questdetail.mapper

import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.InstructionStep
import com.togetherly.domain.quest.InstructionText
import com.togetherly.domain.quest.MaterialName
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.SafetyNote
import com.togetherly.domain.quest.validFamilyQuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FAMILY_PLUS = "family_plus"

class QuestDetailUiMapperTest {

    @Test
    fun instructionsAreOrderedByStepOrderEvenIfSuppliedOutOfOrder() {
        val quest = validFamilyQuest(
            instructions = listOf(
                InstructionStep(2, InstructionText("second step")),
                InstructionStep(1, InstructionText("first step")),
            ),
        )

        val detail = quest.toDetailUi(locked = false, packTitle = null)

        assertEquals(listOf("first step", "second step"), detail.instructions)
    }

    @Test
    fun materialsAndHintsMapToPlainStrings() {
        val quest = validFamilyQuest(
            materials = listOf(MaterialName("Chalk"), MaterialName("Rope")),
            hints = listOf(InstructionText("Try the garden.")),
        )

        val detail = quest.toDetailUi(locked = false, packTitle = null)

        assertEquals(listOf("Chalk", "Rope"), detail.materials)
        assertEquals(listOf("Try the garden."), detail.hints)
    }

    @Test
    fun missingSafetyNoteMapsToNull() {
        val quest = validFamilyQuest(safetyNote = null)

        assertNull(quest.toDetailUi(locked = false, packTitle = null).safetyNote)
    }

    @Test
    fun presentSafetyNoteMapsToItsPlainText() {
        val quest = validFamilyQuest(safetyNote = SafetyNote("Adult supervision required."))

        assertEquals("Adult supervision required.", quest.toDetailUi(locked = false, packTitle = null).safetyNote)
    }

    @Test
    fun lockedMappingNeverPopulatesInstructionsOrHints() {
        val quest = validFamilyQuest(
            instructions = listOf(InstructionStep(1, InstructionText("Do the secret thing."))),
            hints = listOf(InstructionText("A spoiler hint.")),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )

        val detail = quest.toDetailUi(locked = true, packTitle = null)

        assertTrue(detail.instructions.isEmpty())
        assertTrue(detail.hints.isEmpty())
    }

    @Test
    fun lockedMappingStillPopulatesMaterialsAndSafetyNote() {
        val quest = validFamilyQuest(
            materials = listOf(MaterialName("Paper")),
            safetyNote = SafetyNote("Adult supervision required."),
            access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)),
        )

        val detail = quest.toDetailUi(locked = true, packTitle = null)

        assertEquals(listOf("Paper"), detail.materials)
        assertEquals("Adult supervision required.", detail.safetyNote)
    }

    @Test
    fun premiumQuestMapsIsPremiumTrue() {
        val quest = validFamilyQuest(access = QuestAccess.Premium(EntitlementId(FAMILY_PLUS)))

        assertTrue(quest.toDetailUi(locked = false, packTitle = null).isPremium)
    }

    @Test
    fun freeQuestMapsIsPremiumFalse() {
        val quest = validFamilyQuest(access = QuestAccess.Free)

        assertEquals(false, quest.toDetailUi(locked = false, packTitle = null).isPremium)
    }

    @Test
    fun packTitleIsPassedThroughUnchanged() {
        val quest = validFamilyQuest()

        assertEquals("Creative Sparks", quest.toDetailUi(locked = false, packTitle = "Creative Sparks").packTitle)
    }
}
