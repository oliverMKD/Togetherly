package com.togetherly.content.validation

import com.togetherly.content.model.QuestAccessDto
import com.togetherly.content.model.InstructionStepDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QuestCatalogueValidatorTest {

    private val validator = QuestCatalogueValidator()

    @Test
    fun completelyValidCatalogueIsValid() {
        val result = validator.validate(validCatalogueDto())

        // A minimal single-pack/single-quest catalogue still trips content-quality warnings
        // (single-quest pack, uniform category) — those don't make it Invalid.
        assertIs<CatalogueValidationResult.Valid>(result)
        assertTrue(result.warnings.all { it.severity == CatalogueIssueSeverity.WARNING })
    }

    @Test
    fun unsupportedSchemaFails() {
        val result = validator.validate(validCatalogueDto(schemaVersion = 999))

        assertHasIssue(result, "schemaVersion", CatalogueIssueCode.UNSUPPORTED_SCHEMA_VERSION)
    }

    @Test
    fun duplicateQuestIdsFails() {
        val quest = validQuestDto()
        val result = validator.validate(validCatalogueDto(quests = listOf(quest, quest)))

        assertHasIssue(result, "quests", CatalogueIssueCode.DUPLICATE_VALUE)
    }

    @Test
    fun duplicatePackIdsFails() {
        val pack = validPackDto()
        val result = validator.validate(validCatalogueDto(packs = listOf(pack, pack)))

        assertHasIssue(result, "packs", CatalogueIssueCode.DUPLICATE_VALUE)
    }

    @Test
    fun missingPackReferenceFails() {
        val quest = validQuestDto(packId = "missing-pack")
        val result = validator.validate(validCatalogueDto(quests = listOf(quest)))

        assertHasIssue(result, "quests[0].packId", CatalogueIssueCode.REFERENCE_NOT_FOUND)
    }

    @Test
    fun missingQuestReferenceFails() {
        val pack = validPackDto(questIds = listOf("missing-quest"))
        val result = validator.validate(validCatalogueDto(packs = listOf(pack)))

        assertHasIssue(result, "packs[0].questIds[0]", CatalogueIssueCode.REFERENCE_NOT_FOUND)
    }

    @Test
    fun invalidAccessCombinationFails() {
        val quest = validQuestDto(access = QuestAccessDto(type = "free", entitlementId = "family_plus"))
        val result = validator.validate(validCatalogueDto(quests = listOf(quest)))

        assertHasIssue(result, "quests[0].access.entitlementId", CatalogueIssueCode.INVALID_ACCESS_COMBINATION)
    }

    @Test
    fun freePackContainingPremiumQuestFails() {
        val quest = validQuestDto(access = QuestAccessDto(type = "premium", entitlementId = "family_plus"))
        val pack = validPackDto(access = QuestAccessDto(type = "free"))
        val result = validator.validate(validCatalogueDto(packs = listOf(pack), quests = listOf(quest)))

        assertHasIssue(result, "packs[0].questIds[0]", CatalogueIssueCode.CONTRADICTORY_STATE)
    }

    @Test
    fun categorySpecificPackContainingWrongCategoryFails() {
        val quest = validQuestDto(category = "move")
        val pack = validPackDto(category = "discover")
        val result = validator.validate(validCatalogueDto(packs = listOf(pack), quests = listOf(quest)))

        assertHasIssue(result, "packs[0].questIds[0]", CatalogueIssueCode.CONTRADICTORY_STATE)
    }

    @Test
    fun questListedInWrongPackFails() {
        val quest = validQuestDto(id = "quest-1", packId = "pack-1")
        val decoyQuest = validQuestDto(id = "quest-2", packId = "pack-2")
        val pack1 = validPackDto(id = "pack-1", questIds = listOf("quest-1"))
        // pack-2 wrongly also claims quest-1, which actually belongs to pack-1.
        val pack2 = validPackDto(id = "pack-2", questIds = listOf("quest-1", "quest-2"))

        val result = validator.validate(
            validCatalogueDto(packs = listOf(pack1, pack2), quests = listOf(quest, decoyQuest)),
        )

        assertHasIssue(result, "packs[1].questIds[0]", CatalogueIssueCode.REFERENCE_MISMATCH)
    }

    @Test
    fun questNotListedByItsDeclaredPackFails() {
        val quest1 = validQuestDto(id = "quest-1", packId = "pack-1")
        val quest2 = validQuestDto(id = "quest-2", packId = "pack-1")
        // pack-1 only lists quest-2, even though quest-1 declares pack-1 as its pack.
        val pack = validPackDto(questIds = listOf("quest-2"))

        val result = validator.validate(validCatalogueDto(packs = listOf(pack), quests = listOf(quest1, quest2)))

        assertHasIssue(result, "quests[0]", CatalogueIssueCode.REFERENCE_NOT_FOUND)
    }

    @Test
    fun invalidInstructionOrderingFails() {
        val quest = validQuestDto(
            instructions = listOf(
                InstructionStepDto(order = 1, text = "Step one."),
                InstructionStepDto(order = 3, text = "Step three."),
            ),
        )
        val result = validator.validate(validCatalogueDto(quests = listOf(quest)))

        assertHasIssue(result, "quests[0].instructions", CatalogueIssueCode.INVALID_ORDER)
    }

    @Test
    fun multipleIndependentProblemsAreAllReturnedTogether() {
        val duplicateQuest = validQuestDto()
        val invalidVersionQuest = validQuestDto(id = "quest-2", version = 0)
        val invalidOrderQuest = validQuestDto(
            id = "quest-3",
            instructions = listOf(InstructionStepDto(order = 2, text = "Wrong start.")),
        )
        val blankTitlePack = validPackDto().copy(title = "")

        val catalogue = validCatalogueDto(
            packs = listOf(blankTitlePack),
            quests = listOf(duplicateQuest, duplicateQuest, invalidVersionQuest, invalidOrderQuest),
        )

        val result = validator.validate(catalogue)

        assertIs<CatalogueValidationResult.Invalid>(result)
        val codes = result.issues.map { it.code }
        assertTrue(CatalogueIssueCode.DUPLICATE_VALUE in codes, "expected a duplicate-quest-id issue")
        assertTrue(codes.count { it == CatalogueIssueCode.NON_POSITIVE_VALUE } >= 1, "expected the non-positive version issue")
        assertTrue(CatalogueIssueCode.INVALID_ORDER in codes, "expected the invalid instruction order issue")
        assertTrue(result.issues.any { it.path == "packs[0].title" && it.code == CatalogueIssueCode.BLANK_VALUE })

        // Root issues (duplicate quest id) must precede pack issues, which must precede quest issues.
        val firstRootIndex = result.issues.indexOfFirst { it.path == "quests" }
        val firstPackIndex = result.issues.indexOfFirst { it.path.startsWith("packs[") }
        val firstQuestFieldIndex = result.issues.indexOfFirst { it.path.startsWith("quests[") }
        assertTrue(firstRootIndex < firstPackIndex)
        assertTrue(firstPackIndex < firstQuestFieldIndex)
    }

    @Test
    fun issueOrderingIsDeterministicAcrossRuns() {
        val duplicateQuest = validQuestDto()
        val catalogue = validCatalogueDto(quests = listOf(duplicateQuest, duplicateQuest))

        val first = validator.validate(catalogue)
        val second = validator.validate(catalogue)

        assertEquals(first, second)
    }

    private fun assertHasIssue(result: CatalogueValidationResult, path: String, code: CatalogueIssueCode) {
        assertIs<CatalogueValidationResult.Invalid>(result)
        assertTrue(
            result.issues.any { it.path == path && it.code == code },
            "expected an issue at '$path' with code $code, got: ${result.issues}",
        )
    }
}
