package com.togetherly.content.mapper

import com.togetherly.content.model.FamilyQuestDto
import com.togetherly.content.model.InstructionStepDto
import com.togetherly.content.model.QuestAccessDto
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.QuestAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun validQuestDto(
    id: String = "quest-1",
    access: QuestAccessDto = QuestAccessDto(type = "free"),
    safetyNote: String? = null,
) = FamilyQuestDto(
    id = id,
    version = 1,
    title = "Backyard Scavenger Hunt",
    summary = "Find five hidden treasures together.",
    instructions = listOf(InstructionStepDto(order = 1, text = "Hide five small objects.")),
    category = "discover",
    ageBands = listOf("6-8"),
    durationMinutes = 20,
    location = "outdoor",
    preparation = "simple-materials",
    energy = "moderate",
    completionPrompt = "Share what you found!",
    safetyNote = safetyNote,
    packId = "pack-1",
    access = access,
)

class FamilyQuestMapperTest {

    private val mapper: FamilyQuestMapper = DefaultFamilyQuestMapper()

    @Test
    fun validFreeQuestMapsCorrectly() {
        val result = mapper.map(validQuestDto(), "quests[0]")

        val quest = (result as ContentMappingResult.Success).value
        assertEquals(QuestAccess.Free, quest.access)
        assertEquals(AgeBand.AGE_6_TO_8, quest.ageBands.single())
        assertEquals(1, quest.instructions.single().order)
    }

    @Test
    fun validPremiumQuestMapsCorrectly() {
        val result = mapper.map(
            validQuestDto(access = QuestAccessDto(type = "premium", entitlementId = "family_plus")),
            "quests[0]",
        )

        val quest = (result as ContentMappingResult.Success).value
        assertEquals(QuestAccess.Premium(EntitlementId("family_plus")), quest.access)
    }

    @Test
    fun optionalSafetyNoteIsPreservedWhenPresentAndNullWhenAbsent() {
        val withoutNote = mapper.map(validQuestDto(safetyNote = null), "quests[0]")
        val withNote = mapper.map(validQuestDto(safetyNote = "Adult supervision required."), "quests[0]")

        assertEquals(null, (withoutNote as ContentMappingResult.Success).value.safetyNote?.value)
        assertEquals(
            "Adult supervision required.",
            (withNote as ContentMappingResult.Success).value.safetyNote?.value,
        )
    }

    @Test
    fun domainValidationFailureBecomesAMappingIssue() {
        val result = mapper.map(validQuestDto().copy(title = ""), "quests[0]")

        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.DOMAIN_VALIDATION_FAILED, result.issue.code)
    }

    @Test
    fun rawDomainExceptionDoesNotEscape() {
        // An empty age-band list violates FamilyQuest's own invariant (at least one required),
        // so this proves the mapper catches it rather than letting it propagate uncaught.
        val result = mapper.map(validQuestDto().copy(ageBands = emptyList()), "quests[0]")

        assertIs<ContentMappingResult.Failure>(result)
    }
}
