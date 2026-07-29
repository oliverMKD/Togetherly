package com.togetherly.domain.validation

import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.quest.ArtworkKey
import com.togetherly.domain.quest.InstructionText
import com.togetherly.domain.quest.MaterialName
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.quest.SafetyNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class TextValueSpec(
    val maxLength: Int,
    val construct: (String) -> Any,
)

class DomainTextValueValidationTest {

    private val textValueSpecs: List<TextValueSpec> = listOf(
        TextValueSpec(FamilyDisplayName.MAX_LENGTH) { FamilyDisplayName(it) },
        TextValueSpec(QuestTitle.MAX_LENGTH) { QuestTitle(it) },
        TextValueSpec(QuestSummary.MAX_LENGTH) { QuestSummary(it) },
        TextValueSpec(InstructionText.MAX_LENGTH) { InstructionText(it) },
        TextValueSpec(CompletionPrompt.MAX_LENGTH) { CompletionPrompt(it) },
        TextValueSpec(SafetyNote.MAX_LENGTH) { SafetyNote(it) },
        TextValueSpec(MaterialName.MAX_LENGTH) { MaterialName(it) },
        TextValueSpec(ArtworkKey.MAX_LENGTH) { ArtworkKey(it) },
    )

    @Test
    fun validTextIsAccepted() {
        for (spec in textValueSpecs) {
            spec.construct("Valid text")
        }
    }

    @Test
    fun blankTextIsRejected() {
        for (spec in textValueSpecs) {
            val exception = assertFailsWith<DomainValidationException> { spec.construct("") }
            assertEquals(DomainValidationReason.BLANK_VALUE, exception.reason)
        }
    }

    @Test
    fun surroundingWhitespaceIsRejected() {
        for (spec in textValueSpecs) {
            val leading = assertFailsWith<DomainValidationException> { spec.construct(" leading") }
            assertEquals(DomainValidationReason.SURROUNDING_WHITESPACE, leading.reason)

            val trailing = assertFailsWith<DomainValidationException> { spec.construct("trailing ") }
            assertEquals(DomainValidationReason.SURROUNDING_WHITESPACE, trailing.reason)
        }
    }

    @Test
    fun maximumLengthIsEnforced() {
        for (spec in textValueSpecs) {
            spec.construct("a".repeat(spec.maxLength))

            val exception = assertFailsWith<DomainValidationException> {
                spec.construct("a".repeat(spec.maxLength + 1))
            }
            assertEquals(DomainValidationReason.VALUE_TOO_LONG, exception.reason)
        }
    }
}
